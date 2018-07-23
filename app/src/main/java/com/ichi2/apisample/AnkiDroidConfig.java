package com.ichi2.plaintextimporter;

import java.util.ArrayList;
import java.util.HashMap;


public final class AnkiDroidConfig {
    // Name of deck which will be created in AnkiDroid
    public static final String DECK_NAME = "ImporterSample";
    // Name of model which will be created in AnkiDroid
    public static final String MODEL_NAME = "com.ichi2.plaintextimporter";
    // Optional space separated list of tags to add to every note
    public static final String TAGS = "AnkiImporter2";
    // List of field names that will be used in AnkiDroid model
    public static final String[] FIELDS = {"Front","Back"};

    // Define two keys which will be used when using legacy ACTION_SEND intent
    public static final String FRONT_SIDE_KEY = FIELDS[0];
    public static final String BACK_SIDE_KEY = FIELDS[1];

    /**
     * Generate the ArrayList<HashMap> example data which will be sent to AnkiDroid
     * @return
     */
    public static ArrayList<HashMap<String, String>> getExampleData() {
        final String[] EXAMPLE_FRONTS = {"front1","front2"};
        final String[] EXAMPLE_BACKS = {"back1", "back2"};

        ArrayList<HashMap<String, String>> data = new ArrayList<>();
        for (int idx = 0; idx < EXAMPLE_FRONTS.length; idx++) {
            HashMap<String, String> hm = new HashMap<>();
            hm.put(FIELDS[0], EXAMPLE_FRONTS[idx]);
            hm.put(FIELDS[1], EXAMPLE_BACKS[idx]);
            data.add(hm);
        }
        return data;
    }
}
