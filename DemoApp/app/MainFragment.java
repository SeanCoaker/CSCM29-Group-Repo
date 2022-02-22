package com.example.demo.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.demo.ChoicesSingleton;
import com.example.demo.MainActivity;
import com.example.demo.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Objects;


public class MainFragment extends Fragment implements View.OnClickListener {

    private final MainActivity parent;
    private Button addButton;
    private TextInputLayout addChoiceTextLayout;
    private ChoicesSingleton choicesSingleton = ChoicesSingleton.getInstance();
    private ArrayList<String> choices = choicesSingleton.getChoices();
    private RecyclerView.Adapter<MainAdapter.ViewHolder> adapter;

    public MainFragment(MainActivity parent) {
        this.parent = parent;
    }

    public static Fragment newInstance(MainActivity parent) {
        return new MainFragment(parent);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.main_fragment, container, false);
        RecyclerView recyclerView = root.findViewById(R.id.choiceRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MainAdapter(this);
        recyclerView.setAdapter(adapter);
        addButton = root.findViewById(R.id.addButton);
        addChoiceTextLayout = root.findViewById(R.id.choiceTextLayout);

        View textViewAddChoice = root.findViewById(R.id.textViewAddChoice);
        textViewAddChoice.setOnClickListener(this);
        addButton.setOnClickListener(this);

        return root;
    }

    public void onClick(View v) {
        if (v.getId() == R.id.textViewAddChoice) {
            addButton.setVisibility(View.VISIBLE);
            addChoiceTextLayout.setVisibility(View.VISIBLE);
        } else if (v.getId() == R.id.addButton) {
            populateChoicesList();
            adapter.notifyDataSetChanged();
            addButton.setVisibility(View.GONE);
            addChoiceTextLayout.setVisibility(View.GONE);
            TextInputEditText editText = parent.findViewById(R.id.editTextChoices);
            Objects.requireNonNull(editText.getText()).clear();
        }
    }

    public void populateChoicesList() {
        TextInputEditText choicesText = parent.findViewById(R.id.editTextChoices);
        String choices = Objects.requireNonNull(choicesText.getText()).toString();
        String[] choicesList = choices.split(",");

        boolean alreadyExists = false;

        if (!choicesList[0].equals("")) {
            for (String elem : choicesList) {
                if (!this.choices.contains(elem)) {
                    if (elem.trim().length() > 0) {
                        this.choicesSingleton.addChoice(elem);
                    }
                } else {
                    alreadyExists = true;
                }
            }
        }

        if (alreadyExists) {
            Snackbar.make(
                    requireView(),
                    "One or more choices already exist in this list",
                    Snackbar.LENGTH_LONG)
                    .show();
        }
    }
}