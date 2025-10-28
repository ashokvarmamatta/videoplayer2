import android.net.Uri
import androidx.compose.ui.input.key.type
import androidx.room.TypeConverter
import com.applications.player.data.db.UriTypeAdapter
import com.applications.player.model.Video
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.GsonBuilder

class VideoListConverter {
    // Modify the gson instance creation
    private val gson: Gson = GsonBuilder()
        // Add parentheses to create an INSTANCE of the adapter
        .registerTypeAdapter(Uri::class.java, UriTypeAdapter()) // <-- THE FIX IS HERE
        .create()

    @TypeConverter
    fun fromVideoList(videos: List<Video>?): String? {
        return videos?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toVideoList(videoListJson: String?): List<Video>? {
        return videoListJson?.let {
            val type = object : TypeToken<List<Video>>() {}.type
            // This now works because our custom gson instance knows how to handle Uris
            gson.fromJson(it, type)
        }
    }
}