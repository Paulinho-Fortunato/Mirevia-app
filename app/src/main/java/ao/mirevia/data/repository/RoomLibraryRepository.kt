package ao.mirevia.data.repository

import androidx.room.withTransaction
import ao.mirevia.core.model.*
import ao.mirevia.core.repository.LibraryRepository
import ao.mirevia.data.local.*
import java.util.Locale
import kotlinx.coroutines.flow.map

class RoomLibraryRepository(private val db: MireviaDatabase) : LibraryRepository {
    override val entries =
        db.library().observe().map { rows ->
            rows.map {
                LibraryEntry(
                    it.media.toDomain(),
                    it.favorite,
                    it.watchlisted,
                    it.watched,
                    it.updatedAt,
                )
            }
        }
    override val history =
        db.playback().observe().map { rows ->
            rows.map {
                PlaybackEntry(
                    it.id,
                    it.media.toDomain(),
                    it.season,
                    it.episode,
                    it.openedAt,
                    it.leftAt,
                    PlaybackStatus.valueOf(it.status),
                    it.positionMillis,
                    it.durationMillis,
                )
            }
        }
    override val searchHistory = db.searches().observe().map { rows -> rows.map { it.query } }

    override fun episodeStates(key: MediaKey) =
        db.episodes().observe(key.storageKey).map { rows ->
            rows.map { EpisodeWatchState(key, it.season, it.episode, it.watchedAt) }
        }

    override suspend fun toggle(media: MediaSummary, flag: LibraryFlag) =
        db.withTransaction {
            val old = db.library().get(media.key.storageKey)
            val favorite =
                if (flag == LibraryFlag.FAVORITE) !(old?.favorite ?: false)
                else old?.favorite ?: false
            val watchlisted =
                if (flag == LibraryFlag.WATCHLIST) !(old?.watchlisted ?: false)
                else old?.watchlisted ?: false
            val watched =
                if (flag == LibraryFlag.WATCHED) !(old?.watched ?: false) else old?.watched ?: false
            if (!favorite && !watchlisted && !watched) db.library().delete(media.key.storageKey)
            else
                db.library()
                    .upsert(
                        LibraryEntity(
                            media.key.storageKey,
                            MediaSnapshot.from(media),
                            favorite,
                            watchlisted,
                            watched,
                            System.currentTimeMillis(),
                        )
                    )
        }

    override suspend fun remove(key: MediaKey) = db.library().delete(key.storageKey)

    override suspend fun markEpisode(key: MediaKey, season: Int, episode: Int, watched: Boolean) {
        require(key.type == MediaType.TV && season >= 0 && episode > 0)
        if (watched)
            db.episodes()
                .upsert(
                    EpisodeWatchEntity(key.storageKey, season, episode, System.currentTimeMillis())
                )
        else db.episodes().delete(key.storageKey, season, episode)
    }

    override suspend fun recordOpened(media: MediaSummary, season: Int?, episode: Int?): Long =
        db.withTransaction {
            val id =
                db.playback()
                    .insert(
                        PlaybackEntity(
                            media = MediaSnapshot.from(media),
                            season = season,
                            episode = episode,
                            openedAt = System.currentTimeMillis(),
                        )
                    )
            db.playback().trim()
            id
        }

    override suspend fun recordLeft(historyId: Long) =
        db.playback().close(historyId, System.currentTimeMillis())

    override suspend fun removeHistory(historyId: Long) = db.playback().delete(historyId)

    override suspend fun clearHistory() = db.playback().clear()

    override suspend fun rememberSearch(query: String) =
        db.withTransaction {
            val trimmed = query.trim().take(200)
            if (trimmed.length >= 2) {
                db.searches()
                    .upsert(
                        SearchEntity(
                            trimmed.lowercase(Locale.ROOT),
                            trimmed,
                            System.currentTimeMillis(),
                        )
                    )
                db.searches().trim()
            }
        }

    override suspend fun clearSearchHistory() = db.searches().clear()

    override suspend fun clearAll() =
        db.withTransaction {
            db.library().clear()
            db.playback().clear()
            db.episodes().clear()
            db.searches().clear()
        }
}
