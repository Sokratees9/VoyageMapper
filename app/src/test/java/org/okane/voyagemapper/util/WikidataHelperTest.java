package org.okane.voyagemapper.util;

import static org.junit.jupiter.api.Assertions.*;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;

import org.junit.jupiter.api.Test;
import org.robolectric.annotation.Config;

import java.util.concurrent.atomic.AtomicReference;

import io.hosuaby.inject.resources.junit.jupiter.GivenTextResource;
import io.hosuaby.inject.resources.junit.jupiter.TestWithResources;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

@TestWithResources
@Config(manifest = Config.NONE)
public class WikidataHelperTest {

    @GivenTextResource("org/okane/voyagemapper/util/LatLong.json") String jsonFile;

//    @Test
//    public void testParseP625_Valid() {
//        String json = "{ \"entities\": { \"Q123\": { \"claims\": { \"P625\": [ { " +
//                "\"mainsnak\": { \"datavalue\": { \"value\": { \"latitude\": 41.9, \"longitude\": 12.49 }}}}]}}}}";
//
//        LatLng ll = WikidataHelper.parseP625("Q123", json);
//
//        assertNotNull(ll);
//        assertEquals(41.9, ll.latitude, 1e-6);
//        assertEquals(12.49, ll.longitude, 1e-6);
//    }
//
//    @Test
//    public void testParseP625_Missing() {
//        String json = "{ \"entities\": { \"Q123\": { \"claims\": {} }}}";
//
//        LatLng ll = WikidataHelper.parseP625("Q123", json);
//
//        assertNull(ll);
//    }
//
//    @Test
//    public void testFetchCoordinates_UsesMockHttp() throws Exception {
//        ResponseBody body = ResponseBody.create(jsonFile, MediaType.get("application/json"));
//
//        Response mockResponse = new Response.Builder()
//                .code(200)
//                .message("OK")
//                .request(new Request.Builder().url("http://test").build())
//                .protocol(Protocol.HTTP_1_1)
//                .body(body)
//                .build();
//
//        OkHttpClient fakeClient = new OkHttpClient() {
//            @NonNull
//            @Override
//            public Call newCall(@NonNull Request request) {
//                return new FakeCall(request, mockResponse);
//            }
//        };
//
//        WikidataHelper.setHttpClient(fakeClient);
//        AtomicReference<LatLng> result = new AtomicReference<>();
//        WikidataHelper.fetchCoordinates("Q123", result::set);
//
//        // --- Assert ---
//        assertNotNull(result.get());
//        assertEquals(50.5, result.get().latitude, 1e-6);
//        assertEquals(-1.3, result.get().longitude, 1e-6);
//    }
}