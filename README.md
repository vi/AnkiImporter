# Project abandoned

Function similar to this project is now available in AnkiDroid proper (though you need to create a deck manually).

Note that each import (using this tool) created a separate note type; you can use [this instruction](https://forums.ankiweb.net/t/how-do-i-merge-those-excessive-api-created-note-types-into-one/52210/6) to deduplicate them using database editing.

----

# AnkiImport

PlainText semicolon-separated file -> AnkiDroid importer

Based on [API sample](https://github.com/ankidroid/Anki-Android/wiki/AnkiDroid-API).

License is the same as license of the sample linked above.

# Running 

The app still works in Android 12, but is tricky to run properly:

1. You need to create /sdcard/AnkiDroid/TextImport directory manually
2. You need to know file format and filename-to-deck coding principle
3. You need to grant filesystem and AnkiDroid access permissions manually (using "App info"). The app does not request permissions, just fails to work if they are not granted.
4. You need to force-restart the app after granting permissions to re-read file list.

# Building

I can build it on Debian using command line similar to this one:

    JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 ANDROID_HOME=/path/to/androidsdk ./gradlew assembleDebug

## TODO/FIXME

Missing features:

* Configurable directory
* Non-basic note type, configurable field mapping
* Error handling
* Proper UI, better help message
* Upstream AnkiDroid integration
* Duplicate handling (was removed due to some bug)
* Storage Access Framework instead of relying on old school permission.
