package handler

import addQuotationMarks
import com.adamratzman.spotify.models.Playable
import com.adamratzman.spotify.models.Track
import currentSpotifySong
import logger
import spotifyClient

class RequestedByQueueHandler {
    private val requestedByQueue = mutableListOf<RequestedByEntry>()
    // Displays the current username if the song got requested by said user
    var currentRequestedByUsername: String? = null


    /**
     * Updates the requested by queue. Things that will be updated are:
     *  - indexes, where the songs are supposed to be currently (-1)
     *  - get and update the current requested by username (if there is one)
     *  - update the amount of the same track in queue before
     *  - remove overdue tracks
     * Has to be called after the currentSpotifySong got updated (= changed) to correctly update the values.
     * To know which amounts of the same track before to update, the spotify song name getter gives the
     * song before the change as a parameter.
     * @param trackBeforeChange the track before the currently playing track got updated to the new one
     */
    fun updateRequestedByQueue(trackBeforeChange: Track?) {
        currentRequestedByUsername = null
        if(requestedByQueue.isEmpty()) {
            return
        }

        updateIndexesOfQueue()

        val foundUserName = getAndRemoveFoundUserName()

        if(foundUserName != null) {
            currentRequestedByUsername = foundUserName
            logger.info(
                "Current track " +
                currentSpotifySong!!.name.addQuotationMarks() +
                " is requested by user " +
                currentRequestedByUsername
            )
        } else {
            updateAmountOfSameTrackInQueueBefore(trackBeforeChange)
        }

        removeOverdueTracks()
    }


    /**
     * Searches the requestedByQueue, if the current song is requested by a user. If it is, it returns the
     * username and removes it.
     * @return The username, if the current song is requested by them, else null
     */
    private fun getAndRemoveFoundUserName(): String? {
        val userName = requestedByQueue.find { isCurrentSongRequestedByUser(it) }?.userName
        if(userName != null) {
            requestedByQueue.removeIf { isCurrentSongRequestedByUser(it) }
        }

        return userName
    }


    /**
     * Helper function which checks if the current song is requested by a user in the requestedByQueue. This is the
     * case, when the saved track in the requestedByQueue is the same as the currently playing song and the amount
     * of same tracks before is 0.
     * @param entry the requestedByQueue-entry to check for
     * @return true, if the song is requested by the user given in the entry, else false
     */
    private fun isCurrentSongRequestedByUser(entry: RequestedByEntry): Boolean {
        return  entry.indexInQueueAndTrack.track == currentSpotifySong &&
                entry.amountOfSameTrackInQueueBefore == 0
    }


    /**
     * Updates (reduces by 1) all amounts of the same track before, specified by the trackBeforeChange parameter.
     * @param trackBeforeChange the track before the currently playing track got updated
     */
    private fun updateAmountOfSameTrackInQueueBefore(trackBeforeChange: Track?) {
        requestedByQueue.filter {
            it.indexInQueueAndTrack.track == trackBeforeChange
        }.forEach {
            it.amountOfSameTrackInQueueBefore--
        }
    }


    /**
     * Removes tracks that are overdue and apparently got removed out of the queue by the app. To have a small buffer
     * for untrackable changes, they will not be removed right after index = 0, but later.
     */
    private fun removeOverdueTracks() {
        // To have a little buffer in case of untrackable changes, the index for
        // when overdue tracks are going to get removed is not less than 0 but less than -2
        val overdueIndex = -3
        requestedByQueue.removeIf {
            it.indexInQueueAndTrack.indexInQueue < overdueIndex
        }
    }


    /**
     * Updates (reduces by 1) all indexes in the requestedByQueue.
     */
    private fun updateIndexesOfQueue() {
        requestedByQueue.forEach { entry ->
            entry.indexInQueueAndTrack.indexInQueue--
        }
    }


    /**
     * Compares two lists and finds the song that has been added to the second list and its index in the queue.
     * @param queueBefore the spotify queue before the song has been added via song-request
     * @return The Track and its index on success, null on error
     */
    private suspend fun getAddedTrackWithIndex(queueBefore: List<Playable>): IndexInQueueAndTrack? {
        val queueAfter = mutableListOf(currentSpotifySong as Playable)
        queueAfter.addAll(spotifyClient.player.getUserQueue().queue)

        for(index in 0..queueBefore.size) {
            val currentTrackBefore = queueBefore.getOrNull(index) ?: break
            val currentTrackAfter = queueAfter.getOrNull(index) ?: break

            if (currentTrackBefore != currentTrackAfter) {
                val currentTrackAfterParsing = currentTrackAfter.asTrack

                if (currentTrackAfterParsing != null) {
                    return IndexInQueueAndTrack(index, currentTrackAfterParsing)
                }
            }
        }

        return null
    }


    /**
     * Adds an entry to the requestedByQueue. The queueBeforeWithoutCurrentlyPlaying has to be saved before successfully
     * adding the song to the queue. This function then has to be called after the song got added to the queue.
     * @param queueBeforeWithoutCurrentlyPlaying the queue (without currently playing song) before adding the song request
     * @param userName name of the requesting user
     *
     */
    suspend fun addEntryToRequestedByQueue(queueBeforeWithoutCurrentlyPlaying: List<Playable>, userName: String) {
        val queueBefore = mutableListOf(currentSpotifySong as Playable)
        queueBefore.addAll(queueBeforeWithoutCurrentlyPlaying)

        val indexInQueueAndTrack = getAddedTrackWithIndex(queueBefore)

        if(indexInQueueAndTrack != null) {
            val amountOfSameTrackBefore = getAmountOfSameTrackInQueue(
                indexInQueueAndTrack,
                queueBefore
            )

            requestedByQueue += RequestedByEntry(
                indexInQueueAndTrack, userName, amountOfSameTrackBefore
            )
            logger.info("Added new RequestedByEntry. New queue: " +
                    requestedByQueue.joinToString(" | ") {
                        "(" +
                                "index: ${it.indexInQueueAndTrack.indexInQueue}, " +
                                "track: ${it.indexInQueueAndTrack.track.name}, " +
                                "amount of same before: ${it.amountOfSameTrackInQueueBefore}, " +
                                "username: ${it.userName}" +
                        ")"
                    }
            )
        }
    }


    /**
     * Finds the amount of identical tracks in the queue before the given track.
     * @param currentTrack the reference-track and its index
     * @param queue the whole queue (before or after the song got added)
     * @return the amount of the given track before the reference-index
     */
    private fun getAmountOfSameTrackInQueue(
        currentTrack: IndexInQueueAndTrack,
        queue: List<Playable>
    ): Int {
        return queue.subList(0, currentTrack.indexInQueue).count { it.asTrack == currentTrack.track }
    }
}

data class IndexInQueueAndTrack (
    var indexInQueue: Int,
    val track: Track
)

data class RequestedByEntry (
    val indexInQueueAndTrack: IndexInQueueAndTrack,
    val userName: String,
    var amountOfSameTrackInQueueBefore: Int
)