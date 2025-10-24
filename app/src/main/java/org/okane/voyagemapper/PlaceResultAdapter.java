package org.okane.voyagemapper;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class PlaceResultAdapter extends RecyclerView.Adapter<PlaceResultAdapter.VH> {

    public interface OnItemClick {
        void onClick(PlaceResult item);
    }

    private final List<PlaceResult> data = new ArrayList<>();
    private final OnItemClick onItemClick;

    public PlaceResultAdapter(OnItemClick onItemClick) {
        this.onItemClick = onItemClick;
    }

    public void submit(List<PlaceResult> items) {
        data.clear();
        data.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_place_result, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int position) {
        PlaceResult item = data.get(position);
        h.title.setText(item.title);
        h.itemView.setOnClickListener(v -> onItemClick.onClick(item));
    }

    @Override public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title;
        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.placeTitle);
        }
    }
}