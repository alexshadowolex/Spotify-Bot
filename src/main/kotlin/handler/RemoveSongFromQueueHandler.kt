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
     * Adds a song to the set of songs marked for skipping. The input is searched for in the current queue
     * to find the closest match.
     * @param songSearchQuery {String} query string of the song to be searched for and then skipped
     * @return {Track?} the track-object of the song to be skipped, if a fitting song was found, else null
     */
    suspend fun addSongToSetMarkedForSkipping(songSearchQuery: String): Track? {
        val track = findTrackInQueue(songSearchQuery)

        if(track != null) {
            songsMarkedForSkipping.add(track)
        }

        return track
    }


    /**
     * Starts the while-loop in a coroutine to check, if the current song is marked for skipping.
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
     * Finds the closest matching track from the current queue for the input using fuzzy string matching.
     * This function tokenizes the input and each display string of each song in the queue,
     * and calculates the similarity between corresponding tokens. It then selects the track with the
     * highest overall similarity as the closest match.
     * @param input {String} The input to find a match for.
     * @return {Track?} The closest matching track from the queue, or null if no match is found.
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
     * Lowercase and tokenize the input for the findTrackInQueue-function by splitting on whitespaces
     * and removing punctuation.
     * @param input {String} input to be tokenized
     * @return {List<String>} list of tokens
     */
    private fun tokenize(input: String): List<String> {
        return input.lowercase(Locale.getDefault()).split(Regex("\\W+"))
    }
}