
import com.adamratzman.spotify.models.Track
import handler.WorkaroundTrack
import kotlinx.serialization.Serializable
import java.io.OutputStream
import java.util.*
import kotlin.time.Duration
import kotlin.time.DurationUnit


/**
 * An OutputStream implementation that forwards all write, flush, and close
 * operations to multiple underlying output streams.
 *
 * This is useful for duplicating output (for example, writing to a file
 * and the console simultaneously).
 *
 * @param streams the output streams to forward all operations to
 */
class MultiOutputStream(private vararg val streams: OutputStream) : OutputStream() {


    /**
     * Closes all underlying output streams.
     *
     * Each wrapped stream receives a close call.
     */
    override fun close() = streams.forEach(OutputStream::close)


    /**
     * Flushes all underlying output streams.
     *
     * Each wrapped stream receives a flush call.
     */
    override fun flush() = streams.forEach(OutputStream::flush)


    /**
     * Writes a single byte to all underlying output streams.
     *
     * @param b the byte to write
     */
    override fun write(b: Int) = streams.forEach {
        it.write(b)
    }


    /**
     * Writes the entire byte array to all underlying output streams.
     *
     * @param b the byte array to write
     */
    override fun write(b: ByteArray) = streams.forEach {
        it.write(b)
    }


    /**
     * Writes a portion of a byte array to all underlying output streams.
     *
     * @param b the byte array containing data
     * @param off the start offset in the array
     * @param len the number of bytes to write
     */
    override fun write(b: ByteArray, off: Int, len: Int) = streams.forEach {
        it.write(b, off, len)
    }
}


/**
 * Simplified representation of Spotify's playback state response.
 *
 * This model only includes the playback status flag.
 *
 * @property is_playing indicates whether playback is currently active
 */
@Serializable
data class SimplifiedSpotifyPlaybackResponse(
    val is_playing: Boolean
)


/**
 * Represents the result of a song request operation.
 *
 * @property track the resolved Spotify track, or null if resolution failed
 * @property songRequestResultExplanation a human-readable explanation of the result
 */
data class SongRequestResult(
    val track: WorkaroundTrack?,
    val songRequestResultExplanation: String
)


/**
 * Represents a GitHub release response returned by the GitHub Releases API.
 *
 * @property name the release name (usually containing the version)
 * @property body the release notes text
 * @property assets the list of downloadable assets associated with the release
 */
@Serializable
data class GitHubReleaseResponse (
    val name: String,
    val body: String,
    val assets: List<GitHubReleaseAsset>
)


/**
 * Represents a downloadable asset attached to a GitHub release.
 *
 * @property name the asset file name
 * @property browser_download_url the direct download URL for the asset
 */
@Serializable
data class GitHubReleaseAsset (
    val name: String,
    val browser_download_url: String
)


/**
 * Defines the security levels available for command execution.
 *
 * BROADCASTER and MODERATOR rely on permission checks,
 * while CUSTOM allows a configurable list of authorized users.
 */
enum class CustomCommandPermissions {
    BROADCASTER,
    MODERATOR,
    CUSTOM
}


/**
 * Wraps the string in quotation marks if it is not empty.
 *
 * Empty strings are returned unchanged.
 *
 * @receiver the original string
 * @return the quoted string, or the original string if empty
 */
fun String.addQuotationMarks() =
    if(this.isNotEmpty()) {
        "\"$this\""
    } else {
        this
    }


/**
 * Joins all non-empty elements of the list into a single string.
 *
 * Empty strings are filtered out before joining.
 *
 * @param delimiter the delimiter used between elements
 * @return a joined string containing only non-empty elements
 */
fun List<String>.joinToPropertiesString(delimiter: String) =
    this.filter { it.isNotEmpty() }.joinToString(delimiter)


/**
 * Joins all non-empty elements of the list into a lowercase string.
 *
 * The join uses the provided delimiter and applies locale-aware lowercasing.
 *
 * @param delimiter the delimiter used between elements
 * @return lowercase joined string of non-empty elements
 */
fun List<String>.joinToLowercasePropertiesString(delimiter: String) =
    this.joinToPropertiesString(delimiter).lowercase(Locale.getDefault())


/**
 * Converts this duration into an integer-based string representation
 * using the specified duration unit.
 *
 * @param durationUnit the unit to convert the duration to
 * @return the integer value of the duration as a string
 */
fun Duration.toIntPropertiesString(durationUnit: DurationUnit) =
    this.toInt(durationUnit).toString()


/**
 * Converts this duration into a double-based string representation
 * using the specified duration unit.
 *
 * @param durationUnit the unit to convert the duration to
 * @return the double value of the duration as a string
 */
fun Duration.toDoublePropertiesString(durationUnit: DurationUnit) =
    this.toDouble(durationUnit).toString()