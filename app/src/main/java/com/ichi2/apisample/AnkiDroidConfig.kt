package com.ichi2.apisample

import java.util.*

object AnkiDroidConfig {
    // Name of deck which will be created in AnkiDroid
    val DECK_NAME = "ImporterSample"
    // Name of model which will be created in AnkiDroid
    internal const val MODEL_NAME = "com.ichi2.plaintextimporter"
    // Optional space separated list of tags to add to every note
    internal const val TAGS = "AnkiImporter2"
    // List of field names that will be used in AnkiDroid model
    internal val FIELDS = arrayOf("Front", "Back")

    // Define two keys which will be used when using legacy ACTION_SEND intent
    val FRONT_SIDE_KEY = FIELDS[0]
    val BACK_SIDE_KEY = FIELDS[1]

    /**
     * Generate the ArrayList<HashMap> example data which will be sent to AnkiDroid
     * @return
    </HashMap> */
    val exampleData: ArrayList<HashMap<String, String>>
        get() {
            val EXAMPLE_FRONTS = arrayOf("front1", "front2")
            val EXAMPLE_BACKS = arrayOf("back1", "back2")

            val data = ArrayList<HashMap<String, String>>()
            for (idx in EXAMPLE_FRONTS.indices) {
                val hm = HashMap<String, String>()
                hm[FIELDS[0]] = EXAMPLE_FRONTS[idx]
                hm[FIELDS[1]] = EXAMPLE_BACKS[idx]
                data.add(hm)
            }
            return data
        }
}
