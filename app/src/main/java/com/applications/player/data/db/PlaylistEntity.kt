import androidx.room.Entity
import androidx.room.PrimaryKey
import com.applications.player.model.Video

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val videos: List<Video> = emptyList()
)