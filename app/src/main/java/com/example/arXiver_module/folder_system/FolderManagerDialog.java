package com.example.arXiver_module.folder_system;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.core.content.res.ResourcesCompat;

import com.example.arXiver_module.ArXivPaper;
import com.example.arXiver_module.R;

import java.util.ArrayList;

public class FolderManagerDialog extends AppCompatDialogFragment {

    private FolderManagerListener listener;
    private RadioGroup folderRadioGroup;
    public final ArrayList<ArXivPaper> papers;

    public FolderManagerDialog(ArrayList<ArXivPaper> papers){
        this.papers = papers;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.folder_manager,container,false);

        folderRadioGroup= rootView.findViewById(R.id.folderRadioGroup);

        // select folder
        Button selectButton = rootView.findViewById(R.id.selectFolderButton);
        selectButton.setOnClickListener(view12 -> {
            int radioID = folderRadioGroup.getCheckedRadioButtonId();
            if (radioID!=-1) {
                RadioButton radioButton = rootView.findViewById(radioID);
                if(radioButton!=null) {
                    String folderName = radioButton.getText().toString();
                    Folder folder = new Folder(folderName);
                    listener.addPapersToFolder(folder, papers);
                    Toast.makeText(getContext(), requireContext().getResources().getString(R.string.paper_added_folder) + " " + folderName, Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(getContext(), requireContext().getResources().getString(R.string.no_folder_selected) , Toast.LENGTH_SHORT).show();
                }
            }
            dismiss();
        });

        // delete folder
        Button deleteButton = rootView.findViewById(R.id.deleteFolderButton);
        deleteButton.setOnClickListener(view12 -> {
            int radioID = folderRadioGroup.getCheckedRadioButtonId();
            if (radioID!=-1) {
                RadioButton radioButton = rootView.findViewById(radioID);
                if(radioButton!=null) {
                    String folderName = radioButton.getText().toString();
                    if (folderName.equals(getResources().getString(R.string.saved_papers))) {
                        Toast.makeText(getContext(), requireContext().getResources().getString(R.string.cant_delete), Toast.LENGTH_SHORT).show();
                    } else {
                        Folder folder = new Folder(folderName);
                        listener.deleteFolder(folder);
                        Toast.makeText(getContext(), requireContext().getResources().getString(R.string.folder_deleted) + " " + folderName, Toast.LENGTH_SHORT).show();
                    }
                }
            }
            updateFolderTable(rootView);
            dismiss();
        });

        if(papers!=null){
            deleteButton.setVisibility(View.GONE);
        }else{
            selectButton.setVisibility(View.GONE);
        }

        // cancel button
        Button cancelButton = rootView.findViewById(R.id.cancelFolderButton);
        cancelButton.setOnClickListener(view1 -> dismiss());

        Context context = rootView.getContext();

        // add folder button
        Button addFolderButton = rootView.findViewById(R.id.addFolderButton);
        addFolderButton.setOnClickListener(view13 -> {
            AlertDialog.Builder builderAdd = new AlertDialog.Builder(context);
            builderAdd.setTitle(context.getResources().getString(R.string.name_folder));

            EditText input = new EditText(context);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builderAdd.setView(input);

            builderAdd.setPositiveButton("OK", (dialog, which) -> {
                String folderName = input.getText().toString();
                if(!folderName.isEmpty()) {
                    listener.addFolder(new Folder(folderName));
                    Toast.makeText(context,context.getResources().getString(R.string.added_folder)+" "+folderName,Toast.LENGTH_SHORT).show();
                    updateFolderTable(rootView);
                }
            });
            builderAdd.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

            builderAdd.show();
        });

        updateFolderTable(rootView);

        return rootView;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(requireContext().getResources().getString(R.string.folder_manager_title));
        return dialog;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (FolderManagerListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()+" must implement FolderClassListener");
        }
    }

    public void updateFolderTable(View view){
        Context context = view.getContext();

        folderRadioGroup = view.findViewById(R.id.folderRadioGroup);
        folderRadioGroup.removeAllViews();

        ArrayList<String> folderNames = listener.getAllFolderNames();
        if (folderNames.size()>0){
            for (String folderName : folderNames) {
                RadioButton radioButton = new RadioButton(context);
                radioButton.setText(folderName);
                Drawable drawable;
                if (folderName.equals(context.getResources().getString(R.string.saved_papers))){
                    drawable = ResourcesCompat.getDrawable(context.getResources(),R.drawable.ic_baseline_archive_24_green,context.getTheme());
                }else{
                    drawable = ResourcesCompat.getDrawable(context.getResources(),R.drawable.ic_baseline_folder_24,context.getTheme());
                }
                radioButton.setCompoundDrawablesWithIntrinsicBounds(drawable,null,null,null);
                folderRadioGroup.addView(radioButton);
            }
        }
    }

    @Override
    public void onDismiss(@NonNull final DialogInterface dialog) {
        super.onDismiss(dialog);
        final Activity activity = getActivity();
        if (activity instanceof FolderActivity) {
            ((FolderActivity) activity).onFolderButtonClicked();
        }
    }

    public interface FolderManagerListener {
        ArrayList<String> getAllFolderNames();
        void deleteFolder(Folder folder);
        void addFolder(Folder folder);
        void addPapersToFolder(Folder folder, ArrayList<ArXivPaper> papers);
    }
}