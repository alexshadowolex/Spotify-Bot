package handler

import com.adamratzman.spotify.models.Playable
import com.adamratzman.spotify.models.Track
import spotifyClient

class RequestedByQueueHandler {
    private val requestedByQueue = mutableListOf<RequestedByEntry>()

    suspend fun updateRequestedByQueue(queueBefore: List<Playable>) {
        if(requestedByQueue.isEmpty()) {
            return
        }

        val positionAndTrack = getAddedTrack(queueBefore)

        // TODO more
    }

    private suspend fun getAddedTrack(queueBefore: List<Playable>): PositionAndTrack? {
        val queueAfter = spotifyClient.player.getUserQueue().queue

        for(index in 0..queueBefore.size) {
            val currentTrackBefore = queueBefore.getOrNull(index) ?: break
            val currentTrackAfter = queueAfter.getOrNull(index) ?: break
            if (currentTrackBefore != currentTrackAfter) {
                val currentTrackAfterParsed = currentTrackAfter.asTrack

                if (currentTrackAfterParsed != null) {
                    return PositionAndTrack(index, currentTrackAfterParsed)
                }
            }
        }

        return null
    }

    fun addEntryToRequestedByQueue(track: Track, userName: String) {

    }
}

data class PositionAndTrack (
    var positionInQueue: Int,
    val track: Track
)

data class RequestedByEntry (
    val positionAndTrack: PositionAndTrack,
    val userName: String,
    val sameTrackInQueueBefore: MutableSet<PositionAndTrack>
)