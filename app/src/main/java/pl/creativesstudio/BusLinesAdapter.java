package pl.creativesstudio;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter for displaying a list of bus lines in a RecyclerView.
 * Handles item clicks to trigger actions when a bus line is selected.
 */
public class BusLinesAdapter extends RecyclerView.Adapter<BusLinesAdapter.ViewHolder> {

    /**
     * Interface for handling click events on bus lines.
     */
    public interface OnLineClickListener {
        /**
         * Called when a bus line is clicked.
         *
         * @param line The selected bus line.
         */
        void onLineClick(String line);
    }

    /**
     * List of bus lines to display.
     */
    private final List<String> busLines;

    /**
     * Listener for handling click events on bus lines.
     */
    private final OnLineClickListener listener;

    /**
     * Constructs a new `BusLinesAdapter`.
     *
     * @param busLines A list of strings representing bus lines to display.
     * @param listener An instance of `OnLineClickListener` to handle item clicks.
     */
    public BusLinesAdapter(List<String> busLines, OnLineClickListener listener) {
        this.busLines = busLines;
        this.listener = listener;
    }

    /**
     * Creates a new ViewHolder for the RecyclerView.
     *
     * @param parent   The parent ViewGroup.
     * @param viewType The view type of the new view (unused in this implementation).
     * @return A new ViewHolder instance.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds data to the ViewHolder for a specific position.
     *
     * @param holder   The ViewHolder to bind data to.
     * @param position The position of the item in the data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String line = busLines.get(position);
        holder.textView.setText(line);
        holder.itemView.setOnClickListener(v -> listener.onLineClick(line));
    }

    /**
     * Returns the total number of items in the data set.
     *
     * @return The number of items in the list.
     */
    @Override
    public int getItemCount() {
        return busLines.size();
    }

    /**
     * ViewHolder class for holding views in the RecyclerView.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        /**
         * TextView for displaying the bus line name.
         */
        TextView textView;

        /**
         * Constructs a new ViewHolder.
         *
         * @param itemView The view for a single RecyclerView item.
         */
        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}
