package com.example.demoapp.ui.main;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.demoapp.ChoicesSingleton;
import com.example.demoapp.MainActivity;
import com.example.demoapp.R;

import java.util.ArrayList;

public class MainAdapter extends RecyclerView.Adapter<MainAdapter.ViewHolder> {

    private ChoicesSingleton choicesSingleton = ChoicesSingleton.getInstance();
    private final ArrayList<String> choices = choicesSingleton.getChoices();
    private final com.example.demoapp.ui.main.MainFragment parentFragment;

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView choiceTextView;

        public ViewHolder(View view) {
            super(view);
            view.setClickable(true);

            choiceTextView = view.findViewById(R.id.choiceTextView);

            ImageButton crossButton = view.findViewById(R.id.crossButton);
            crossButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.crossButton) {
                removeItem(getAdapterPosition());
            }
        }
    }


    public MainAdapter(com.example.demoapp.ui.main.MainFragment parentFragment) {
        this.parentFragment = parentFragment;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.list_item,
                parent,
                false
        );

        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String choice = choices.get(position);
        holder.choiceTextView.setText(choice);
    }

    @Override
    public int getItemCount() {
        return choices.size();
    }

    public void removeItem(int pos) {
        choicesSingleton.removeChoice(pos);
        notifyItemRemoved(pos);
        if (choicesSingleton.getChoices().size() == 6) {
            parentFragment.updateOperatingMode(MainActivity.OperatingMode.SIXSIDENORMAL);
        } else if (choicesSingleton.getChoices().size() >= 6) {
            parentFragment.updateOperatingMode(MainActivity.OperatingMode.SIXSIDEEXTENDED);
        } else {
            parentFragment.updateOperatingMode(MainActivity.OperatingMode.INACTIVE);
        }
    }
}
