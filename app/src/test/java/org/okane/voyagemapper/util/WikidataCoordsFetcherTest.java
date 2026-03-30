package org.okane.voyagemapper.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.google.android.gms.maps.model.LatLng;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okane.voyagemapper.log.AppLogger;
import org.okane.voyagemapper.log.NoOpLogger;
import org.okane.voyagemapper.service.WikidataService;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

class WikidataCoordsFetcherTest {
    final AppLogger logger = mock(AppLogger.class);
    @BeforeEach
    void setUp() {
        WikidataCoordsFetcher.setLoggerForTests(logger);
    }

    @AfterEach
    void tearDown() {
        TemplateMatcher.setLoggerForTests(new NoOpLogger()); // or restore AndroidLogger if you prefer
    }

    @Test
    void fetchCoords_nullOrEmptyId_callsBackNull_noNetwork() {
        WikidataService svc = mock(WikidataService.class);
        WikidataCoordsFetcher f = new WikidataCoordsFetcher(svc);

        final LatLng[] result = new LatLng[1];
        f.fetchCoords(null, coords -> result[0] = coords);
        assertNull(result[0]);

        f.fetchCoords("", coords -> result[0] = coords);
        assertNull(result[0]);

        verifyNoInteractions(svc);
        verifyNoInteractions(logger);
    }

    @Test
    void fetchCoords_success_parsesCachesAndReturnsCoords_thenUsesCache() {
        WikidataService svc = mock(WikidataService.class);
        @SuppressWarnings("unchecked")
        Call<ResponseBody> call = mock(Call.class);
        when(svc.getEntity("Q1")).thenReturn(call);

        String json = """
                {
                  "entities": {
                    "Q1": {
                      "claims": {
                        "P625": [ {
                          "mainsnak": {
                            "datavalue": {
                              "value": { "latitude": 12.34, "longitude": 56.78 }
                            }
                          }
                        } ]
                      }
                    }
                  }
                }""";

        // Make enqueue call the callback immediately with a successful response
        doAnswer(inv -> {
            Callback<ResponseBody> cb = inv.getArgument(0);
            ResponseBody body = ResponseBody.create(json, MediaType.get("application/json"));
            cb.onResponse(call, Response.success(body));
            return null;
        }).when(call).enqueue(any());

        WikidataCoordsFetcher f = new WikidataCoordsFetcher(svc);

        final LatLng[] r1 = new LatLng[1];
        f.fetchCoords("Q1", coords -> r1[0] = coords);

        assertNotNull(r1[0]);
        assertEquals(12.34, r1[0].latitude, 1e-9);
        assertEquals(56.78, r1[0].longitude, 1e-9);

        // Second call should hit cache: no second enqueue
        final LatLng[] r2 = new LatLng[1];
        f.fetchCoords("Q1", coords -> r2[0] = coords);

        assertNotNull(r2[0]);
        assertEquals(12.34, r2[0].latitude, 1e-9);

        verify(svc, times(1)).getEntity("Q1");
        verify(call, times(1)).enqueue(any());
        verifyNoInteractions(logger);
    }

    @Test
    void fetchCoords_successButNoP625_cachesNoCoords_returnsNull_thenUsesCache() {
        WikidataService svc = mock(WikidataService.class);
        @SuppressWarnings("unchecked")
        Call<ResponseBody> call = mock(Call.class);
        when(svc.getEntity("Q2")).thenReturn(call);

        String jsonNoP625 = "{ \"entities\": { \"Q2\": { \"claims\": { } } } }";

        doAnswer(inv -> {
            Callback<ResponseBody> cb = inv.getArgument(0);
            ResponseBody body = ResponseBody.create(jsonNoP625, MediaType.get("application/json"));
            cb.onResponse(call, Response.success(body));
            return null;
        }).when(call).enqueue(any());

        WikidataCoordsFetcher f = new WikidataCoordsFetcher(svc);

        final LatLng[] r1 = new LatLng[1];
        f.fetchCoords("Q2", coords -> r1[0] = coords);
        assertNull(r1[0]);

        // cache hit should avoid network
        final LatLng[] r2 = new LatLng[1];
        f.fetchCoords("Q2", coords -> r2[0] = coords);
        assertNull(r2[0]);

        verify(svc, times(1)).getEntity("Q2");
        verify(call, times(1)).enqueue(any());

        // parseP625 logs "No P625..." via Log.d; safe (we mocked Log statics)
        verify(logger).d(anyString(), contains("No P625"));
        verifyNoMoreInteractions(logger);
    }

