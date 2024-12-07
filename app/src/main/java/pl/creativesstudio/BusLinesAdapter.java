package pl.creativesstudio;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BusLinesAdapter extends RecyclerView.Adapter<BusLinesAdapter.ViewHolder> {
    private final List<String> busLines;
    private final OnLineClickListener listener;

    public interface OnLineClickListener {
        void onLineClick(String line);
    }

    public BusLinesAdapter(List<String> busLines, OnLineClickListener listener) {
        this.busLines = busLines;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String line = busLines.get(position);
        holder.textView.setText(line);
        holder.itemView.setOnClickListener(v -> listener.onLineClick(line));
    }

    @Override
    public int getItemCount() {
        return busLines.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}
