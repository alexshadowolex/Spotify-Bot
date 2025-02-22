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
            val sameTracksInQueueBefore = getSameTracksInQueueBefore(positionAndTrack, queueBefore)
        }
    }

    private fun getSameTracksInQueueBefore(
        currentTrack: IndexInQueueAndTrack,
        queue: List<Playable>
    ): MutableList<IndexInQueueAndTrack> {
        return queue.subList(0, currentTrack.indexInQueue)
            .withIndex()
            .filter { it.value.asTrack == currentTrack.track }
            .map { IndexInQueueAndTrack(it.index, it.value.asTrack!!) }.toMutableList()
    }
}

data class IndexInQueueAndTrack (
    var indexInQueue: Int,
    val track: Track
)

data class RequestedByEntry (
    val indexInQueueAndTrack: IndexInQueueAndTrack,
    val userName: String,
    val sameTrackInQueueBefore: MutableList<IndexInQueueAndTrack>
)