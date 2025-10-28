package com.applications.player.data.db


import android.net.Uri
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.IOException

/**
 * Teaches Gson how to serialize and deserialize an android.net.Uri object.
 */
class UriTypeAdapter : TypeAdapter<Uri>() {

    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: Uri?) {
        // Convert the Uri to a String and write it to JSON
        out.value(value?.toString())
    }

    @Throws(IOException::class)
    override fun read(input: JsonReader): Uri? {
        // Read the String from JSON and parse it back into a Uri
        try {
            val uriString = input.nextString()
            return if (uriString == null) {
                null
            } else {
                Uri.parse(uriString)
            }
        }catch (e: Exception){
            return null
            e.printStackTrace()
        }

    }
}