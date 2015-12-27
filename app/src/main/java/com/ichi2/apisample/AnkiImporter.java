package com.ichi2.plaintextimporter;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionProvider;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.ichi2.anki.api.AddContentApi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;


public class AnkiImporter extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    public static final String LOG_TAG = "AnkiDroidApiSample";
    private static final int AD_PERM_REQUEST = 0;

    private ListView mListView;
    private ArrayList<HashMap<String, String>> mListData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Create the example data
        mListData = AnkiDroidConfig.getExampleData();
        // Setup the ListView containing the example data
        mListView = (ListView) findViewById(R.id.main_list);
        mListView.setAdapter(new SimpleAdapter(this, mListData, R.layout.word_layout,
                Arrays.copyOfRange(AnkiDroidConfig.FIELDS, 0, 3),
                new int[]{R.id.word_item, R.id.word_item_reading, R.id.word_item_translation}));
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        // When an item is long-pressed the ListSelectListener will make a Contextual Action Bar with Share icon
        mListView.setMultiChoiceModeListener(new ListSelectListener());
    }


    public void onRequestPermissionsResult (int requestCode, @NonNull String[] permissions,
                                            @NonNull int[] grantResults) {
        if (requestCode==AD_PERM_REQUEST && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            addCardsToAnkiDroid(getSelectedData());
        } else {
            Toast.makeText(AnkiImporter.this, R.string.permission_denied, Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Inner class that handles the contextual action bar that appears when an item is long-pressed in the ListView
     */
    class ListSelectListener implements AbsListView.MultiChoiceModeListener {

        @Override
        public void onItemCheckedStateChanged(android.view.ActionMode mode, int position, long id, boolean checked) {
            // Set the subtitle on the action bar to show how many items are selected
            int numItemsChecked = mListView.getCheckedItemCount();
            String subtitle = getResources().getString(R.string.n_items_selected, numItemsChecked);
            mode.setSubtitle(subtitle);
        }

        @Override
        public boolean onCreateActionMode(android.view.ActionMode mode, Menu menu) {
            // Inflate the menu resource while holds the contextual action bar actions
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.action_mode_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(android.view.ActionMode mode, Menu menu) {
            // Don't need to do anything here
            return false;
        }

        @Override
        public boolean onActionItemClicked(android.view.ActionMode mode, MenuItem item) {
            // This is called when the contextual action bar buttons are pressed
            switch (item.getItemId()) {
                case R.id.share_data_button:
                    String reqPerm = AddContentApi.checkRequiredPermission(AnkiImporter.this);
                    if (reqPerm != null && ContextCompat.checkSelfPermission(AnkiImporter.this, reqPerm)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(AnkiImporter.this, new String[]{reqPerm}, AD_PERM_REQUEST);
                        return true;
                    }
                    // Add all data using AnkiDroid provider
                    addCardsToAnkiDroid(getSelectedData());
                   
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(android.view.ActionMode mode) {
            // Don't need to do anything here
        }
    }

    ArrayList<HashMap<String, String>> getSelectedData() {
        // Extract the selected data
        SparseBooleanArray checked = mListView.getCheckedItemPositions();
        ArrayList<HashMap<String, String>> selectedData = new ArrayList<>();
        for (int i=0;i<checked.size();i++) {
            if (checked.valueAt(i)) {
                selectedData.add(mListData.get(checked.keyAt(i)));
            }
        }
        return selectedData;
    }

    /**
     * Use the instant-add API to add flashcards directly to AnkiDroid
     * @param data List of cards to be added. Each card has a HashMap of field name / field value pairs.
     */
    private void addCardsToAnkiDroid(final ArrayList<HashMap<String, String>> data) {
        // Get api instance
        final AddContentApi api = new AddContentApi(AnkiImporter.this);
        // Look for our deck, add a new one if it doesn't exist
        Long did = api.findDeckIdByName(AnkiDroidConfig.DECK_NAME);
        if (did == null) {
            did = api.addNewDeck(AnkiDroidConfig.DECK_NAME);
        }
        // Look for our model, add a new one if it doesn't exist
        Long mid = api.findModelIdByName(AnkiDroidConfig.MODEL_NAME, AnkiDroidConfig.FIELDS.length);
        if (mid == null) {
            mid = api.addNewBasicModel(AnkiDroidConfig.MODEL_NAME);
        }
        // Double-check that everything was added correctly
        String[] fieldNames = api.getFieldList(mid);
        if (mid == null || did == null || fieldNames == null) {
            Toast.makeText(AnkiImporter.this, R.string.card_add_fail, Toast.LENGTH_LONG).show();
            return;
        }
        // Add cards
        int added = 0;
        for (HashMap<String, String> hm: data) {
            // Build a field map accounting for the fact that the user could have changed the fields in the model
            String[] flds = new String[fieldNames.length];
            for (int i = 0; i < flds.length; i++) {
                // Fill up the fields one-by-one up until either all fields are filled or we run out of fields to send
                if (i < AnkiDroidConfig.FIELDS.length) {
                    flds[i] = hm.get(AnkiDroidConfig.FIELDS[i]);
                }
            }
            // Add a new note using the current field map
            try {
                // Only add item if there aren't any duplicates
                if (!api.checkForDuplicates(mid, did, flds)) {
                    Uri noteUri = api.addNewNote(mid, did, flds, AnkiDroidConfig.TAGS);
                    if (noteUri != null) {
                        added++;
                    }
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "Exception adding cards to AnkiDroid", e);
                Toast.makeText(AnkiImporter.this, R.string.card_add_fail, Toast.LENGTH_LONG).show();
                return;
            }
        }
        Toast.makeText(AnkiImporter.this, getResources().getString(R.string.n_items_added, added), Toast.LENGTH_LONG).show();
    }
}
