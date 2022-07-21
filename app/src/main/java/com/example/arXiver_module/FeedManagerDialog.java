package com.example.arXiver_module;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

public class FeedManagerDialog extends AppCompatDialogFragment {

    private FeedManagerListener listener;
    private String[] allCategories;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.feed_manager,null);

        // categories autocomplete
        this.allCategories = listener.getAllCategories();
        AutoCompleteTextView feedACTV = view.findViewById(R.id.feedAutoCompleteTextView);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, allCategories);
        feedACTV.setAdapter(adapter);

        builder.setView(view);
        builder.setNegativeButton(getResources().getString(R.string.cancel),
                (dialogInterface, i) -> listener.dismiss()
        );

        builder.setPositiveButton(getResources().getString(R.string.add),
                (dialogInterface, i) -> {
                    String category = feedACTV.getText().toString();
                    if(validFeed(category)){
                        Toast.makeText(getActivity(),category + " " + getResources().getString(R.string.added_to_feed_lc), Toast.LENGTH_SHORT).show();
                        listener.addCategory(category);
                    }else{
                        Toast.makeText(getActivity(),getResources().getString(R.string.invalid_feed), Toast.LENGTH_SHORT).show();
                    }
                    listener.dismiss();
                }
        );

        Dialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        return dialog;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            listener = (FeedManagerListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()+" must implement FeedClassListener");
        }
    }

    private boolean validFeed(String category){
        for (String goodCategory : allCategories) {
            if (category.equals(goodCategory)) {
                return true;
            }
        }
        return false;
    }

    public interface FeedManagerListener {
        String[] getAllCategories();
        void dismiss();
        void addCategory(String category);
    }
}
