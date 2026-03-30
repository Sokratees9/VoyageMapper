package org.okane.voyagemapper.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.okane.voyagemapper.data.local.dao.CachedArticleDao;
import org.okane.voyagemapper.data.local.model.CachedArticleEntity;
import org.okane.voyagemapper.ui.model.PlaceItem;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class ArticleRepository {

    public interface ArticleCallback {
        void onSuccess(@Nullable CachedArticleEntity article);
        void onError(Throwable t);
    }

    public interface ArticlesCallback {
        void onSuccess(List<CachedArticleEntity> articles);
        void onError(Throwable t);
    }

    private final CachedArticleDao articleDao;
    private final ExecutorService diskIo;

    public ArticleRepository(
            @NonNull CachedArticleDao articleDao,
            @NonNull ExecutorService diskIo
    ) {
        this.articleDao = articleDao;
        this.diskIo = diskIo;
    }

    public void getArticleByPageId(long pageId, @NonNull ArticleCallback callback) {
        diskIo.execute(() -> {
            try {
                callback.onSuccess(articleDao.getArticleByPageId(pageId));
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public void cacheArticleSummary(@NonNull PlaceItem item) {
        if (item.getKind() != PlaceItem.Kind.ARTICLE) {
            return;
        }

        diskIo.execute(() -> {
            long now = System.currentTimeMillis();

            CachedArticleEntity entity = articleDao.getArticleByPageId(item.getPageId());
            if (entity == null) {
                entity = new CachedArticleEntity();
                entity.pageId = item.getPageId();
                entity.isSaved = false;
            }

            entity.title = item.getTitle();
            entity.snippet = item.getSnippet();
            entity.thumbUrl = item.getThumbUrl();
            entity.articleUrl = "https://en.wikivoyage.org/?curid=" + item.getPageId();
            entity.lat = item.getPosition().latitude;
            entity.lon = item.getPosition().longitude;
            entity.lastFetchedAt = now;
            if (entity.lastViewedAt == 0L) {
                entity.lastViewedAt = now;
            }

            articleDao.upsert(entity);
        });
    }

    public void markArticleViewed(@NonNull PlaceItem item) {
        if (item.getKind() != PlaceItem.Kind.ARTICLE) {
            return;
        }

        diskIo.execute(() -> {
            long now = System.currentTimeMillis();

            CachedArticleEntity entity = articleDao.getArticleByPageId(item.getPageId());
            if (entity == null) {
                entity = new CachedArticleEntity();
                entity.pageId = item.getPageId();
                entity.title = item.getTitle();
                entity.snippet = item.getSnippet();
                entity.thumbUrl = item.getThumbUrl();
                entity.articleUrl = "https://en.wikivoyage.org/?curid=" + item.getPageId();
                entity.lat = item.getPosition().latitude;
                entity.lon = item.getPosition().longitude;
                entity.lastFetchedAt = now;
                entity.isSaved = false;
            }

            entity.lastViewedAt = now;
            articleDao.upsert(entity);
        });
    }

    public void saveArticleRecord(@NonNull PlaceItem item) {
        if (item.getKind() != PlaceItem.Kind.ARTICLE) {
            return;
        }

        diskIo.execute(() -> {
            long now = System.currentTimeMillis();

            CachedArticleEntity entity = articleDao.getArticleByPageId(item.getPageId());
            if (entity == null) {
                entity = new CachedArticleEntity();
                entity.pageId = item.getPageId();
            }

            entity.title = item.getTitle();
            entity.snippet = item.getSnippet();
            entity.thumbUrl = item.getThumbUrl();
            entity.articleUrl = "https://en.wikivoyage.org/?curid=" + item.getPageId();
            entity.lat = item.getPosition().latitude;
            entity.lon = item.getPosition().longitude;
            entity.lastFetchedAt = now;
            entity.lastViewedAt = now;
            entity.isSaved = true;

            articleDao.upsert(entity);
        });
    }

    public void setSaved(long pageId, boolean saved) {
        diskIo.execute(() -> articleDao.setSaved(pageId, saved));
    }

    public void isSaved(long pageId, @NonNull java.util.function.Consumer<Boolean> callback) {
        diskIo.execute(() -> {
            CachedArticleEntity cached = articleDao.getArticleByPageId(pageId);
            callback.accept(cached != null && cached.isSaved);
        });
    }

    public void pruneOldCache() {
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000L);

        diskIo.execute(() -> {
            articleDao.deleteOldUnsavedArticles(thirtyDaysAgo);
            articleDao.trimUnsavedArticlesToMaxCount(500);
        });
    }
}