    @Test
    void fetchCoords_httpNotSuccessful_cachesNoCoords_returnsNull() {
        WikidataService svc = mock(WikidataService.class);
        Call<ResponseBody> call = mock(Call.class);
        when(svc.getEntity("Q3")).thenReturn(call);

        doAnswer(inv -> {
            Callback<ResponseBody> cb = inv.getArgument(0);
            // Unsuccessful response (e.g. 403/500) with null body
            Response<ResponseBody> bad = Response.error(
                    500,
                    ResponseBody.create("oops", MediaType.get("text/plain"))
            );
            cb.onResponse(call, bad);
            return null;
        }).when(call).enqueue(any());

        WikidataCoordsFetcher f = new WikidataCoordsFetcher(svc);

        final LatLng[] r = new LatLng[1];
        f.fetchCoords("Q3", coords -> r[0] = coords);
        assertNull(r[0]);

        // Should be cached as NO_COORDS
        final LatLng[] r2 = new LatLng[1];
        f.fetchCoords("Q3", coords -> r2[0] = coords);
        assertNull(r2[0]);

        verify(svc, times(1)).getEntity("Q3");
        verify(call, times(1)).enqueue(any());
        verifyNoInteractions(logger);
    }

    @Test
    void fetchCoords_successButInvalidJson_triggersParseException_logsWarn_andReturnsNull() {
        WikidataService svc = mock(WikidataService.class);
        Call<ResponseBody> call = mock(Call.class);
        when(svc.getEntity("Q4")).thenReturn(call);

        doAnswer(inv -> {
            Callback<ResponseBody> cb = inv.getArgument(0);
            ResponseBody body = ResponseBody.create("NOT JSON", MediaType.get("application/json"));
            cb.onResponse(call, Response.success(body));
            return null;
        }).when(call).enqueue(any());

        WikidataCoordsFetcher f = new WikidataCoordsFetcher(svc);

        final LatLng[] r = new LatLng[1];
        f.fetchCoords("Q4", coords -> r[0] = coords);
        assertNull(r[0]);

        verify(logger).w(contains("WikidataCoordsFetcher"),
                contains("Failed to parse Wikidata for Q4"),
                any(Throwable.class));
        verifyNoMoreInteractions(logger);
    }

    @Test
    void fetchCoords_onFailure_logsWarn_cachesNoCoords_returnsNull() {
        WikidataService svc = mock(WikidataService.class);
        Call<ResponseBody> call = mock(Call.class);
        when(svc.getEntity("Q5")).thenReturn(call);

        RuntimeException boom = new RuntimeException("network down");

        doAnswer(inv -> {
            Callback<ResponseBody> cb = inv.getArgument(0);
            cb.onFailure(call, boom);
            return null;
        }).when(call).enqueue(any());

        WikidataCoordsFetcher f = new WikidataCoordsFetcher(svc);

        final LatLng[] r = new LatLng[1];
        f.fetchCoords("Q5", coords -> r[0] = coords);
        assertNull(r[0]);

        // cached as NO_COORDS, second call no network
        final LatLng[] r2 = new LatLng[1];
        f.fetchCoords("Q5", coords -> r2[0] = coords);
        assertNull(r2[0]);

        verify(svc, times(1)).getEntity("Q5");
        verify(call, times(1)).enqueue(any());

        verify(logger).w(contains("WikidataCoordsFetcher"),
                contains("Failed to fetch Q5"),
                eq(boom));
        verifyNoMoreInteractions(logger);
    }

    @Test
    void parseP625_happyPath_returnsLatLng() throws Exception {
        WikidataCoordsFetcher f = new WikidataCoordsFetcher(mock(WikidataService.class));

        String json = "{ \"entities\": { \"Q9\": { \"claims\": { \"P625\": [ { " +
                "\"mainsnak\": { \"datavalue\": { \"value\": { \"latitude\": 1.2, \"longitude\": 3.4 } } } }" +
                "] } } } }";

        LatLng coords = f.parseP625(json, "Q9");
        assertNotNull(coords);
        assertEquals(1.2, coords.latitude, 1e-9);
        assertEquals(3.4, coords.longitude, 1e-9);
        verifyNoInteractions(logger);
    }

    @Test
    void parseP625_missingOrEmptyP625_returnsNull_andLogsDebug() throws Exception {
        WikidataCoordsFetcher f = new WikidataCoordsFetcher(mock(WikidataService.class));

        String jsonNoP625 = "{ \"entities\": { \"Q10\": { \"claims\": { } } } }";
        assertNull(f.parseP625(jsonNoP625, "Q10"));

        String jsonEmptyP625 = "{ \"entities\": { \"Q11\": { \"claims\": { \"P625\": [] } } } }";
        assertNull(f.parseP625(jsonEmptyP625, "Q11"));

        verify(logger, times(2))
                .d(contains("WikidataCoordsFetcher"), contains("No P625 for"));
        verifyNoMoreInteractions(logger);
    }
}
