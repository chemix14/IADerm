package com.example.iaderm;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

public class TriggersFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_triggers, container, false);

        MaterialButton btnSaveTrigger = view.findViewById(R.id.btnSaveTrigger);
        btnSaveTrigger.setOnClickListener(v -> {
            UiFeedback.shortMessage(getContext(), R.string.trigger_saved);
        });

        return view;
    }
}
