package org.okane.voyagemapper.data.local.dao;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Upsert;

import org.okane.voyagemapper.data.local.model.CachedArticleEntity;

import java.util.List;

@Dao
public interface CachedArticleDao {

    @Upsert
    void upsert(CachedArticleEntity article);

    @Query("SELECT * FROM articles WHERE pageId = :pageId LIMIT 1")
        CachedArticleEntity getArticleByPageId(long pageId);

    @Query("SELECT * FROM articles WHERE isSaved = 1 ORDER BY title ASC")
    List<CachedArticleEntity> getSavedArticles();

    @Query("SELECT * FROM articles ORDER BY lastViewedAt DESC LIMIT :limit")
    List<CachedArticleEntity> getRecentArticles(int limit);

    @Query("UPDATE articles SET isSaved = :saved WHERE pageId = :pageId")
    void setSaved(long pageId, boolean saved);

    @Query("UPDATE articles SET lastViewedAt = :timestamp WHERE pageId = :pageId")
    void updateLastViewed(long pageId, long timestamp);

    @Query("DELETE FROM articles WHERE isSaved = 0 AND lastFetchedAt < :cutoff")
    void deleteOldUnsavedArticles(long cutoff);

    @Query("""
        SELECT * FROM articles
        WHERE lat BETWEEN :minLat AND :maxLat
          AND lon BETWEEN :minLon AND :maxLon
        ORDER BY lastViewedAt DESC
    """)
    List<CachedArticleEntity> getArticlesInBounds(
            double minLat,
            double maxLat,
            double minLon,
            double maxLon
    );

    @Query(
            "DELETE FROM articles " +
                    "WHERE isSaved = 0 AND pageId NOT IN (" +
                    "  SELECT pageId FROM articles " +
                    "  WHERE isSaved = 0 " +
                    "  ORDER BY lastViewedAt DESC " +
                    "  LIMIT :maxCount" +
                    ")"
    )
    void trimUnsavedArticlesToMaxCount(int maxCount);
}
