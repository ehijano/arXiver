package com.example.arXiver_module;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arXiver_module.items.GeneralItem;
import com.example.arXiver_module.items.HeaderItem;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

public class DayRecyclerViewAdapter extends BrowseRecyclerViewAdapter{

    final Context context;
    final String category;
    ArrayList<ArXivPaper> papers;

    @RequiresApi(api = Build.VERSION_CODES.N)
    public DayRecyclerViewAdapter(Context context,ActivityAdapterListener listener, String category) throws IOException, ParserConfigurationException, SAXException {
        super(context,listener);
        this.context = context;
        this.category = category;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        super.onBindViewHolder(viewHolder, position);
        if (viewHolder.getItemViewType()== GeneralItem.TYPE_PAPER) {
            BrowseRecyclerViewAdapter.PaperRecyclerViewHolder holder = (BrowseRecyclerViewAdapter.PaperRecyclerViewHolder) viewHolder;
            // Don't show dates (today's papers)
            holder.datesLinearLayout.setVisibility(View.GONE);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void refresh(ArrayList<ArXivPaper> papers){
        this.papers = papers;
        consolidatedItems = getConsolidated();
        notifyDataSetChanged();
    }

    @Override
    public ArrayList<GeneralItem> getConsolidated(){
        ArrayList<GeneralItem> consolidatedPapers = new ArrayList<>();

        SharedPreferences sharedPreferencesDeleted = context.getSharedPreferences(DELETED_PREFS,Context.MODE_PRIVATE);

        ArrayList<ArXivPaper> newPapers = new ArrayList<>();
        ArrayList<ArXivPaper> crossListPapers = new ArrayList<>();
        ArrayList<ArXivPaper> replacedPapers = new ArrayList<>();

        for (ArXivPaper paper: papers){
            if (paper.isNew && paper.categories[0].equals(category))  {
                if (!sharedPreferencesDeleted.contains(paper.id)){ newPapers.add(paper); }
            }else if (paper.isNew) {
                if (!sharedPreferencesDeleted.contains(paper.id)){ crossListPapers.add(paper); }
            }else {
                if (!sharedPreferencesDeleted.contains(paper.id)){ replacedPapers.add(paper); }
            }
        }

        consolidatedPapers.add(new HeaderItem(context.getResources().getString(R.string.new_header)));
        consolidatedPapers.addAll(newPapers);
        consolidatedPapers.add(new HeaderItem(context.getResources().getString(R.string.cross_listed_header)));
        consolidatedPapers.addAll(crossListPapers);
        consolidatedPapers.add(new HeaderItem(context.getResources().getString(R.string.replaced_header)));
        consolidatedPapers.addAll(replacedPapers);

        return consolidatedPapers;
    }

    @Override
    public void deletePaperPrefs(ArXivPaper paper){
        // Saving the fact that paper has been deleted
        SharedPreferences sharedPreferences = context.getSharedPreferences(DELETED_PREFS,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(paper.id,"R.I.P.");
        editor.apply();
    }

    @Override
    public void unDeletePaperPrefs(ArXivPaper paper){
        // remove from deleted prefs
        SharedPreferences sharedPreferences = context.getSharedPreferences(DELETED_PREFS,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(paper.id);
        editor.apply();
    }

    @Override
    public void swipeLeft(int position, View v){
        deleteItem(position, v);
    }

    @Override
    public void swipeRight(int position, View v){
        saveItem(position, v);
    }

}
