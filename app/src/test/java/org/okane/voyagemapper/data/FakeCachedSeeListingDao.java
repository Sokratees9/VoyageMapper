package org.okane.voyagemapper.data;

import org.okane.voyagemapper.data.local.dao.CachedSeeListingDao;
import org.okane.voyagemapper.data.local.model.CachedSeeListingEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FakeCachedSeeListingDao implements CachedSeeListingDao {
    private final Map<Long, List<CachedSeeListingEntity>> store = new HashMap<>();

    @Override
    public List<CachedSeeListingEntity> getListingsForPage(long pageId) {
        List<CachedSeeListingEntity> listings = store.get(pageId);
        if (listings == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(listings);
    }

    @Override
    public void insertAll(List<CachedSeeListingEntity> listings) {
        for (CachedSeeListingEntity entity : listings) {
            store.computeIfAbsent(entity.pageId, k -> new ArrayList<>()).add(entity);
        }
    }

    @Override
    public void deleteForPage(long pageId) {
        store.remove(pageId);
    }

    @Override
    public void updateCoordsForListing(long pageId, String name, Double lat, Double lon) {
        List<CachedSeeListingEntity> listings = store.get(pageId);
        if (listings == null) return;

        for (CachedSeeListingEntity entity : listings) {
            if (name.equals(entity.name)) {
                entity.lat = lat;
                entity.lon = lon;
            }
        }
    }

    public void put(long pageId, List<CachedSeeListingEntity> listings) {
        store.put(pageId, new ArrayList<>(listings));
    }
}
