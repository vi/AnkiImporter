package com.ichi2.apisample

import java.util.*

object AnkiDroidConfig {
    // Name of deck which will be created in AnkiDroid
    val DECK_NAME = "ImporterSample"
    // Name of model which will be created in AnkiDroid
    const val MODEL_NAME = "com.ichi2.plaintextimporter"
    // Optional space separated list of tags to add to every note
    const val TAGS = "AnkiImporter2"
    // List of field names that will be used in AnkiDroid model
    val FIELDS = arrayOf("Front", "Back")

    // Define two keys which will be used when using legacy ACTION_SEND intent
    val FRONT_SIDE_KEY = FIELDS[0]
    val BACK_SIDE_KEY = FIELDS[1]

    /**
     * Generate the ArrayList<HashMap> example data which will be sent to AnkiDroid
     * @return
    </HashMap> */
    val exampleData: ArrayList<HashMap<String, String>>
        get() {
            val exampleFronts = arrayOf("front1", "front2")
            val exampleBacks = arrayOf("back1", "back2")

            return ArrayList<HashMap<String, String>>().apply {
                for (idx in exampleFronts.indices) {
                    val hm = HashMap<String, String>()
                    hm[FIELDS[0]] = exampleFronts[idx]
                    hm[FIELDS[1]] = exampleBacks[idx]
                    add(hm)
                }
            }
        }
}
