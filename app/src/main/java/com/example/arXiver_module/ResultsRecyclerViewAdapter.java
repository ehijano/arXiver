package com.example.arXiver_module;


import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arXiver_module.items.GeneralItem;
import com.example.arXiver_module.items.ResultsItem;
import com.google.android.material.snackbar.Snackbar;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

public class ResultsRecyclerViewAdapter extends BrowseRecyclerViewAdapter{

    int page = 0;
    int numberFound = 0;
    public final int MAX_RESULTS = 10;
    ArrayList<ArXivPaper> papers = new ArrayList<>();

    public ResultsRecyclerViewAdapter(Context context, ActivityAdapterListener listener) throws IOException, ParserConfigurationException, SAXException {
        super(context,listener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        super.onBindViewHolder(viewHolder,position);
        if(viewHolder.getItemViewType()== GeneralItem.TYPE_PAPER) {
            PaperRecyclerViewHolder holder = (PaperRecyclerViewHolder) viewHolder;
            // deleteButton -> hide Button
            View.OnClickListener deleteListener = view -> {
                hideItem(position,view);
                resetSelection();
            };
            holder.deleteButton.setText(context.getResources().getString(R.string.paper_hide));
            holder.deleteButton.setBackground(ResourcesCompat.getDrawable(context.getResources(),R.drawable.border_button_hide,null));
            holder.deleteButton.setOnClickListener(deleteListener);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void refresh(ArrayList<ArXivPaper> papers){
        isSelectMode = false;
        selectedPapers = new ArrayList<>();

        this.papers = papers;
        consolidatedItems = getConsolidated();
        numberFound = papers.size();
        notifyDataSetChanged();
    }

    @Override
    public ArrayList<GeneralItem> getConsolidated() {

        int min = page * MAX_RESULTS;
        int max = page * MAX_RESULTS + Math.min(papers.size(),MAX_RESULTS);

        ResultsItem resultsItem = new ResultsItem(
                String.format("%s (%s-%s)", context.getResources().getString(R.string.results), Math.min(min + 1, max), max)
        );
        ArrayList<GeneralItem> result = new ArrayList<>();
        result.add(resultsItem);
        result.addAll(papers);

        return result;
    }

    public void updatePage(int page){
        this.page = page;
    }

    public void hideItem(int position, View v){

        ArXivPaper removedPaper = (ArXivPaper) consolidatedItems.get(position);

        notifyItemChanged(position);

        consolidatedItems.remove(position);

        notifyItemRemoved(position);
        notifyItemRangeChanged(position,consolidatedItems.size());

        View.OnClickListener undoListener = view -> {
            removedPaper.setExpanded(false);
            removedPaper.setSelected(false);
            consolidatedItems.add(position,removedPaper);
            notifyItemInserted(position);
            notifyItemRangeChanged(position,consolidatedItems.size());
        };

        Snackbar.make(v, context.getResources().getString(R.string.snack_bar_hidden), Snackbar.LENGTH_LONG)
                .setAction(context.getResources().getString(R.string.snack_bar_undo), undoListener)
                .show();
    }

    @Override
    public void swipeLeft(int position, View v){
        hideItem(position, v);
    }

    @Override
    public void swipeRight(int position, View v){
        saveItem(position, v);
    }


}
