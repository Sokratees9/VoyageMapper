package org.okane.voyagemapper.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.okane.voyagemapper.MapActivity;
import org.okane.voyagemapper.R;
import org.okane.voyagemapper.data.local.AppDatabase;
import org.okane.voyagemapper.data.local.dao.CachedArticleDao;
import org.okane.voyagemapper.data.local.model.CachedArticleEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SavedArticlesActivity extends AppCompatActivity {
    private CachedArticleDao articleDao;
    private final ExecutorService diskIo = Executors.newSingleThreadExecutor();

    private RecyclerView recyclerView;
    private TextView emptyView;
    private SavedArticlesAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_saved_articles);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.saved_articles);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.emptyView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new SavedArticlesAdapter(article -> {
            Intent intent = new Intent(SavedArticlesActivity.this, MapActivity.class);
            intent.putExtra(MapActivity.EXTRA_OPEN_SAVED_ARTICLE, true);
            intent.putExtra(MapActivity.EXTRA_PAGE_ID, article.pageId);
            intent.putExtra(MapActivity.EXTRA_LAT, article.lat);
            intent.putExtra(MapActivity.EXTRA_LON, article.lon);
            intent.putExtra(MapActivity.EXTRA_TITLE, article.title);
            intent.putExtra(MapActivity.EXTRA_SNIPPET, article.snippet);
            intent.putExtra(MapActivity.EXTRA_THUMB_URL, article.thumbUrl);
            startActivity(intent);
            finish();
        });

        recyclerView.setAdapter(adapter);

        articleDao = AppDatabase.getInstance(this).cachedArticleDao();
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadSavedArticles();
    }

    private void loadSavedArticles() {
        diskIo.execute(() -> {
            List<CachedArticleEntity> articles = articleDao.getSavedArticles();

            runOnUiThread(() -> {
                adapter.setItems(articles);
                boolean isEmpty = articles == null || articles.isEmpty();
                emptyView.setVisibility(isEmpty ? TextView.VISIBLE : TextView.GONE);
                recyclerView.setVisibility(isEmpty ? RecyclerView.GONE : RecyclerView.VISIBLE);
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        diskIo.shutdown();
    }
}
