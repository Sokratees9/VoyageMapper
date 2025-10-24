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
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int position) {
        PlaceResult item = data.get(position);
        ((TextView) h.itemView.findViewById(android.R.id.text1)).setText(item.title);
        ((TextView) h.itemView.findViewById(android.R.id.text2))
                .setText(String.format("Lat %.5f, Lon %.5f", item.lat, item.lon));
        h.itemView.setOnClickListener(v -> onItemClick.onClick(item));
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        VH(@NonNull View itemView) { super(itemView); }
    }
}