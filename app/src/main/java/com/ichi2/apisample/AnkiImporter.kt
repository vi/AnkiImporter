package com.ichi2.apisample

import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.AbsListView
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.ichi2.anki.api.AddContentApi
import com.ichi2.plaintextimporter.R
import java.io.File
import java.util.*

class AnkiImporter : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {
    private var importDirectory: File? = null

    private var mListView: ListView? = null
    private var mListData: ArrayList<HashMap<String, String>>? = null

    private val filenames: ArrayList<HashMap<String, String>>
        get() {
            val output = ArrayList<HashMap<String, String>>()

            if (importDirectory == null) return output

            val files = importDirectory!!.listFiles() ?: return output

            for (i in files) {
                val hm = HashMap<String, String>()
                hm[FIELDS[0]] = i.name
                output.add(hm)
            }

            return output
        }

    private inner class DeckInformation {
        var name: String? = null
        internal var content: ArrayList<HashMap<String, String>>? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        importDirectory = File(File(android.os.Environment.getExternalStorageDirectory(), "AnkiDroid"), "TextImport")

        setContentView(R.layout.activity_main)
        // Create the example data
        mListData = filenames

        if (mListData!!.size == 0) {
            importDirectory!!.mkdirs()
            val builder = AlertDialog.Builder(this)
            builder.setMessage(resources.getString(R.string.place_files, importDirectory!!.absolutePath))
            builder.create().show()
        }

        // Setup the ListView containing the example data
        mListView = findViewById(R.id.main_list)
        mListView!!.adapter = SimpleAdapter(this, mListData, R.layout.word_layout,
                FIELDS,
                intArrayOf(R.id.filename_item))
        mListView!!.choiceMode = ListView.CHOICE_MODE_MULTIPLE_MODAL
        // When an item is long-pressed the ListSelectListener will make a Contextual Action Bar with Share icon
        mListView!!.setMultiChoiceModeListener(ListSelectListener())
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode == AD_PERM_REQUEST && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            addOrUpdateCardsToAnkiDroid(gatherData())
        } else {
            Toast.makeText(this@AnkiImporter, R.string.permission_denied, Toast.LENGTH_LONG).show()
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    /**
     * Inner class that handles the contextual action bar that appears when an item is long-pressed in the ListView
     */
    internal inner class ListSelectListener : AbsListView.MultiChoiceModeListener {

        override fun onItemCheckedStateChanged(mode: android.view.ActionMode, position: Int, id: Long, checked: Boolean) {
            // Set the subtitle on the action bar to show how many items are selected
            val numItemsChecked = mListView!!.checkedItemCount
            val subtitle = resources.getString(R.string.n_items_selected, numItemsChecked)
            mode.subtitle = subtitle
        }

        override fun onCreateActionMode(mode: android.view.ActionMode, menu: Menu): Boolean {
            // Inflate the menu resource while holds the contextual action bar actions
            val inflater = mode.menuInflater
            inflater.inflate(R.menu.action_mode_menu, menu)
            return true
        }

        override fun onPrepareActionMode(mode: android.view.ActionMode, menu: Menu): Boolean {
            // Don't need to do anything here
            return false
        }

        override fun onActionItemClicked(mode: android.view.ActionMode, item: MenuItem): Boolean {
            // This is called when the contextual action bar buttons are pressed
            return when (item.itemId) {
                R.id.share_data_button -> {
                    // Add all data using AnkiDroid provider
                    addOrUpdateCardsToAnkiDroid(gatherData())

                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: android.view.ActionMode) {
            // Don't need to do anything here
        }
    }

    private fun filterString(`in`: String): String {
        return `in`.trim { it <= ' ' }.replace("&".toRegex(), "&amp;").replace("<".toRegex(), "&lt;")
    }

    private fun readFile(file: File): DeckInformation {

        val basenameExt = file.name.split("\\.(?=[^.]+$)".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val basename = basenameExt[0]
        val deckName = basename.replace("\\.".toRegex(), "::")

        val data = ArrayList<HashMap<String, String>>()

        try {
            file.forEachLine { line ->
                val split = line.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (split.size > 1) {
                    val hm = HashMap<String, String>()
                    hm[AnkiDroidConfig.FIELDS[0]] = filterString(split[0])
                    hm[AnkiDroidConfig.FIELDS[1]] = filterString(split[1])
                    data.add(hm)
                }
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

        val info = DeckInformation()
        info.name = deckName
        info.content = data
        return info
    }

    private fun gatherData(): ArrayList<DeckInformation> {
        val output = ArrayList<DeckInformation>()

        // Extract the selected data
        val checked = mListView!!.checkedItemPositions
        for (i in 0 until checked.size()) {
            if (checked.valueAt(i)) {
                val fileName = mListData!![checked.keyAt(i)][FIELDS[0]]
                val info = readFile(File(importDirectory, fileName))
                output.add(info)
            }
        }
        return output
    }

    /**
     * Use the instant-add API to add flashcards directly to AnkiDroid
     *
     * @param input List of cards to be added. Each card has a HashMap of field name / field value pairs.
     */
    private fun addOrUpdateCardsToAnkiDroid(input: ArrayList<DeckInformation>) {

        for (info in input) {
            val deckName = info.name
            val data = info.content

            // Get api instance
            val api = AddContentApi(this@AnkiImporter)
            // Look for our deck, add a new one if it doesn't exist
            val decks = api.deckList
            var did: Long? = null
            for ((key, value) in decks) {
                if (value == deckName) {
                    did = key
                    break
                }
            }
            if (did == null) {
                did = api.addNewDeck(deckName)
            }
            // Look for our model, add a new one if it doesn't exist
            var mid: Long? = null

            for ((key, value) in decks) {
                if (value == AnkiDroidConfig.MODEL_NAME) {
                    mid = key
                    break
                }
            }

            if (mid == null) {
                mid = api.addNewBasicModel(AnkiDroidConfig.MODEL_NAME)
            }
            // Double-check that everything was added correctly
            val fieldNames = api.getFieldList(mid!!)
            if (did == null || fieldNames == null) {
                Toast.makeText(this@AnkiImporter, R.string.card_add_fail, Toast.LENGTH_LONG).show()
                return
            }
            // Add cards

            val tags = HashSet<String>()
            val fieldsList = ArrayList<Array<String?>>()
            val tagsList = ArrayList<Set<String>>()

            tags.add(AnkiDroidConfig.TAGS)
            for (hm in data!!) {
                // Build a field map accounting for the fact that the user could have changed the fields in the model
                val flds = arrayOfNulls<String>(fieldNames.size)
                for (i in flds.indices) {
                    // Fill up the fields one-by-one up until either all fields are filled or we run out of fields to send
                    if (i < AnkiDroidConfig.FIELDS.size) {
                        flds[i] = hm[AnkiDroidConfig.FIELDS[i]]
                    }
                }
                fieldsList.add(flds)
                tagsList.add(tags)
                /*
                // Add a new note using the current field map
                try {
                    // Only add item if there aren't any duplicates
                    //if (!api.checkForDuplicates(mid, did, flds)) {
                    Long noteId = api.addNote(mid, did, flds, tags);
                    if (noteId != null) {
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

            val ret = api.addNotes(mid, did, fieldsList, tagsList)

            Toast.makeText(this@AnkiImporter, resources.getString(R.string.n_items_added, ret, 0), Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        //    public static final String LOG_TAG = "AnkiDroidApiSample";
        private const val AD_PERM_REQUEST = 0

        private val FIELDS = arrayOf("Filename")
    }
}
