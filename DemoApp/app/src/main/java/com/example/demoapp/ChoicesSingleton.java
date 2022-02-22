package com.example.demoapp;

import java.util.ArrayList;

public class ChoicesSingleton {

    private static ChoicesSingleton INSTANCE;
    private ArrayList<String> choices;

    private ChoicesSingleton() {
        choices =  new ArrayList<>();
    }

    public static ChoicesSingleton getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ChoicesSingleton();
        }

        return INSTANCE;
    }

    public ArrayList<String> getChoices() {
        return choices;
    }

    public void setChoices(ArrayList<String> choices) {
        this.choices = choices;
    }

    public boolean addChoice(String choice) {
        return this.choices.add(choice);
    }

    public String removeChoice(int position) {
        return this.choices.remove(position);
    }
}
