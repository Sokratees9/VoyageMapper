package org.okane.voyagemapper.ui;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.okane.voyagemapper.R;
import org.okane.voyagemapper.model.PlaceResult;

import java.util.ArrayList;
import java.util.List;

public class PlaceResultAdapter extends RecyclerView.Adapter<PlaceResultAdapter.PlaceResultViewHolder> {

    public interface OnItemClick {
        void onClick(PlaceResult item);
    }

    private final List<PlaceResult> data = new ArrayList<>();
    private final OnItemClick onItemClick;

    public PlaceResultAdapter(OnItemClick onItemClick) {
        this.onItemClick = onItemClick;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void submit(List<PlaceResult> items) {
        data.clear();
        data.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull @Override public PlaceResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_place_result, parent, false);
        return new PlaceResultViewHolder(v);
    }

    @Override public void onBindViewHolder(@NonNull PlaceResultViewHolder h, int position) {
        PlaceResult item = data.get(position);
        h.title.setText(item.title());
        h.itemView.setOnClickListener(v -> onItemClick.onClick(item));
    }

    @Override public int getItemCount() {
        return data.size();
    }

    public static class PlaceResultViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        PlaceResultViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.placeTitle);
        }
    }
}