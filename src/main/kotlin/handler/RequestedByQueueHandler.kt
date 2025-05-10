package handler

import com.adamratzman.spotify.models.Playable
import com.adamratzman.spotify.models.Track
import currentSpotifySong
import logger
import spotifyClient

class RequestedByQueueHandler {
    private val requestedByQueue = mutableListOf<RequestedByEntry>()
    var currentRequestedByUsername: String? = null

    fun updateRequestedByQueue(trackBeforeChange: Track?) {
        currentRequestedByUsername = null
        if(requestedByQueue.isEmpty()) {
            return
        }

        updateIndexesOfQueue()

        val foundUserName = getAndRemoveFoundUserName()

        if(foundUserName != null) {
            currentRequestedByUsername = foundUserName
        } else {
            updateAmountOfSameTrackInQueueBefore(trackBeforeChange)
        }

        removeOverdueTracks()
    }

    private fun getAndRemoveFoundUserName(): String? {
        val userName = requestedByQueue.find { isCurrentSongRequestedByUser(it) }?.userName
        if(userName != null) {
            requestedByQueue.removeIf { isCurrentSongRequestedByUser(it) }
        }

        return userName
    }

    private fun isCurrentSongRequestedByUser(entry: RequestedByEntry): Boolean {
        return  entry.indexInQueueAndTrack.track == currentSpotifySong &&
                entry.amountOfSameTrackInQueueBefore == 0
    }

    private fun updateAmountOfSameTrackInQueueBefore(trackBeforeChange: Track?) {
        requestedByQueue.filter {
            it.indexInQueueAndTrack.track == trackBeforeChange
        }.forEach {
            it.amountOfSameTrackInQueueBefore--
        }
    }

    private fun removeOverdueTracks() {
        // To have a little buffer in case of untrackable changes, the index for
        // when overdue tracks are going to get removed is not less than 0 but less than -2
        val overdueIndex = -3
        requestedByQueue.removeIf {
            it.indexInQueueAndTrack.indexInQueue < overdueIndex
        }
    }

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

    suspend fun addEntryToRequestedByQueue(queueBeforeWithoutCurrentlyPlaying: List<Playable>, userName: String) {
        val queueBefore = mutableListOf(currentSpotifySong as Playable)
        queueBefore.addAll(queueBeforeWithoutCurrentlyPlaying)

        val indexInQueueAndTrack = getAddedTrackWithIndex(queueBefore)

        if(indexInQueueAndTrack != null) {
            val amountOfSameTrackBefore = getAmountOfSameTrackInQueue(indexInQueueAndTrack, queueBefore)

            requestedByQueue += RequestedByEntry(indexInQueueAndTrack, userName, amountOfSameTrackBefore)
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