package org.okane.voyagemapper.data.local.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.okane.voyagemapper.data.local.dao.DaoUtil.upsertArticle;
import static org.okane.voyagemapper.data.local.dao.DaoUtil.PAGE_1;
import static org.okane.voyagemapper.data.local.dao.DaoUtil.PAGE_2;

import android.content.Context;
import android.util.Log;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.okane.voyagemapper.data.local.AppDatabase;
import org.okane.voyagemapper.data.local.model.CachedArticleEntity;

import java.io.IOException;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class CachedArticleDaoTest {
    private AppDatabase db;
    private CachedArticleDao articleDao;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();

        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .setQueryCallback(
                        (sqlQuery, bindArgs) -> Log.i("RoomSQL", "SQL: " + sqlQuery + " ARGS: " + bindArgs),
                        Runnable::run
                )
                .build();
        articleDao = db.cachedArticleDao();

        // set up the DB with a some articles to test with
        upsertArticle(PAGE_1, "Kirkcaldy", articleDao);
        upsertArticle(PAGE_2, "Leven", articleDao);
    }

    @After
    public void closeDb() throws IOException {
        db.close();
    }

    @Test
    public void upsert_checksForExisting() {
        // two articles have already been upserted as part of set up
        assertEquals(2, articleDao.getSavedArticles().size());


    }

    @Test
    public void upsert_addNewArticle() {
        long pageId = 1003L;
        upsertArticle(pageId, "Swatragh", articleDao);
        assertEquals(3, articleDao.getSavedArticles().size());
        assertEquals("Swatragh", articleDao.getSavedArticles().get(2).title);
    }

    @Test
    public void upsert_existing() {
        assertEquals(2, articleDao.getSavedArticles().size());
        upsertArticle(PAGE_1, "Kirkcaldy", articleDao);
        assertEquals(2, articleDao.getSavedArticles().size());
    }

    @Test
    public void upsert_existingWithChangesToAllFieldsExceptPageId() {
        List<CachedArticleEntity> savedArticles = articleDao.getSavedArticles();
        assertEquals(2, savedArticles.size());
        CachedArticleEntity article = savedArticles.get(0);
        assertNull(article.fullContent);

        // change all the data apart from the pageId
        String title = "test title";
        String fullContent = "test fullContent";
        String snippet = "test snippet";
        String thumbUrl = "thumbUrl";
        String articleUrl = "articleUrl";
        double lat = 10.5;
        double lon = 11.5;
        long lastFetchedAt = System.currentTimeMillis();
        long lastViewedAt = System.currentTimeMillis();
        boolean isSaved = false;

        article.title = title;
        article.fullContent = fullContent;
        article.snippet = snippet;
        article.thumbUrl = thumbUrl;
        article.articleUrl = articleUrl;
        article.lat = lat;
        article.lon = lon;
        article.lastFetchedAt = lastFetchedAt;
        article.lastViewedAt = lastViewedAt;
        article.isSaved = isSaved;
        upsertArticle(article, articleDao);

        // can't use getSavedArticles to check the number of rows is still 2
        savedArticles = articleDao.getRecentArticles(500);
        assertEquals(2, savedArticles.size());
        article = articleDao.getArticleByPageId(article.pageId);

        assertEquals(title, article.title);
        assertEquals(fullContent, article.fullContent);
        assertEquals(snippet, article.snippet);
        assertEquals(thumbUrl, article.thumbUrl);
        assertEquals(articleUrl, article.articleUrl);
        assertEquals(lat, article.lat, 0.001);
        assertEquals(lon, article.lon, 0.001);
        assertEquals(lastFetchedAt, article.lastFetchedAt);
        assertEquals(lastViewedAt, article.lastViewedAt);
        assertEquals(isSaved, article.isSaved);
    }

    @Test
    public void upsert_existingWithNewPageID() {
        // Not sure if this is a valid use case, but we can test it anyway
        List<CachedArticleEntity> savedArticles = articleDao.getSavedArticles();
        assertEquals(2, savedArticles.size());
        CachedArticleEntity article = savedArticles.get(0);
        long pageId = 1003L;
        article.pageId = pageId;
        upsertArticle(article, articleDao);

        assertEquals(3, articleDao.getSavedArticles().size());
        assertEquals(1003L, articleDao.getArticleByPageId(pageId).pageId);
    }

    @Test
    public void getArticleByPageId_unknownPageID() {
        assertNull(articleDao.getArticleByPageId(999L));
    }

    @Test
    public void getRecentArticles() {
        assertEquals(1, articleDao.getRecentArticles(1).size());
        assertEquals(2, articleDao.getRecentArticles(500).size());
        // -ve implies no limit in SQLite, so all rows are returned
        assertEquals(2, articleDao.getRecentArticles(-1).size());
    }

    @Test
    public void setSaved_changeSavedValue() {
        assertEquals(2, articleDao.getSavedArticles().size());
        CachedArticleEntity article = articleDao.getArticleByPageId(PAGE_1);
        articleDao.setSaved(article.pageId, false);
        assertEquals(1, articleDao.getSavedArticles().size());
        articleDao.setSaved(article.pageId, true);
        assertEquals(2, articleDao.getSavedArticles().size());
    }

    @Test
    public void deleteOldUnsavedArticles() throws InterruptedException {
        // Given two existing saved articles, mark them as unsaved,
        // sleep for 1.5 seconds, add another one, check there are three articles
        List<CachedArticleEntity> savedArticles = articleDao.getSavedArticles();
        assertEquals(2, savedArticles.size());
        for (CachedArticleEntity unsaved : savedArticles) {
            articleDao.setSaved(unsaved.pageId, false);
        }
        Thread.sleep(1500);
        CachedArticleEntity maghera = DaoUtil.createArticleEntry(1004L, "Maghera");
        maghera.isSaved = false;
        DaoUtil.upsertArticle(maghera, articleDao);
        assertEquals(3, articleDao.getRecentArticles(500).size());

        // When I delete all articles older that 1 second
        long cutoff = System.currentTimeMillis() - 1000;
        articleDao.deleteOldUnsavedArticles(cutoff);

        // Then there should be only one article left
        assertEquals(1, articleDao.getRecentArticles(500).size());
    }
}
