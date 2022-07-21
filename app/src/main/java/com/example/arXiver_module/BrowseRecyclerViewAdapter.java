package com.example.arXiver_module;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arXiver_module.items.DateItem;
import com.example.arXiver_module.items.GeneralItem;
import com.example.arXiver_module.items.HeaderItem;
import com.example.arXiver_module.items.ResultsItem;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public abstract class BrowseRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    final Context context;
    ArrayList<GeneralItem> consolidatedItems = new ArrayList<>();
    final ArXivScanner arXivScanner;
    boolean isSelectMode = false;
    ArrayList<ArXivPaper> selectedPapers = new ArrayList<>();
    final ActivityAdapterListener listener;

    public static final String DOWNLOAD_PREFS = "downloadPrefs";
    public static final String DELETED_PREFS = "deletedPrefs";

    public BrowseRecyclerViewAdapter(Context context, ActivityAdapterListener listener){
        this.listener = listener;
        this.context = context;
        arXivScanner = new ArXivScanner();
    }

    public abstract ArrayList<GeneralItem> getConsolidated();

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == GeneralItem.TYPE_PAPER) {
            View viewPaper = inflater.inflate(R.layout.paper_row_layout, parent, false);
            return new PaperRecyclerViewHolder(viewPaper, listener);
        } else if(viewType == GeneralItem.TYPE_HEADER){
            View viewHeader = inflater.inflate(R.layout.order_row_layout, parent, false);
            return new HeaderRecyclerViewHolder(viewHeader);
        } else if(viewType == GeneralItem.TYPE_DATE){
            View viewHeader = inflater.inflate(R.layout.order_row_layout, parent, false);
            return new DateRecyclerViewHolder(viewHeader);
        } else{
            View viewHeader = inflater.inflate(R.layout.order_row_layout, parent, false);
            return new ResultsRecyclerViewHolder(viewHeader);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {

        if (viewHolder.getItemViewType()==GeneralItem.TYPE_PAPER) {
            BrowseRecyclerViewAdapter.PaperRecyclerViewHolder holder = (BrowseRecyclerViewAdapter.PaperRecyclerViewHolder) viewHolder;
            ArXivPaper paper = (ArXivPaper) consolidatedItems.get(position);

            // Title
            holder.titleTextView.setText(paper.title);

            // Authors
            holder.authorTextView.setText(
                    ArXivScanner.join(", ", paper.authors)
            );

            // Published and updated Date
            holder.publishedTextView.setText(
                    String.format("%s: %s", context.getResources().getString(R.string.published), arXivScanner.simpleDate(paper.publishedDate))
            );
            holder.updatedTextView.setText(
                    String.format("%s: %s", context.getResources().getString(R.string.updated), arXivScanner.simpleDate(paper.updatedDate))
            );

            // Abstract
            holder.abstractTextView.setText(paper.abs);

            // id and category
            holder.idTextView.setText( String.format("%s [%s]",paper.id, ArXivScanner.join(", ", paper.categories)));

            // Set the visibility based on state
            holder.extrasLinearLayout.setVisibility(paper.isExpanded ? View.VISIBLE : View.GONE);

            // saveButton
            View.OnClickListener saveListener = view -> {
                //saveItem(position, view);
                ((BrowseActivity) context).showFolderManager(new ArrayList<>(Collections.singletonList(paper)));
                resetSelection();
            };
            holder.saveButton.setOnClickListener(saveListener);

            // deleteButton
            View.OnClickListener deleteListener = view -> {
                deleteItem(position, view);
                resetSelection();
            };
            holder.deleteButton.setOnClickListener(deleteListener);

            // readButton
            View.OnClickListener readListener = view -> {
                Intent pdfIntent = new Intent(Intent.ACTION_VIEW);
                pdfIntent.setDataAndType(Uri.parse(paper.pdfURL), "application/pdf");
                pdfIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try {
                    context.startActivity(pdfIntent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(context, context.getResources().getString(R.string.no_app_to_view_pdf), Toast.LENGTH_LONG).show();
                }
                resetSelection();
            };
            holder.readButton.setOnClickListener(readListener);

            // offline switch
            holder.offlineSwitch.setVisibility(View.GONE);

            if (paper.isSelected) {
                holder.paperConstraintLayout.setBackgroundResource(R.color.selected_color);
            } else {
                holder.paperConstraintLayout.setBackgroundResource(R.color.gray);
            }

        }else if (viewHolder.getItemViewType()==GeneralItem.TYPE_HEADER) {
            BrowseRecyclerViewAdapter.HeaderRecyclerViewHolder headerHolder = (BrowseRecyclerViewAdapter.HeaderRecyclerViewHolder) viewHolder;
            HeaderItem headerItem = (HeaderItem) consolidatedItems.get(position);
            headerHolder.headerTextView.setText(headerItem.header);
        }
        else if (viewHolder.getItemViewType()==GeneralItem.TYPE_DATE) {
            BrowseRecyclerViewAdapter.DateRecyclerViewHolder dateHolder = (BrowseRecyclerViewAdapter.DateRecyclerViewHolder) viewHolder;
            DateItem dateItem = (DateItem) consolidatedItems.get(position);

            // Today
            Date today = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String todayString = simpleDateFormat.format(today);

            if(dateItem.date.equals(todayString)){
                dateHolder.dateTextView.setText(context.getResources().getString(R.string.today));
            }else {
                dateHolder.dateTextView.setText(dateItem.date);
            }
        }else if (viewHolder.getItemViewType()==GeneralItem.TYPE_RESULTS) {
            BrowseRecyclerViewAdapter.ResultsRecyclerViewHolder resultsHolder = (BrowseRecyclerViewAdapter.ResultsRecyclerViewHolder) viewHolder;
            ResultsItem resultsItem = (ResultsItem) consolidatedItems.get(position);
            resultsHolder.resultsTextView.setText(resultsItem.header);
        }
    }



    @Override
    public int getItemViewType(int position){
        return consolidatedItems.get(position).getType();
    }

    @Override
    public int getItemCount() {
        return consolidatedItems != null ? consolidatedItems.size() : 0;
    }

    public void deleteItem(int position, View v){

        ArXivPaper removedPaper = (ArXivPaper) consolidatedItems.get(position);

        // Saving the fact that paper has been deleted
        SharedPreferences sharedPreferences = context.getSharedPreferences(DELETED_PREFS,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(removedPaper.id,"R.I.P.");
        editor.apply();

        notifyItemChanged(position);

        consolidatedItems.remove(position);

        notifyItemRemoved(position);
        notifyItemRangeChanged(position,consolidatedItems.size());

        View.OnClickListener undoListener = view -> {
            editor.remove(removedPaper.id);
            editor.apply();

            removedPaper.setExpanded(false);
            removedPaper.setSelected(false);
            consolidatedItems.add(position,removedPaper);
            notifyItemInserted(position);
            notifyItemRangeChanged(position,consolidatedItems.size());
        };

        Snackbar.make(v, context.getResources().getString(R.string.snack_bar_deleted), Snackbar.LENGTH_LONG)
                .setAction(context.getResources().getString(R.string.snack_bar_undo), undoListener)
                .show();
    }

    public void saveItem(int position, View v){

        ArXivPaper removedPaper = (ArXivPaper) consolidatedItems.get(position);

        Date dateObj = Calendar.getInstance().getTime();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateSaved = simpleDateFormat.format(dateObj);
        removedPaper.setDateSaved(dateSaved);

        // Saving the paper to saved
        SharedPreferences sharedPreferencesSaved = context.getSharedPreferences(ParentActivity.SAVED_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editorSaved = sharedPreferencesSaved.edit();
        editorSaved.putString(removedPaper.id,arXivScanner.compress(removedPaper));
        editorSaved.apply();
        // Saving the paper to deleted
        SharedPreferences sharedPreferencesDeleted = context.getSharedPreferences(DELETED_PREFS,Context.MODE_PRIVATE);
        SharedPreferences.Editor editorDeleted = sharedPreferencesDeleted.edit();
        editorDeleted.putString(removedPaper.id,"R.I.P.");
        editorDeleted.apply();
        // Removing the paper from consolidated
        consolidatedItems.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position,consolidatedItems.size());

        // Undo Snack-bar
        View.OnClickListener undoListener = view -> {
            // undo saving in saved memory
            editorSaved.remove(removedPaper.id);
            editorSaved.apply();
            // undo saving in deleted memory
            editorDeleted.remove(removedPaper.id);
            editorDeleted.apply();
            // add back to consolidated
            removedPaper.setExpanded(false);
            removedPaper.setSelected(false);
            consolidatedItems.add(position,removedPaper);
            notifyItemInserted(position);
            notifyItemRangeChanged(position,consolidatedItems.size());
        };
        Snackbar.make(v, context.getResources().getString(R.string.snack_bar_saved), Snackbar.LENGTH_LONG)
                .setAction(context.getResources().getString(R.string.snack_bar_undo), undoListener)
                .show();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void resetSelection(){
        isSelectMode = false;
        selectedPapers = new ArrayList<>();
        listener.update(isSelectMode,selectedPapers);
        for(GeneralItem generalItem:consolidatedItems){
            if(generalItem instanceof ArXivPaper){
                ArXivPaper paper = (ArXivPaper) generalItem;
                paper.setSelected(false);
            }
        }
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void selectAllItems(){
        selectedPapers = new ArrayList<>();

        for(GeneralItem generalItem:consolidatedItems){
            if(generalItem instanceof ArXivPaper){
                ArXivPaper paper = (ArXivPaper) generalItem;
                paper.setSelected(true);
                selectedPapers.add(paper);
            }
        }

        listener.update(true,selectedPapers);

        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void deleteItems(ArrayList<ArXivPaper> toDeletePapers, View v){
        ArrayList<ArXivPaper> undoPapers = new ArrayList<>();
        for(int i=0; i< toDeletePapers.size();i++){
            ArXivPaper selectedPaper = toDeletePapers.get(i);
            undoPapers.add(selectedPaper);
            deletePaperPrefs(selectedPaper);
            consolidatedItems.remove(selectedPaper);
        }
        listener.update(false,new ArrayList<>());
        selectedPapers = new ArrayList<>();
        isSelectMode = false;
        notifyDataSetChanged();

        @SuppressLint("NotifyDataSetChanged") View.OnClickListener undoListener = view -> {
            for(int i=0; i<undoPapers.size();i++) {
                ArXivPaper removedPaper = undoPapers.get(i);
                removedPaper.setExpanded(false);
                removedPaper.setSelected(false);
                unDeletePaperPrefs(removedPaper);
            }
            // refresh memory
            consolidatedItems = getConsolidated();
            notifyDataSetChanged();
        };

        Snackbar.make(v, context.getResources().getString(R.string.snack_bar_deleted), Snackbar.LENGTH_LONG)
                .setAction(context.getResources().getString(R.string.snack_bar_undo), undoListener)
                .show();
    }

    public void deletePaperPrefs(ArXivPaper paper){

    }

    public void unDeletePaperPrefs(ArXivPaper paper){

    }

    public abstract void swipeLeft(int position, View itemView);
    public abstract void swipeRight(int position, View itemView);


    public class PaperRecyclerViewHolder extends RecyclerView.ViewHolder {

        final TextView titleTextView;
        final TextView authorTextView;
        final TextView abstractTextView;
        final TextView idTextView;
        final TextView updatedTextView;
        final TextView publishedTextView;
        final LinearLayout extrasLinearLayout;
        final LinearLayout datesLinearLayout;
        final ConstraintLayout paperConstraintLayout;
        final SwitchCompat offlineSwitch;
        final Button saveButton;
        final Button deleteButton;
        final Button readButton;

        public PaperRecyclerViewHolder(@NonNull View itemView, ActivityAdapterListener listener){
            super(itemView);

            titleTextView = itemView.findViewById(R.id.titleTextView);
            authorTextView = itemView.findViewById(R.id.authorTextView);
            extrasLinearLayout = itemView.findViewById(R.id.extrasLinearLayout);

            publishedTextView = itemView.findViewById(R.id.publishedTextView);
            updatedTextView = itemView.findViewById(R.id.updatedTextView);

            abstractTextView = itemView.findViewById(R.id.abstractTextView);
            idTextView = itemView.findViewById(R.id.idTextView);

            paperConstraintLayout = itemView.findViewById(R.id.paperConstraintLayout);

            saveButton = itemView.findViewById(R.id.saveButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            readButton = itemView.findViewById(R.id.readButton);

            datesLinearLayout = itemView.findViewById(R.id.datesLinearLayout);

            offlineSwitch = itemView.findViewById(R.id.offlineSwitch);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                ArXivPaper paper = (ArXivPaper) consolidatedItems.get(pos);
                if(isSelectMode){
                    if(selectedPapers.contains(paper)){
                        paperConstraintLayout.setBackgroundResource(R.color.gray);
                        paper.setSelected(false);
                        selectedPapers.remove(paper);
                    }else{
                        paperConstraintLayout.setBackgroundResource(R.color.selected_color);
                        paper.setSelected(true);
                        selectedPapers.add(paper);
                    }
                    if(selectedPapers.size()==0){
                        isSelectMode = false;
                    }
                    listener.update(isSelectMode,selectedPapers);

                }else{
                    // Get the current state of the item
                    boolean expanded = paper.isExpanded;
                    // Change the state
                    paper.setExpanded(!expanded);
                    extrasLinearLayout.setVisibility(paper.isExpanded ? View.VISIBLE : View.GONE);
                }
            });

            itemView.setOnLongClickListener(v -> {
                isSelectMode = true;
                int pos = getAdapterPosition();
                ArXivPaper paper = (ArXivPaper) consolidatedItems.get(pos);
                // do we have this item selected already?
                if(!selectedPapers.contains(paper)){
                    paperConstraintLayout.setBackgroundResource(R.color.selected_color);
                    paper.setSelected(true);
                    selectedPapers.add(paper);
                }
                listener.update(isSelectMode,selectedPapers);
                return true;
            });
        }

    }

    public static class DateRecyclerViewHolder extends RecyclerView.ViewHolder{

        final TextView dateTextView;

        public DateRecyclerViewHolder(@NonNull View itemView){
            super(itemView);

            dateTextView = itemView.findViewById(R.id.orderTextView);

        }

    }

    public static class HeaderRecyclerViewHolder extends RecyclerView.ViewHolder{

        final TextView headerTextView;

        public HeaderRecyclerViewHolder(@NonNull View itemView){
            super(itemView);

            headerTextView = itemView.findViewById(R.id.orderTextView);
        }

    }

    public static class ResultsRecyclerViewHolder extends RecyclerView.ViewHolder{

        final TextView resultsTextView;

        public ResultsRecyclerViewHolder(@NonNull View itemView){
            super(itemView);

            resultsTextView = itemView.findViewById(R.id.orderTextView);
        }

    }


}