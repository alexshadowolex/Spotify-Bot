package handler

import backgroundCoroutineScope
import com.adamratzman.spotify.models.Track
import createSongString
import currentSpotifySong
import getCurrentSpotifySong
import isSpotifySongNameGetterEnabled
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import logger
import me.xdrop.fuzzywuzzy.FuzzySearch
import spotifyClient
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class RemoveSongFromQueueHandler {
    private val songsMarkedForSkipping = mutableSetOf<Track>()

    init {
        startRemoveSongFromQueueChecker()
    }


    /**
     * Searches the user's current Spotify queue for a track matching the given query
     * and marks it for skipping.
     *
     * The query is resolved using fuzzy string matching against the tracks in the
     * current queue.
     * If a close enough match is found, the corresponding
     * [Track] is added to the internal skip set and will be skipped automatically
     * once it becomes the currently playing song.
     *
     * @param songSearchQuery a user-provided search string identifying the song to skip
     * @return the matched [Track] if a suitable song was found and marked, or `null` otherwise
     */

    suspend fun addSongToSetMarkedForSkipping(songSearchQuery: String): Track? {
        val track = findTrackInQueue(songSearchQuery)

        if(track != null) {
            songsMarkedForSkipping.add(track)
        }

        return track
    }


    /**
     * Launches a background coroutine that continuously monitors the currently
     * playing Spotify track and skips it if it has been marked for removal.
     *
     * The polling interval adapts depending on whether an external Spotify song
     * name getter is enabled, minimizing unnecessary API calls when possible.
     * Any failures while attempting to skip playback are logged and retried
     * gracefully.
     */
    private fun startRemoveSongFromQueueChecker() {
        var delay: Duration
        backgroundCoroutineScope.launch {
            while (isActive) {
                // If the song name getter is enabled, we don't need to pull the name from the API again
                if(!isSpotifySongNameGetterEnabled()) {
                    val currentTrack = getCurrentSpotifySong()
                    if(currentTrack == null) {
                        delay(1.seconds)
                        continue
                    }

                    currentSpotifySong = currentTrack
                    delay = 2.seconds
                } else {
                    delay = 0.1.seconds
                }

                if(songsMarkedForSkipping.contains(currentSpotifySong)) {
                    try {
                        spotifyClient.player.skipForward()
                    } catch (e: Exception) {
                        logger.error("Skipping the current song failed: ", e)
                        delay(delay)
                        continue
                    }

                    songsMarkedForSkipping.remove(currentSpotifySong)
                }

                delay(delay)
            }
        }
    }


    /**
     * Attempts to find the best-matching track in the user's current Spotify queue
     * for the given input string.
     *
     * The input and each queued track are tokenized and compared using fuzzy
     * string matching. For each input token, the highest similarity score against
     * the track tokens is accumulated. The track with the highest total similarity
     * score is selected as the best match.
     *
     * @param input the search string used to identify a track in the queue
     * @return the closest matching [Track], or `null` if the queue is empty or
     * no suitable match could be determined
     */
    private suspend fun findTrackInQueue(input: String): Track? {
        val queue = try {
            spotifyClient.player.getUserQueue().queue
        } catch (e: Exception) {
            logger.error("Error while trying to get the user queue in findTrackInQueue: ", e)
            return null
        }

        if(queue.isEmpty()) {
            return null
        }

        val inputTokens = tokenize(input.lowercase(Locale.getDefault()))
        var bestMatch: Track? = null
        var bestSimilarity = 0

        for (song in queue) {
            val track = song.asTrack ?: continue
            val lowerCaseSongString = createSongString(track.name, track.artists).lowercase(Locale.getDefault())

            val songTokens = tokenize(lowerCaseSongString)
            var totalSimilarity = 0

            for (inputToken in inputTokens) {
                val maxSimilarity = songTokens.maxOfOrNull { FuzzySearch.partialRatio(inputToken, it) } ?: 0
                totalSimilarity += maxSimilarity
            }

            if (totalSimilarity > bestSimilarity) {
                bestMatch = track
                bestSimilarity = totalSimilarity
            }
        }

        return bestMatch
    }


    /**
     * Normalizes and tokenizes a string for fuzzy comparison.
     *
     * The input is converted to lowercase, split on non-word characters, and
     * stripped of punctuation to produce a list of comparable tokens.
     *
     * @param input the raw input string to tokenize
     * @return a list of lowercase tokens derived from the input
     */
    private fun tokenize(input: String): List<String> {
        return input.lowercase(Locale.getDefault()).split(Regex("\\W+"))
    }
}