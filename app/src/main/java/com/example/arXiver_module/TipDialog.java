package com.example.arXiver_module;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.DialogFragment;

import java.util.Objects;

public class TipDialog extends AppCompatDialogFragment {

    final String tip1;
    final String tip2;
    final Drawable drawable;
    final int size;
    public TipDialog(String tip1, Drawable drawable, int size, String tip2){
        this.tip1 = tip1;
        this.tip2 = tip2;
        this.drawable = drawable;
        this.size = size;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.tip_layout,null);
        builder.setView(view)
                .setPositiveButton(getResources().getString(R.string.ok), (dialogInterface, i) -> {

                });
        TextView tipTextView1 = view.findViewById(R.id.tipTextView1);
        tipTextView1.setText(tip1);

        TextView tipTextView2 = view.findViewById(R.id.tipTextView2);
        tipTextView2.setText(tip2);

        ImageView tipImageView = view.findViewById(R.id.tipImageView1);
        tipImageView.setImageDrawable(drawable);
        tipImageView.requestLayout();
        tipImageView.getLayoutParams().height = size;
        tipImageView.getLayoutParams().width = size;


        return builder.create();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Objects.requireNonNull(getDialog()).requestWindowFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.gray)));
        setStyle(DialogFragment.STYLE_NO_FRAME, android.R.style.Theme);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override public void onStart() {
        super.onStart();
        Window window = Objects.requireNonNull(getDialog()).getWindow();
        WindowManager.LayoutParams windowParams = window.getAttributes();
        windowParams.dimAmount = 0.20f;
        windowParams.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(windowParams);
    }
}
