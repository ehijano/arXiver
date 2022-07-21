package com.example.arXiver_module;

import static com.example.arXiver_module.ParentActivity.FONT_PREFS;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arXiver_module.items.GeneralItem;

import java.util.Objects;

public class BrowseSwipeToDeleteCallBack extends ItemTouchHelper.SimpleCallback {

    private final BrowseRecyclerViewAdapter mAdapter;
    private final Drawable leftDrawable;
    private final Drawable rightDrawable;
    private final float scale;

    public BrowseSwipeToDeleteCallBack(BrowseRecyclerViewAdapter adapter, Drawable leftDrawable, Drawable rightDrawable){
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.leftDrawable = leftDrawable;
        this.rightDrawable = rightDrawable;
        mAdapter = adapter;
        SharedPreferences fontPreferences = mAdapter.context.getSharedPreferences(FONT_PREFS, Context.MODE_PRIVATE);
        this.scale = fontPreferences.getFloat("FONT_SIZE", 1.0f) * mAdapter.context.getResources().getDisplayMetrics().xdpi / 160;

    }

    @Override
    public int getSwipeDirs(@NonNull RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        if(viewHolder.getItemViewType() == GeneralItem.TYPE_PAPER && !mAdapter.isSelectMode){
            return super.getSwipeDirs(recyclerView,viewHolder);
        }else{
            return 0;
        }
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onChildDraw (@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive){
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

        if( actionState==ItemTouchHelper.ACTION_STATE_SWIPE && isCurrentlyActive && viewHolder.getItemViewType()==GeneralItem.TYPE_PAPER && !mAdapter.isSelectMode) {
            if (dX < 0) {
                View itemView = viewHolder.itemView;

                Paint fillPaint = new Paint();
                fillPaint.setStyle(Paint.Style.FILL);
                fillPaint.setColor(itemView.getResources().getColor(R.color.light_red));
                c.drawRoundRect(new RectF(itemView.getLeft(), itemView.getTop(), itemView.getRight(), itemView.getBottom())
                        ,50,50, fillPaint);

                int size = (int) (50*scale);
                int margin = 16;
                int top = itemView.getTop()+(itemView.getBottom()-itemView.getTop())/2-size/2;
                int bot = itemView.getTop()+(itemView.getBottom()-itemView.getTop())/2+size/2;
                int left = itemView.getRight()-margin-size;
                int right = itemView.getRight()-margin;

                Objects.requireNonNull(leftDrawable).setBounds(left, top, right, bot);
                leftDrawable.draw(c);
            }else if(dX>0){
                View itemView = viewHolder.itemView;

                Paint fillPaint = new Paint();
                fillPaint.setStyle(Paint.Style.FILL);
                fillPaint.setColor(itemView.getResources().getColor(R.color.green));
                c.drawRoundRect(new RectF(itemView.getLeft(), itemView.getTop(), itemView.getRight(), itemView.getBottom())
                        ,50,50, fillPaint);

                int size = (int) (50*scale);
                int margin = 16;
                int top = itemView.getTop()+(itemView.getBottom()-itemView.getTop())/2-size/2;
                int bot = itemView.getTop()+(itemView.getBottom()-itemView.getTop())/2+size/2;
                int left = itemView.getLeft()+margin;
                int right = itemView.getLeft()+margin+size;

                Objects.requireNonNull(rightDrawable).setBounds(left, top, right, bot);
                rightDrawable.draw(c);
            }
        }

    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        if(viewHolder.getItemViewType() == GeneralItem.TYPE_PAPER && !mAdapter.isSelectMode) {
            // What element has been swiped?
            int position = viewHolder.getAdapterPosition();

            if (direction == ItemTouchHelper.LEFT) {
                mAdapter.swipeLeft(position, viewHolder.itemView);
            } else if (direction == ItemTouchHelper.RIGHT) {
                mAdapter.swipeRight(position, viewHolder.itemView);
            }
        }

    }
}
