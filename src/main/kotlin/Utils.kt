
import com.adamratzman.spotify.models.Track
import kotlinx.serialization.Serializable
import java.io.OutputStream
import java.util.*
import kotlin.time.Duration
import kotlin.time.DurationUnit

class MultiOutputStream(private vararg val streams: OutputStream) : OutputStream() {
    override fun close() = streams.forEach(OutputStream::close)
    override fun flush() = streams.forEach(OutputStream::flush)

    override fun write(b: Int) = streams.forEach {
        it.write(b)
    }

    override fun write(b: ByteArray) = streams.forEach {
        it.write(b)
    }

    override fun write(b: ByteArray, off: Int, len: Int) = streams.forEach {
        it.write(b, off, len)
    }
}

@Serializable
data class SimplifiedSpotifyPlaybackResponse(
    val is_playing: Boolean
)

data class SongRequestResult(
    val track: Track?,
    val songRequestResultExplanation: String
)

@Serializable
data class GitHubReleaseResponse (
    val name: String,
    val body: String,
    val assets: List<GitHubReleaseAsset>
)

@Serializable
data class GitHubReleaseAsset (
    val name: String,
    val browser_download_url: String
)

enum class CustomCommandPermissions {
    BROADCASTER,
    MODERATOR,
    CUSTOM
}


/**
 * Adds quotation marks to the string. If the string is empty, it changes nothing.
 */
fun String.addQuotationMarks() =
    if(this.isNotEmpty()) {
        "\"$this\""
    } else {
        this
    }


/**
 * Joins all non-empty elements of a list to a string
 * @param delimiter delimiter between the list elements to join
 */
fun List<String>.joinToPropertiesString(delimiter: String) =
    this.filter { it.isNotEmpty() }.joinToString(delimiter)


/**
 * Joins all non-empty elements of a list to a string and formats them to lowercase
 * @param delimiter delimiter between the list elements to join
 */
fun List<String>.joinToLowercasePropertiesString(delimiter: String) =
    this.joinToPropertiesString(delimiter).lowercase(Locale.getDefault())


/**
 * Creates a String out of a duration based on an int value and given duration unit.
 * @param durationUnit the desired duration unit
 */
fun Duration.toIntPropertiesString(durationUnit: DurationUnit) =
    this.toInt(durationUnit).toString()


/**
 * Creates a String out of a duration based on a double value and given duration unit.
 * @param durationUnit the desired duration unit
 */
fun Duration.toDoublePropertiesString(durationUnit: DurationUnit) =
    this.toDouble(durationUnit).toString()