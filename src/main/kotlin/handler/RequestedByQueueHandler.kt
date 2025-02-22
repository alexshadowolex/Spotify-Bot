package handler

import com.adamratzman.spotify.models.Playable
import com.adamratzman.spotify.models.Track
import spotifyClient

class RequestedByQueueHandler {
    private val requestedByQueue = mutableListOf<RequestedByEntry>()

    fun updateRequestedByQueue() {
        if(requestedByQueue.isEmpty()) {
            return
        }

        // TODO more
    }


    /**
     * Compares two lists and finds the song that has been added to the second list and its index in the queue.
     * @param queueBefore the spotify queue before the song has been added via song-request
     * @return The Track and its index on success, null on error
     */
    private suspend fun getAddedTrack(queueBefore: List<Playable>): IndexInQueueAndTrack? {
        val queueAfter = spotifyClient.player.getUserQueue().queue

        for(index in 0..queueBefore.size) {
            val currentTrackBefore = queueBefore.getOrNull(index) ?: break
            val currentTrackAfter = queueAfter.getOrNull(index) ?: break
            if (currentTrackBefore != currentTrackAfter) {
                val currentTrackAfterParsed = currentTrackAfter.asTrack

                if (currentTrackAfterParsed != null) {
                    return IndexInQueueAndTrack(index, currentTrackAfterParsed)
                }
            }
        }

        return null
    }

    suspend fun addEntryToRequestedByQueue(queueBefore: List<Playable>, userName: String) {
        val positionAndTrack = getAddedTrack(queueBefore)

        if(positionAndTrack != null) {
            val sameTracksInQueueBefore = getIndexesOfSameTrackInQueue(positionAndTrack, queueBefore)
        }
    }


    /**
     * Finds all identical tracks and its indexes in the queue before the given track.
     * @param currentTrack the reference-track and its index
     * @param queue the whole queue (before or after the song got added)
     * @return a list containing all indexes inside the queue of occurrences of the
     * given track before the reference-index
     */
    private fun getIndexesOfSameTrackInQueue(
        currentTrack: IndexInQueueAndTrack,
        queue: List<Playable>
    ): MutableList<Int> {
        return queue.subList(0, currentTrack.indexInQueue)
            .withIndex()
            .filter { it.value.asTrack == currentTrack.track }
            .map { it.index }.toMutableList()
    }
}

data class IndexInQueueAndTrack (
    var indexInQueue: Int,
    val track: Track
)

data class RequestedByEntry (
    val indexInQueueAndTrack: IndexInQueueAndTrack,
    val userName: String,
    val sameTrackInQueueBefore: MutableList<Int>
)