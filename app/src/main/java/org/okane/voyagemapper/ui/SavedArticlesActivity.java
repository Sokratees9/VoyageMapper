package org.okane.voyagemapper.ui;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.snackbar.Snackbar;

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

        View root = findViewById(R.id.root);
        Toolbar toolbar = findViewById(R.id.toolbar);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
            );

            toolbar.setPadding(
                    toolbar.getPaddingLeft(),
                    systemBars.top,
                    toolbar.getPaddingRight(),
                    toolbar.getPaddingBottom()
            );
            return insets;
        });

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

        ItemTouchHelper.SimpleCallback callback =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                    @Override
                    public boolean onMove(
                            @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onChildDraw(
                            @NonNull Canvas c,
                            @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            float dX,
                            float dY,
                            int actionState,
                            boolean isCurrentlyActive
                    ) {
                        View itemView = viewHolder.itemView;

                        if (dX < 0) { // Swiping left
                            Paint backgroundPaint = new Paint();
                            int backgroundColor = MaterialColors.getColor(
                                    itemView, com.google.android.material.R.attr.colorErrorContainer);
                            backgroundPaint.setColor(backgroundColor);
                            c.drawRect(
                                    itemView.getRight() + dX,
                                    itemView.getTop(),
                                    itemView.getRight(),
                                    itemView.getBottom(),
                                    backgroundPaint
                            );

                            Drawable deleteIcon = AppCompatResources.getDrawable(
                                    itemView.getContext(), R.drawable.ic_delete);

                            if (deleteIcon != null) {
                                int iconColor = MaterialColors.getColor(
                                        itemView, com.google.android.material.R.attr.colorOnErrorContainer);
                                DrawableCompat.setTint(deleteIcon, iconColor);

                                int iconMargin = dpToPx(itemView, 24);
                                int iconSize = dpToPx(itemView, 24);
                                int iconTop = itemView.getTop() + (itemView.getHeight() - iconSize) / 2;
                                int iconLeft = itemView.getRight() - iconMargin - iconSize;

                                deleteIcon.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize);
                                deleteIcon.draw(c);
                            }
                        }

                        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        int position = viewHolder.getBindingAdapterPosition();
                        if (position == RecyclerView.NO_POSITION) {
                            return;
                        }
                        CachedArticleEntity article = adapter.getItem(position);
                        removeSavedArticle(article);

                        Snackbar.make(recyclerView, R.string.article_removed, Snackbar.LENGTH_LONG)
                                .setAction(R.string.undo, v -> restoreSavedArticle(article))
                                .show();
                    }

                    private void removeSavedArticle(CachedArticleEntity article) {
                        adapter.removeArticle(article);
                        diskIo.execute(() -> articleDao.setSaved(article.pageId, false));
                    }

                    private void restoreSavedArticle(CachedArticleEntity article) {
                        adapter.restoreSavedArticle(article);
                        diskIo.execute(() -> articleDao.upsert(article));
                    }

                    private static int dpToPx(View view, int dp) {
                        float density = view.getResources().getDisplayMetrics().density;
                        return Math.round(dp * density);
                    }
                };
        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
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
