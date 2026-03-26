package org.okane.voyagemapper.data.local.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "see_listings",
        foreignKeys = @ForeignKey(
                entity = CachedArticleEntity.class,
                parentColumns = "pageId",
                childColumns = "pageId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("pageId")}
)
public class CachedSeeListingEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long pageId;

    @NonNull
    public String name;

    @Nullable
    public String description;

    @Nullable
    public String address;

    @Nullable
    public String hours;

    @Nullable
    public String price;

    @Nullable
    public String phone;

    @Nullable
    public String url;

    @Nullable
    public String thumbUrl;

    public Double lat;
    public Double lon;
    public String content;
    public String wikipediaUrl;
    public String wikidata;
}