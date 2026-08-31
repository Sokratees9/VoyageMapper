package org.okane.voyagemapper.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.okane.voyagemapper.data.local.AppDatabase;
import org.okane.voyagemapper.data.local.dao.CachedSeeListingDao;
import org.okane.voyagemapper.data.local.model.CachedArticleEntity;
import org.okane.voyagemapper.data.local.model.CachedSeeListingEntity;
import org.okane.voyagemapper.model.SeeListing;
import org.okane.voyagemapper.service.NetworkChecker;
import org.okane.voyagemapper.service.WikiRepository;
import org.okane.voyagemapper.ui.model.PlaceItem;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ListingRepositoryIntegrationTest {

    private AppDatabase db;
    private CachedSeeListingDao dao;
    private ExecutorService diskIo;
    private TestListingRepository repository;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();

        dao = db.cachedSeeListingDao();
        diskIo = Executors.newSingleThreadExecutor();

        WikiRepository unusedWikiRepository = new WikiRepository();
        NetworkChecker networkChecker = () -> true;
        repository = new TestListingRepository(dao, diskIo, unusedWikiRepository, networkChecker);
    }

    @After
    public void tearDown() {
        diskIo.shutdownNow();

        if (db != null) {
            db.close();
        }
    }

    @Test
    public void successfulPrefetchStoresListingsInRoom() throws Exception {
        long pageId = 12345L;
        insertParentArticle(pageId);

        repository.setListingsToReturn(
                List.of(
                        new SeeListing(
                                "Test Museum",
                                51.5,
                                -0.1,
                                "+44 1234 567890",
                                "https://example.com",
                                "A test museum",
                                "1 Test Street",
                                "09:00–17:00",
                                "£10",
                                null,
                                null,
                                null
                        )
                )
        );

        PlaceItem article = createArticle(pageId);
        repository.prefetchListingsForArticle(article);
        assertTrue(repository.awaitFetch(2, TimeUnit.SECONDS));
        diskIo.submit(() -> {}).get(2, TimeUnit.SECONDS);

        List<CachedSeeListingEntity> cached = dao.getListingsForPage(pageId);
        assertEquals(1, cached.size());
        assertEquals("Test Museum", cached.get(0).name);
        assertEquals(1, repository.getFetchCount());
    }

    @Test
    public void cachedArticleIsNotFetchedAgain() throws Exception {
        long pageId = 12345L;
        insertParentArticle(pageId);
        CachedSeeListingEntity existing = new CachedSeeListingEntity();

        existing.pageId = pageId;
        existing.name = "Already cached";
        existing.lat = 51.5;
        existing.lon = -0.1;

        dao.insertAll(List.of(existing));
        repository.prefetchListingsForArticle(createArticle(pageId));

        // Submit a barrier behind the repository's initial diskIo check.
        diskIo.submit(() -> {}).get();
        assertEquals(0, repository.getFetchCount());
        List<CachedSeeListingEntity> cached = dao.getListingsForPage(pageId);
        assertEquals(1, cached.size());
        assertEquals("Already cached", cached.get(0).name);
    }

    @Test
    public void duplicatePrefetchRequestsProduceOnlyOneFetch() throws Exception {
        long pageId = 12345L;
        insertParentArticle(pageId);

        repository.setListingsToReturn(
                List.of(
                        new SeeListing(
                                "Test Museum",
                                51.5,
                                -0.1,
                                null,
                                null,
                                "Test",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null
                        )
                )
        );

        PlaceItem article = createArticle(pageId);

        repository.prefetchListingsForArticle(article);
        repository.prefetchListingsForArticle(article);
        repository.prefetchListingsForArticle(article);

        assertTrue(
                "Prefetch never reached fetchListingsForPage()",
                repository.awaitFetch(2, TimeUnit.SECONDS)
        );

        // Let queued diskIo cache work complete.
        diskIo.submit(() -> {}).get(2, TimeUnit.SECONDS);

        assertEquals(1, repository.getFetchCount());
        List<CachedSeeListingEntity> cached = dao.getListingsForPage(pageId);
        assertEquals(1, cached.size());
    }

    @Test
    public void offlinePrefetchDoesNotFetch() throws Exception {
        NetworkChecker offlineChecker = () -> false;

        TestListingRepository offlineRepository =
                new TestListingRepository(
                        dao,
                        diskIo,
                        new WikiRepository(),
                        offlineChecker
                );

        PlaceItem article = createArticle(12345L);
        offlineRepository.prefetchListingsForArticle(article);
        diskIo.submit(() -> {}).get();

        assertEquals(0, offlineRepository.getFetchCount());
        assertTrue(dao.getListingsForPage(12345L).isEmpty());
    }

    private PlaceItem createArticle(long pageId) {
        return new PlaceItem(
                51.5,
                -0.1,
                "Test article",
                "",
                null,
                pageId,
                PlaceItem.Kind.ARTICLE
        );
    }

    private void insertParentArticle(long pageId) {
        CachedArticleEntity article = new CachedArticleEntity();

        article.pageId = pageId;
        article.title = "Test article";
        article.lat = 51.5;
        article.lon = -0.1;
        article.snippet = "";
        article.thumbUrl = null;

        db.cachedArticleDao().upsert(article);
    }

    private static class TestListingRepository extends ListingRepository {
        private final AtomicInteger fetchCount = new AtomicInteger();
        private volatile List<SeeListing> listingsToReturn = List.of();
        private final CountDownLatch fetchLatch = new CountDownLatch(1);

        TestListingRepository(
                CachedSeeListingDao dao,
                ExecutorService diskIo,
                WikiRepository repo,
                NetworkChecker networkChecker) {
            super(dao, diskIo, repo, networkChecker);
        }

        void setListingsToReturn(List<SeeListing> listings) {
            this.listingsToReturn = listings;
        }

        int getFetchCount() {
            return fetchCount.get();
        }

        boolean awaitFetch(long timeout, TimeUnit unit) throws InterruptedException {
            return fetchLatch.await(timeout, unit);
        }

        @Override
        public void fetchListingsForPage(long pageId, ListingsCallback callback) {
            fetchCount.incrementAndGet();
            callback.onSuccess(listingsToReturn);
            fetchLatch.countDown();
        }
    }
}