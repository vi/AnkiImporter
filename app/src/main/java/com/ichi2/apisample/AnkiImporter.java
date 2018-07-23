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
import android.app.AlertDialog;

import com.ichi2.anki.api.AddContentApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.io.File;


public class AnkiImporter extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    public static final String LOG_TAG = "AnkiDroidApiSample";
    private static final int AD_PERM_REQUEST = 0;
    
    private static final String[] FIELDS = {"Filename"};
    private File importDirectory;

    private ListView mListView;
    private ArrayList<HashMap<String, String>> mListData;

    private class DeckInformation {
        public String name;
        ArrayList<HashMap<String, String>> content;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        importDirectory = new File(new File(android.os.Environment.getExternalStorageDirectory(), "AnkiDroid"),"TextImport");
        
        setContentView(R.layout.activity_main);
        // Create the example data
        mListData = getFilenames();
        
        if (mListData.size()==0) {
            importDirectory.mkdirs();
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getResources().getString(R.string.place_files, importDirectory.getAbsolutePath()));
            builder.create().show();
        }
        
        // Setup the ListView containing the example data
        mListView = (ListView) findViewById(R.id.main_list);
        mListView.setAdapter(new SimpleAdapter(this, mListData, R.layout.word_layout,
                FIELDS,
                new int[]{R.id.filename_item}));
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        // When an item is long-pressed the ListSelectListener will make a Contextual Action Bar with Share icon
        mListView.setMultiChoiceModeListener(new ListSelectListener());
    }

    private ArrayList<HashMap<String, String>> getFilenames() {
        ArrayList<HashMap<String, String>> output = new ArrayList<>();
        
        if (importDirectory == null) return output;
        
        File[] files = importDirectory.listFiles();
        
        if (files == null) return output;
        
        for (File i : files) {
            HashMap<String, String> hm = new HashMap<>();
            hm.put(FIELDS[0], i.getName());
            output.add(hm);
        }
        
        return output;
    }

    public void onRequestPermissionsResult (int requestCode, @NonNull String[] permissions,
                                            @NonNull int[] grantResults) {
        if (requestCode==AD_PERM_REQUEST && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            addOrUpdateCardsToAnkiDroid(gatherData());
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
                    // Add all data using AnkiDroid provider
                    addOrUpdateCardsToAnkiDroid(gatherData());
                   
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

    String filterString(String in) {
        return in.trim().replaceAll("\\&","&amp;").replaceAll("\\<", "&lt;");
    }

    DeckInformation readFile(File file) {
        
        String[] basenameExt = file.getName().split("\\.(?=[^\\.]+$)");
        String basename = basenameExt[0];
        String deckName = basename.replaceAll("\\.","::");
        
        ArrayList<HashMap<String, String>> data = new ArrayList<>();
        
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] split = line.split(";");
                if (split.length>1) {
                    HashMap<String, String> hm = new HashMap<>();
                    hm.put(AnkiDroidConfig.FIELDS[0], filterString(split[0]));
                    hm.put(AnkiDroidConfig.FIELDS[1], filterString(split[1]));
                    data.add(hm);
                }
            }
        } catch (Exception e) { throw new RuntimeException(e); }
        
        DeckInformation info = new DeckInformation();  
        info.name = deckName;
        info.content = data;
        return info;
    }

    ArrayList<DeckInformation> gatherData() {
        ArrayList<DeckInformation> output = new ArrayList<>();
        
        // Extract the selected data
        SparseBooleanArray checked = mListView.getCheckedItemPositions();
        ArrayList<HashMap<String, String>> selectedData = new ArrayList<>();
        for (int i=0;i<checked.size();i++) {
            if (checked.valueAt(i)) {
                String fileName = mListData.get(checked.keyAt(i)).get(FIELDS[0]);
                DeckInformation info = readFile( new File(importDirectory, fileName));
                output.add(info);
            }
        }
        return output;
    }

    /**
     * Use the instant-add API to add flashcards directly to AnkiDroid
     * @param data List of cards to be added. Each card has a HashMap of field name / field value pairs.
     */
    private void addOrUpdateCardsToAnkiDroid(final ArrayList<DeckInformation> input) {
        
      for (DeckInformation info: input) {
        final String deckName = info.name;
        final ArrayList<HashMap<String, String>> data = info.content;
        
        // Get api instance
        final AddContentApi api = new AddContentApi(AnkiImporter.this);
        // Look for our deck, add a new one if it doesn't exist
        Map<Long,String> decks = api.getDeckList();
        Long did = null;
        for (Map.Entry<Long, String> entry : decks.entrySet()) {
            if (entry.getValue().equals(deckName)) {
                did = entry.getKey();
                break;
            }
        }
        if (did == null) {
            did = api.addNewDeck(deckName);
        }
        // Look for our model, add a new one if it doesn't exist
        Long mid = null;
        
        Map<Long,String> models = api.getModelList();
        
        for (Map.Entry<Long, String> entry : decks.entrySet()) {
            if (entry.getValue().equals(AnkiDroidConfig.MODEL_NAME)) {
                mid = entry.getKey();
                break;
            }
        }
        
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
        int dupes = 0;
        
        Set<String> tags = new HashSet();
        List<String[]> fieldsList = new ArrayList();
        List<Set<String>> tagsList = new ArrayList();
        
        tags.add(AnkiDroidConfig.TAGS);
        for (HashMap<String, String> hm: data) {
            // Build a field map accounting for the fact that the user could have changed the fields in the model
            String[] flds = new String[fieldNames.length];
            for (int i = 0; i < flds.length; i++) {
                // Fill up the fields one-by-one up until either all fields are filled or we run out of fields to send
                if (i < AnkiDroidConfig.FIELDS.length) {
                    flds[i] = hm.get(AnkiDroidConfig.FIELDS[i]);
                }
            }
            fieldsList.add(flds);
            tagsList.add(tags);
            /*
            // Add a new note using the current field map
            try {
                // Only add item if there aren't any duplicates
                //if (!api.checkForDuplicates(mid, did, flds)) {
                    Long noteId = api.addNote(mid, did, flds, tags);
                    if (noteId != null) {
                        added++;
                    }
                //} else {
                //    dupes++;
                //}
            } catch (Exception e) {
                Log.e(LOG_TAG, "Exception adding cards to AnkiDroid", e);
                Toast.makeText(AnkiImporter.this, R.string.card_add_fail, Toast.LENGTH_LONG).show();
                return;
            }
            */
        }
        
        int ret = api.addNotes(mid, did, fieldsList, tagsList);
        
        Toast.makeText(AnkiImporter.this, getResources().getString(R.string.n_items_added, ret, 0), Toast.LENGTH_LONG).show();
      }
    }
}
