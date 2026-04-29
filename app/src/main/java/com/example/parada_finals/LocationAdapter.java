package com.example.parada_finals;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.LocationViewHolder> {

    private List<String> locationNames;
    private OnLocationClickListener listener;

    public interface OnLocationClickListener {
        void onLocationClick(String locationName);
    }

    public LocationAdapter(List<String> locationNames, OnLocationClickListener listener) {
        this.locationNames = locationNames;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_location_search, parent, false);
        return new LocationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
        String name = locationNames.get(position);
        holder.tvName.setText(name);
        
        // Dynamic description based on name for premium feel
        if (name.equals("Current Location")) {
            holder.tvDesc.setText("Your present GPS position");
        } else if (name.contains("Mall")) {
            holder.tvDesc.setText("Shopping and lifestyle hub");
        } else if (name.contains("Park")) {
            holder.tvDesc.setText("Public recreational area");
        } else {
            holder.tvDesc.setText("Popular landmark / Barangay");
        }

        holder.itemView.setOnClickListener(v -> listener.onLocationClick(name));
    }

    @Override
    public int getItemCount() {
        return locationNames.size();
    }

    public void updateList(List<String> newList) {
        this.locationNames = newList;
        notifyDataSetChanged();
    }

    static class LocationViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDesc;
        LocationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvLocationName);
            tvDesc = itemView.findViewById(R.id.tvLocationDesc);
        }
    }
}
