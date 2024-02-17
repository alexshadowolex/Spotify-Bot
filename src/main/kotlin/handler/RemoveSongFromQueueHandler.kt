package handler

import backgroundCoroutineScope
import createSongString
import currentSongString
import getCurrentSpotifySong
import isSpotifySongNameGetterEnabled
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import logger
import spotifyClient
import kotlin.time.Duration.Companion.seconds

class RemoveSongFromQueueHandler () {
    private val songsMarkedForSkipping = mutableSetOf<String>()

    init {
        startRemoveSongFromQueueChecker()
    }


    /**
     * Adds a song string to the set of songs marked for skipping. The string is checked before for validity.
     * See isInputStringValid-function for the criteria of a valid string.
     * @param songDisplayString {String} display string of the song to be skipped
     * @return {Boolean} true, if adding it was a success, else false
     */
    fun addSongToSetMarkedForSkipping(songDisplayString: String): Boolean {
        if(!isInputStringValid(songDisplayString)) {
            return false
        }

        songsMarkedForSkipping.add(songDisplayString.trim())
        return true
    }


    /**
     * Starts the while-loop in a coroutine to check, if the current song is marked for skipping.
     */
    private fun startRemoveSongFromQueueChecker() {
        var delay = 0.1.seconds
        backgroundCoroutineScope.launch {
            while (isActive) {
                if(!isSpotifySongNameGetterEnabled()) {
                    val currentTrack = getCurrentSpotifySong()
                    if(currentTrack == null) {
                        delay(1.seconds)
                        continue
                    }

                    currentSongString = createSongString(currentTrack.name, currentTrack.artists)
                    delay = 2.seconds
                }

                if(songsMarkedForSkipping.contains(currentSongString)) {
                    try {
                        spotifyClient.player.skipForward()
                    } catch (e: Exception) {
                        logger.error("Skipping the current song failed: ", e)
                        delay(delay)
                        continue
                    }

                    songsMarkedForSkipping.remove(currentSongString)
                }

                delay(delay)
            }
        }
    }


    /**
     * Checks if the input string for addSongToSetMarkedForSkipping is a valid string.
     * Valid strings contain the following:
     * (1) The connection part between name and artist(s): " by "
     * (2) Two quotation marks
     * (3) A song name with at least one character
     * (4) A (list of) artists with at least one character
     * @param input {String} input string to check for
     * @return {Boolean} true, if the string is valid, else false
     */
    private fun isInputStringValid(input: String): Boolean {
        val minimumSizeOfStringWithQuotationMarks = 2

        val containsInputConnectionPart = input.contains(" by ")
        val containsInputTwoQuotationMarks = input.indexOf("\"") != -1 && input.substringAfter("\"").indexOf("\"") != -1
        val isSongNamePartAtLeastOneCharacter = input.substringBefore(" by ").length > minimumSizeOfStringWithQuotationMarks
        val isArtistsPartAtLeastOneCharacter = input.substringAfter(" by ").isNotEmpty()

        return  containsInputConnectionPart &&
                containsInputTwoQuotationMarks &&
                isSongNamePartAtLeastOneCharacter &&
                isArtistsPartAtLeastOneCharacter
    }
}