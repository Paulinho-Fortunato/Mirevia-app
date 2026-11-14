package ao.mirevia.core.model

data class LibraryEntry(
    val media: MediaSummary,
    val favorite: Boolean = false,
    val watchlisted: Boolean = false,
    val watched: Boolean = false,
    val updatedAt: Long,
)

/** OPENED does not assert that the iframe's video has started. */
enum class PlaybackStatus {
    OPENED,
    LEFT,
}

data class PlaybackEntry(
    val id: Long,
    val media: MediaSummary,
    val season: Int? = null,
    val episode: Int? = null,
    val openedAt: Long,
    val leftAt: Long? = null,
    val status: PlaybackStatus = PlaybackStatus.OPENED,
    val positionMillis: Long? = null,
    val durationMillis: Long? = null,
)

data class EpisodeWatchState(
    val mediaKey: MediaKey,
    val season: Int,
    val episode: Int,
    val watchedAt: Long,
)

enum class LibraryFlag {
    FAVORITE,
    WATCHLIST,
    WATCHED,
}
