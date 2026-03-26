package org.okane.voyagemapper.ui;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.okane.voyagemapper.R;
import org.okane.voyagemapper.data.local.model.CachedArticleEntity;

import java.util.ArrayList;
import java.util.List;

public class SavedArticlesAdapter extends RecyclerView.Adapter<SavedArticlesAdapter.ViewHolder> {

    public interface OnArticleClickListener {
        void onArticleClick(CachedArticleEntity article);
    }

    private final List<CachedArticleEntity> items = new ArrayList<>();
    private final OnArticleClickListener listener;

    public SavedArticlesAdapter(OnArticleClickListener listener) {
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setItems(List<CachedArticleEntity> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        // Change this is you think the data set will become very big
        // Remove the suppress and see the warning to work out what to do
        // Seems like DiffUtil.calculateDiff will be needed
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_saved_article, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CachedArticleEntity item = items.get(position);
        holder.title.setText(item.title);
        holder.itemView.setOnClickListener(v -> listener.onArticleClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView title;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.placeTitle);
        }
    }
}