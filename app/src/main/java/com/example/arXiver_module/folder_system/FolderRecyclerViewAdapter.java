package com.example.arXiver_module.folder_system;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arXiver_module.R;

import java.util.ArrayList;
import java.util.Collections;

public class FolderRecyclerViewAdapter extends RecyclerView.Adapter<FolderRecyclerViewAdapter.FolderViewHolder> {

    public final Context context;
    private static ArrayList<Folder> folderArray;
    final FolderActivityAdapterListener listener;

    public FolderRecyclerViewAdapter(Context context, ArrayList<Folder> folderArray, FolderActivityAdapterListener listener){
        this.context = context;
        FolderRecyclerViewAdapter.folderArray = folderArray;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new FolderViewHolder(
                LayoutInflater.from(context).inflate(R.layout.folder_layout, parent, false)
                , listener );
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateFolders(ArrayList<Folder> folderArray){
        FolderRecyclerViewAdapter.folderArray = folderArray;
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
        holder.folderTextView.setText(folderArray.get(position).folderName);
        if (folderArray.get(position).folderName.equals(context.getResources().getString(R.string.saved_papers))){
            holder.folderImageView.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(),R.drawable.ic_baseline_archive_24_green,context.getTheme()));
        } else{
            holder.folderImageView.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(),R.drawable.ic_baseline_folder_24,context.getTheme()));
        }
    }

    @Override
    public int getItemCount() {
        return folderArray.size();
    }

    public void  push(int fromPos, int toPos){
        // all items from fromPos to toPos change
        int dPos;
        if (toPos<fromPos){
            dPos = -1;
        }else{
            dPos = 1;
        }

        while (fromPos!=toPos){
            Collections.swap(folderArray,fromPos, fromPos + dPos);
            notifyItemMoved(fromPos,fromPos + dPos);
            fromPos  = fromPos + dPos;
        }
    }

    public void updateOrder(){
        listener.updateOrder(folderArray);
    }

    public static class FolderViewHolder extends RecyclerView.ViewHolder {

        public final TextView folderTextView;
        public final CardView folderCardView;
        public final ImageView folderImageView;
        final FolderActivityAdapterListener listener;


        public FolderViewHolder(@NonNull View itemView, FolderActivityAdapterListener listener) {
            super(itemView);

            folderTextView = itemView.findViewById(R.id.folderTextView);
            folderCardView = itemView.findViewById(R.id.folderCardView);
            folderImageView = itemView.findViewById(R.id.folderImageView);
            this.listener=listener;

            folderImageView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                Folder folder = folderArray.get(pos);
                listener.openFolder(folder);
            });
        }

        public void onItemSelected() {
            folderCardView.setCardBackgroundColor(listener.activity.getResources().getColor(R.color.selected_color));
        }

        public void onItemClear() {
            folderCardView.setCardBackgroundColor(listener.activity.getResources().getColor(R.color.gray));
        }
    }

}
