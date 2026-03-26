package org.okane.voyagemapper.data;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okane.voyagemapper.data.local.model.CachedSeeListingEntity;
import org.okane.voyagemapper.service.FakeExecutorService;
import org.okane.voyagemapper.service.FakeWikiRepository;

import java.util.List;

class ListingRepositoryTest {

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void getCachedListingsForPage() {
    }

    @Test
    void fetchListingsForPage() {
    }

    @Test
    void cacheListingsForArticle() {
    }

    @Test
    void mapCachedEntitiesToSeeListings() {
    }

    @Test
    void prefetchListingsForArticle() {
    }

    @Test
    void updateCoordsForListing() {
    }

    @Test
    void deleteListingsForPage() {
        FakeCachedSeeListingDao dao = new FakeCachedSeeListingDao();
        FakeWikiRepository wikiRepository = new FakeWikiRepository();
        FakeExecutorService executor = new FakeExecutorService();

        ListingRepository classUnderTest = new ListingRepository(dao, executor, wikiRepository, () -> true);

        CachedSeeListingEntity a1 = makeListing(100L, "A1");
        CachedSeeListingEntity a2 = makeListing(100L, "A2");
        CachedSeeListingEntity b1 = makeListing(200L, "B1");

        dao.insertAll(List.of(a1, a2, b1));

        assertEquals(2, dao.getListingsForPage(100L).size());
        assertEquals(1, dao.getListingsForPage(200L).size());

        classUnderTest.deleteListingsForPage(100L);

        assertEquals(0, dao.getListingsForPage(100L).size());
        assertEquals(1, dao.getListingsForPage(200L).size());
    }

    private CachedSeeListingEntity makeListing(long pageId, String name) {
        CachedSeeListingEntity entity = new CachedSeeListingEntity();
        entity.pageId = pageId;
        entity.name = name;
        return entity;
    }
}