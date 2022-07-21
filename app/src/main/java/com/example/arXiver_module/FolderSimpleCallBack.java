package com.example.arXiver_module;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class FolderSimpleCallBack extends ItemTouchHelper.SimpleCallback {

    final FolderRecyclerViewAdapter folderAdapter;
    final FolderActivityAdapterListener listener;
    public FolderSimpleCallBack(FolderRecyclerViewAdapter folderAdapter, FolderActivityAdapterListener listener) {
        super(ItemTouchHelper.UP |ItemTouchHelper.DOWN|ItemTouchHelper.LEFT|ItemTouchHelper.RIGHT | ItemTouchHelper.START | ItemTouchHelper.END, 0);
        this.folderAdapter = folderAdapter;
        this.listener = listener;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        int fromPos = viewHolder.getAdapterPosition();
        int toPos = target.getAdapterPosition();
        folderAdapter.push(fromPos,toPos);
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        listener.onMove(true);

        // We only want the active item to change
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            if (viewHolder instanceof FolderRecyclerViewAdapter.FolderViewHolder) {
                // Let the view holder know that this item is being moved or dragged
                ( (FolderRecyclerViewAdapter.FolderViewHolder) viewHolder).onItemSelected();
            }
        }

        super.onSelectedChanged(viewHolder, actionState);
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {

        super.clearView(recyclerView, viewHolder);
        folderAdapter.updateOrder();
        listener.onMove(false);

        //viewHolder.itemView.setAlpha(1.0f);
        if (viewHolder instanceof FolderRecyclerViewAdapter.FolderViewHolder) {
            // Tell the view holder it's time to restore the idle state
            ((FolderRecyclerViewAdapter.FolderViewHolder) viewHolder).onItemClear();
        }
    }
}
