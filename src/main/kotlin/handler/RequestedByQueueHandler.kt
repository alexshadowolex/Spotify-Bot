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
     * Updates the internal "requested by" queue after the currently playing track changes.
     *
     * This method performs several maintenance tasks:
     * - Decrements stored queue indexes to reflect playback progression
     * - Determines whether the current track was requested by a user and exposes the username
     * - Updates counters for identical tracks appearing earlier in the queue
     * - Removes entries that have become overdue due to queue changes
     *
     * This function must be called **after** [currentSpotifySong] has been updated.
     * The previously playing track must be supplied to correctly adjust duplicate-track counters.
     *
     * @param trackBeforeChange the track that was playing before the current track update,
     * or `null` if unavailable
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
     * Checks whether the currently playing track was requested by a user.
     *
     * If a matching request entry is found, the associated username is returned
     * and all corresponding entries are removed from the queue to prevent reuse.
     *
     * @return the username of the requesting user if the current track was requested,
     * or `null` otherwise
     */
    private fun getAndRemoveFoundUserName(): String? {
        val userName = requestedByQueue.find { isCurrentSongRequestedByUser(it) }?.userName
        if(userName != null) {
            requestedByQueue.removeIf { isCurrentSongRequestedByUser(it) }
        }

        return userName
    }


    /**
     * Determines whether a requested-by entry corresponds to the currently playing track.
     *
     * A match occurs when the stored track equals the current Spotify track and no
     * identical instances of the same track are still expected before it in the queue.
     *
     * @param entry the requested-by queue entry to evaluate
     * @return `true` if the entry represents a request for the current track, `false` otherwise
     */
    private fun isCurrentSongRequestedByUser(entry: RequestedByEntry): Boolean {
        return  entry.indexInQueueAndTrack.track == currentSpotifySong &&
                entry.amountOfSameTrackInQueueBefore == 0
    }


    /**
     * Decrements the count of identical tracks expected before each matching request entry.
     *
     * This adjustment reflects that the previously playing track has advanced out of the queue.
     *
     * @param trackBeforeChange the track that was playing before the current track update,
     * or `null` if unavailable
     */
    private fun updateAmountOfSameTrackInQueueBefore(trackBeforeChange: Track?) {
        requestedByQueue.filter {
            it.indexInQueueAndTrack.track == trackBeforeChange
        }.forEach {
            it.amountOfSameTrackInQueueBefore--
        }
    }


    /**
     * Removes requested-by entries that have become overdue due to queue modifications.
     *
     * To tolerate untrackable Spotify queue changes, entries are not removed immediately
     * when their index drops below zero, but only after exceeding a defined negative buffer.
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
     * Decrements the stored queue index of every requested-by entry.
     *
     * This reflects the progression of playback as the queue advances.
     */
    private fun updateIndexesOfQueue() {
        requestedByQueue.forEach { entry ->
            entry.indexInQueueAndTrack.indexInQueue--
        }
    }


    /**
     * Identifies the track newly added to the Spotify queue and determines its index.
     *
     * This function compares the queue state before and after a song request by scanning
     * for the first divergence between the two lists.
     *
     * @param queueBefore the full Spotify queue before the requested track was added
     * @return an [IndexInQueueAndTrack] describing the added track and its position,
     * or `null` if no difference could be determined
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
     * Records a new song request in the requested-by queue.
     *
     * The queue state before the request must be provided. After the track has been
     * successfully added to Spotify's queue, this function resolves the added track,
     * determines its position, tracks duplicates, and associates it with the requesting user.
     *
     * @param queueBeforeWithoutCurrentlyPlaying the Spotify queue (excluding the
     * currently playing track) before the request was added
     * @param userName the name of the user who requested the song
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
     * Counts how many identical instances of a track appear earlier in the queue.
     *
     * This value is used to determine when a requested track should be attributed
     * to the requesting user during playback.
     *
     * @param currentTrack the reference track and its index within the queue
     * @param queue the complete Spotify queue
     * @return the number of identical tracks occurring before the given index
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