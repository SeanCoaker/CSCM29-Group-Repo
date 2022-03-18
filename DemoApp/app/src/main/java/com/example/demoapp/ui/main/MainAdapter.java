package com.example.demoapp.ui.main;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.demoapp.ChoicesSingleton;
import com.example.demoapp.MainActivity;
import com.example.demoapp.R;

import java.util.ArrayList;

public class MainAdapter extends RecyclerView.Adapter<MainAdapter.ViewHolder> {

    private final ChoicesSingleton choicesSingleton = ChoicesSingleton.getInstance();
    private final ArrayList<String> choices = choicesSingleton.getChoices();
    private final com.example.demoapp.ui.main.MainFragment parentFragment;
    private CardView selectedCard;

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView choiceTextView;

        public ViewHolder(View view) {
            super(view);
            view.setClickable(true);

            choiceTextView = view.findViewById(R.id.choiceTextView);

            ImageButton crossButton = view.findViewById(R.id.crossButton);
            crossButton.setOnClickListener(this);

            CardView listCard = view.findViewById(R.id.listCard);
            listCard.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.crossButton) {
                CardView listCard = (CardView) view.getParent().getParent();
                removeItem(getAdapterPosition(), listCard);
            } else if (view.getId() == R.id.listCard) {
                selectCardItem(view.findViewById(R.id.listCard));
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

    public void removeItem(int pos, CardView card) {
        choicesSingleton.removeChoice(pos);
        notifyItemRemoved(pos);
        if (choicesSingleton.getChoices().size() == 6) {
            parentFragment.updateOperatingMode(MainActivity.OperatingMode.SIXSIDENORMAL);
        } else if (choicesSingleton.getChoices().size() >= 6) {
            parentFragment.updateOperatingMode(MainActivity.OperatingMode.SIXSIDEEXTENDED);
            parentFragment.callRandomChoice();
        } else {
            parentFragment.updateOperatingMode(MainActivity.OperatingMode.INACTIVE);
        }

        if (selectedCard == card) {
            selectedCard = null;
            setParentSelectedIndex();
        }
    }

    public void selectCardItem(CardView card) {

        if (selectedCard != null) {
            resetSelectedCardDisplay();
        }

        if (selectedCard == card) {
            selectedCard = null;
        } else {
            card.findViewById(R.id.crossButton).setBackgroundTintList(
                    card.getContext().getColorStateList(R.color.pistachio));
            card.setCardBackgroundColor(ContextCompat.getColor(card.getContext(), R.color.pistachio));
            selectedCard = card;
        }

        setParentSelectedIndex();
    }

    public void resetSelectedCardDisplay() {
        selectedCard.findViewById(R.id.crossButton).setBackgroundTintList(
                selectedCard.getContext().getColorStateList(R.color.white));
        selectedCard.setCardBackgroundColor(ContextCompat.getColor(
                selectedCard.getContext(), R.color.white));
    }

    public void setParentSelectedIndex() {
        if (selectedCard == null) {
            parentFragment.setCardSelected(false);
            return;
        }

        String choice;
        TextView selectedText = selectedCard.findViewById(R.id.choiceTextView);
        choice = selectedText.getText().toString();

        int selectedIndex = choices.indexOf(choice);
        parentFragment.setSelectedIndex(selectedIndex);
        parentFragment.setCardSelected(true);
    }
}
