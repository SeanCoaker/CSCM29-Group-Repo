package com.example.demoapp.ui.main;

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

import com.example.demoapp.ChoicesSingleton;
import com.example.demoapp.MainActivity;
import com.example.demoapp.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Objects;


public class MainFragment extends Fragment implements View.OnClickListener {

    private final MainActivity parent;
    private Button addButton;
    private TextInputLayout addChoiceTextLayout;
    private final ChoicesSingleton choicesSingleton = ChoicesSingleton.getInstance();
    private final ArrayList<String> choices = choicesSingleton.getChoices();
    private RecyclerView.Adapter<MainAdapter.ViewHolder> adapter;
    private int selectedIndex;
    private boolean isCardSelected = false;

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

                    String trimmedElem = elem.trim();

                    if (trimmedElem.length() > 0) {
                        this.choicesSingleton.addChoice(trimmedElem);
                        if (choicesSingleton.getChoices().size() == 6) {
                            updateOperatingMode(MainActivity.OperatingMode.SIXSIDENORMAL);
                        } else if (choicesSingleton.getChoices().size() >= 6) {
                            updateOperatingMode(MainActivity.OperatingMode.SIXSIDEEXTENDED);
                            callRandomChoice();
                        } else {
                            updateOperatingMode(MainActivity.OperatingMode.INACTIVE);
                        }
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

    public void updateOperatingMode(MainActivity.OperatingMode op) {
        parent.setOperatingMode(op);
    }

    public void callRandomChoice() {
        parent.setRandomChoice();
    }

    public void clearRecycler() {
        int len = choices.size();
        choices.clear();
        adapter.notifyItemRangeRemoved(0, len);
        updateOperatingMode(MainActivity.OperatingMode.INACTIVE);
        setCardSelected(false);
    }

    public void setSelectedIndex(int index) {
        this.selectedIndex = index;
    }

    public int getSelectedIndex() {
        return this.selectedIndex;
    }

    public boolean isCardSelected() {
        return this.isCardSelected;
    }

    public void setCardSelected(boolean cardSelected) {
        this.isCardSelected = cardSelected;
    }
}