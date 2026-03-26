package org.okane.voyagemapper.data.local.dao;

import org.okane.voyagemapper.data.local.model.CachedArticleEntity;

public class DaoUtil {

    public static final long PAGE_1 = 1001L;
    public static final long PAGE_2 = 1002L;

    public static CachedArticleEntity createArticleEntry(long pageId, String title) {
        CachedArticleEntity article = new CachedArticleEntity();
        article.pageId = pageId;
        article.title = title;
        article.snippet = "snippet";
        article.thumbUrl = null;
        article.articleUrl = "https://en.wikivoyage.org/?curid=" + pageId;
        article.fullContent = null;
        article.lat = 56.0;
        article.lon = -3.0;
        article.lastFetchedAt = System.currentTimeMillis();
        article.lastViewedAt = System.currentTimeMillis();
        article.isSaved = true;
        return article;
    }
    public static void upsertArticle(long pageId, String title, CachedArticleDao articleDao) {
        upsertArticle(createArticleEntry(pageId, title), articleDao);
    }

    public static void upsertArticle(CachedArticleEntity article, CachedArticleDao articleDao) {
        articleDao.upsert(article);
    }
}
