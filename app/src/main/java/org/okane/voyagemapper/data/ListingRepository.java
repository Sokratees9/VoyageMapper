package org.okane.voyagemapper.data;

import android.util.Log;

import androidx.annotation.NonNull;

import org.okane.voyagemapper.data.local.dao.CachedSeeListingDao;
import org.okane.voyagemapper.data.local.model.CachedSeeListingEntity;
import org.okane.voyagemapper.model.SeeListing;
import org.okane.voyagemapper.service.NetworkChecker;
import org.okane.voyagemapper.service.PageContentResponse;
import org.okane.voyagemapper.service.WikiRepository;
import org.okane.voyagemapper.ui.model.PlaceItem;
import org.okane.voyagemapper.util.TemplateMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ListingRepository {
    private final WikiRepository repo;
    private final ExecutorService diskIo;
    private final CachedSeeListingDao seeListingDao;
    private final NetworkChecker networkChecker;

    public ListingRepository(@NonNull CachedSeeListingDao seeListingDao,
            @NonNull ExecutorService diskIo,
            @NonNull WikiRepository repo,
            @NonNull NetworkChecker networkChecker) {
        this.repo = repo;
        this.diskIo = diskIo;
        this.seeListingDao = seeListingDao;
        this.networkChecker = networkChecker;
    }

    public void getCachedListingsForPage(long pageId, @NonNull ListingsCallback callback) {
        diskIo.execute(() -> {
            try {
                List<CachedSeeListingEntity> cached = seeListingDao.getListingsForPage(pageId);
                List<SeeListing> listings = mapCachedEntitiesToSeeListings(cached);
                callback.onSuccess(listings);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public void fetchListingsForPage(long pageId, ListingsCallback callback) {
        repo.fetchPageWikitext(pageId, new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<PageContentResponse> call,
                    @NonNull Response<PageContentResponse> res) {
                if (!res.isSuccessful() || res.body() == null || res.body().query == null
                        || res.body().query.pages == null || res.body().query.pages.isEmpty()) {
                    callback.onError(new RuntimeException("Server error: " + res.code()));
                    return;
                }

                String wikitext = res.body().query.pages.get(0).revisions.get(0).slots.main.content;
                List<SeeListing> listings = TemplateMatcher.parse(wikitext);
                callback.onSuccess(listings);
            }

            @Override
            public void onFailure(@NonNull Call<PageContentResponse> call, @NonNull Throwable t) {
                callback.onError(t);
            }
        });
    }

    public void cacheListingsForArticle(long pageId, List<SeeListing> listings) {
        if (listings == null) {
            return;
        }

        diskIo.execute(() -> {
            try {
                List<CachedSeeListingEntity> entities = new ArrayList<>();
                Log.d("RoomDebug", "Caching " + entities.size() + " listings for page " + pageId);

                for (SeeListing s : listings) {
                    CachedSeeListingEntity e = new CachedSeeListingEntity();
                    e.pageId = pageId;
                    e.name = s.name();
                    e.lat = s.lat();
                    e.lon = s.lon();
                    e.phone = s.phone();
                    e.url = s.url();
                    e.content = s.content();
                    e.address = s.address();
                    e.hours = s.hours();
                    e.price = s.price();
                    e.wikipediaUrl = s.wikipediaUrl();
                    e.wikidata = s.wikidata();
                    e.thumbUrl = s.thumbUrl();
                    entities.add(e);
                }

                seeListingDao.deleteForPage(pageId);
                seeListingDao.insertAll(entities);
                int count = seeListingDao.getListingsForPage(pageId).size();
                Log.d("RoomDebug", "After insert, page " + pageId + " has " + count + " cached listings");
            } catch (Exception ex) {
                Log.e("cacheListingsForArticle", "Failed to cache listings for page " + pageId, ex);
            }
        });
    }

    public List<SeeListing> mapCachedEntitiesToSeeListings(List<CachedSeeListingEntity> cached) {
        List<SeeListing> listings = new ArrayList<>();
        if (cached == null) {
            return listings;
        }
        for (CachedSeeListingEntity e : cached) {
            listings.add(new SeeListing(
                    e.name,
                    e.lat,
                    e.lon,
                    e.phone,
                    e.url,
                    e.content,
                    e.address,
                    e.hours,
                    e.price,
                    e.wikipediaUrl,
                    e.wikidata,
                    e.thumbUrl
            ));
        }
        return listings;
    }

    public void prefetchListingsForArticle(PlaceItem article) {
        if (article == null || article.getKind() != PlaceItem.Kind.ARTICLE) {
            return;
        }

        diskIo.execute(() -> {
            try {
                List<CachedSeeListingEntity> cached = seeListingDao.getListingsForPage(article.getPageId());

                if (cached != null && !cached.isEmpty()) {
                    return;
                }

                if (!networkChecker.isNetworkAvailable()) {
                    return;
                }

                fetchListingsForPage(article.getPageId(), new ListingsCallback() {
                    @Override
                    public void onSuccess(List<SeeListing> listings) {
                        if (listings == null || listings.isEmpty()) {
                            return;
                        }
                        cacheListingsForArticle(article.getPageId(), listings);
                    }

                    @Override
                    public void onError(Throwable t) {
                        Log.w("prefetchListings", "Failed for " + article.getTitle(), t);
                    }
                 });
            } catch (Exception e) {
                Log.e("ListingRepository", "Prefetch crashed for page " + article.getPageId(), e);
            }
        });
    }

    public void updateCoordsForListing(long pageId, String name, Double lat, Double lon) {
        diskIo.execute(() -> seeListingDao.updateCoordsForListing(pageId, name, lat, lon));
    }

    public void deleteListingsForPage(long pageId) {
        diskIo.execute(() -> seeListingDao.deleteForPage(pageId));
    }

    public interface ListingsCallback {
        void onSuccess(List<SeeListing> listings);
        void onError(Throwable t);
    }
}
