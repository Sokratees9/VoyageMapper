package org.okane.voyagemapper.data.local.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "articles")
public class CachedArticleEntity {
    @PrimaryKey
    public long pageId;

    @NonNull
    public String title;

    @Nullable
    public String snippet;

    @Nullable
    public String thumbUrl;

    @Nullable
    public String articleUrl;

    @Nullable
    public String fullContent;   // whatever you show in detail view

    public double lat;
    public double lon;

    public long lastFetchedAt;
    public long lastViewedAt;

    public boolean isSaved;
}