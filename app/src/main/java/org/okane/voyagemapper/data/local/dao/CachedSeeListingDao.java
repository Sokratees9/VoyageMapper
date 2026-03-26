package org.okane.voyagemapper.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.okane.voyagemapper.data.local.model.CachedSeeListingEntity;

import java.util.List;

@Dao
public interface CachedSeeListingDao {
    @Query("SELECT * FROM see_listings WHERE pageId = :pageId ORDER BY name ASC")
    List<CachedSeeListingEntity> getListingsForPage(long pageId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CachedSeeListingEntity> listings);

    @Query("DELETE FROM see_listings WHERE pageId = :pageId")
    void deleteForPage(long pageId);

    @Query("UPDATE see_listings " +
            "SET lat = :lat, lon = :lon " +
            "WHERE pageId = :pageId AND name = :name")
    void updateCoordsForListing(long pageId, String name, Double lat, Double lon);
}