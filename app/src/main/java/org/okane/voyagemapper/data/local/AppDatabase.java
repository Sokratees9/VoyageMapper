package org.okane.voyagemapper.data.local;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import org.okane.voyagemapper.data.local.dao.CachedArticleDao;
import org.okane.voyagemapper.data.local.dao.CachedSeeListingDao;
import org.okane.voyagemapper.data.local.model.CachedArticleEntity;
import org.okane.voyagemapper.data.local.model.CachedSeeListingEntity;

@Database(
        entities = {CachedArticleEntity.class, CachedSeeListingEntity.class},
        version = 3,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract CachedArticleDao cachedArticleDao();
    public abstract CachedSeeListingDao cachedSeeListingDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "voyagemapper.db"
                    ).fallbackToDestructiveMigration().build();
                    Log.d("RoomDebug", "AppDatabase opened");
                }
            }
        }
        return INSTANCE;
    }
}