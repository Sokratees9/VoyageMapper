package org.okane.voyagemapper.data.local.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.okane.voyagemapper.data.local.dao.DaoUtil.upsertArticle;
import static org.okane.voyagemapper.data.local.dao.DaoUtil.PAGE_1;
import static org.okane.voyagemapper.data.local.dao.DaoUtil.PAGE_2;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.okane.voyagemapper.data.local.AppDatabase;
import org.okane.voyagemapper.data.local.model.CachedSeeListingEntity;

import java.io.IOException;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class CachedSeeListingDaoTest {

    private AppDatabase db;
    private CachedArticleDao articleDao;
    private CachedSeeListingDao seeListingDao;
    private final long unknownPage = 999L;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();

        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();

        articleDao = db.cachedArticleDao();
        seeListingDao = db.cachedSeeListingDao();

        // set up the DB with a some articles and listings to test with
        insertTwoArticlesAndThreeListings();
    }

    @After
    public void closeDb() throws IOException {
        db.close();
    }

    @Test
    public void getListingsForPage_unknownPageID() {
        assertEquals(0, seeListingDao.getListingsForPage(unknownPage).size());
    }

    @Test
    public void insertAll_newArticle() {
        long pageId = 1003L;
        upsertArticle(pageId, "Swatragh", articleDao);
        CachedSeeListingEntity a1 = makeListing(pageId, "The Wall");
        CachedSeeListingEntity a2 = makeListing(pageId, "Davitt's GAC");
        seeListingDao.insertAll(List.of(a1, a2));

        assertEquals(2, seeListingDao.getListingsForPage(pageId).size());
    }

    @Test
    public void deleteForPage_removesOnlyThatPagesListings() {
        seeListingDao.deleteForPage(PAGE_1);

        assertEquals(0, seeListingDao.getListingsForPage(PAGE_1).size());
        assertEquals(1, seeListingDao.getListingsForPage(PAGE_2).size());
    }

    @Test
    public void deleteForPage_unknownPageID() {
        seeListingDao.deleteForPage(unknownPage);

        assertEquals(0, seeListingDao.getListingsForPage(unknownPage).size());
        assertEquals(2, seeListingDao.getListingsForPage(PAGE_1).size());
        assertEquals(1, seeListingDao.getListingsForPage(PAGE_2).size());
    }

    @Test
    public void updateCoordsForListing_changeFromNullToValue() {
        updateCoordsFromNullToValue();
    }

    @Test
    public void updateCoordsForListing_changeFromValueToNull() {
        updateCoordsFromNullToValue();
        seeListingDao.updateCoordsForListing(PAGE_2, "Harbour", null, null);
        CachedSeeListingEntity seeListing = seeListingDao.getListingsForPage(PAGE_2).get(0);
        assertNull(seeListing.lat);
        assertNull(seeListing.lon);
    }

    private void updateCoordsFromNullToValue() {
        CachedSeeListingEntity seeListing = seeListingDao.getListingsForPage(PAGE_2).get(0);
        assertNull(seeListing.lat);
        assertNull(seeListing.lon);
        seeListingDao.updateCoordsForListing(PAGE_2, "Harbour", 10.0, 20.0);
        seeListing = seeListingDao.getListingsForPage(PAGE_2).get(0);
        assertEquals(10.0, seeListing.lat, 0.001);
        assertEquals(20.0, seeListing.lon, 0.001);
    }

    private void insertTwoArticlesAndThreeListings() {
        upsertArticle(PAGE_1, "Kirkcaldy", articleDao);
        upsertArticle(PAGE_2, "Leven", articleDao);

        CachedSeeListingEntity a1 = makeListing(PAGE_1, "Museum");
        CachedSeeListingEntity a2 = makeListing(PAGE_1, "Gallery");
        CachedSeeListingEntity b1 = makeListing(PAGE_2, "Harbour");

        seeListingDao.insertAll(List.of(a1, a2, b1));

        assertEquals(2, seeListingDao.getListingsForPage(PAGE_1).size());
        assertEquals(1, seeListingDao.getListingsForPage(PAGE_2).size());
    }

    private CachedSeeListingEntity makeListing(long pageId, String name) {
        CachedSeeListingEntity entity = new CachedSeeListingEntity();
        entity.pageId = pageId;
        entity.name = name;
        entity.lat = null;
        entity.lon = null;
        entity.phone = null;
        entity.url = null;
        entity.content = "content";
        entity.address = null;
        entity.hours = null;
        entity.price = null;
        entity.wikipediaUrl = null;
        entity.wikidata = null;
        entity.thumbUrl = null;
        return entity;
    }
}
