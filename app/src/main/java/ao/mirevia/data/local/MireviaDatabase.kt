package ao.mirevia.data.local

import androidx.room.*
import ao.mirevia.core.model.*
import kotlinx.coroutines.flow.Flow

data class MediaSnapshot(
    val origin: String,
    val type: String,
    val mediaId: Int,
    val title: String,
    val originalTitle: String?,
    val overview: String?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val releaseDate: String?,
    val rating: Double?,
    val genreIds: String,
    val language: String?,
) {
    fun toDomain() =
        MediaSummary(
            MediaKey(DataOrigin.valueOf(origin), MediaType.valueOf(type), mediaId),
            title,
            originalTitle,
            overview,
            posterUrl,
            backdropUrl,
            releaseDate,
            rating,
            genreIds.split(',').mapNotNull(String::toIntOrNull),
            language,
        )

    companion object {
        fun from(media: MediaSummary) =
            MediaSnapshot(
                media.key.origin.name,
                media.key.type.name,
                media.key.id,
                media.title,
                media.originalTitle,
                media.overview,
                media.posterUrl,
                media.backdropUrl,
                media.releaseDate,
                media.rating,
                media.genreIds.joinToString(","),
                media.originalLanguage,
            )
    }
}

@Entity(tableName = "library")
data class LibraryEntity(
    @PrimaryKey val key: String,
    @Embedded(prefix = "media_") val media: MediaSnapshot,
    val favorite: Boolean,
    val watchlisted: Boolean,
    val watched: Boolean,
    val updatedAt: Long,
)

@Entity(tableName = "playback_history", indices = [Index("openedAt")])
data class PlaybackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @Embedded(prefix = "media_") val media: MediaSnapshot,
    val season: Int?,
    val episode: Int?,
    val openedAt: Long,
    val leftAt: Long? = null,
    val status: String = PlaybackStatus.OPENED.name,
    val positionMillis: Long? = null,
    val durationMillis: Long? = null,
)

@Entity(tableName = "episode_watches", primaryKeys = ["ownerKey", "season", "episode"])
data class EpisodeWatchEntity(
    val ownerKey: String,
    val season: Int,
    val episode: Int,
    val watchedAt: Long,
)

@Entity(tableName = "search_history")
data class SearchEntity(@PrimaryKey val normalized: String, val query: String, val usedAt: Long)

@Dao
interface LibraryDao {
    @Query("SELECT * FROM library ORDER BY updatedAt DESC") fun observe(): Flow<List<LibraryEntity>>

    @Query("SELECT * FROM library WHERE `key` = :key") suspend fun get(key: String): LibraryEntity?

    @Upsert suspend fun upsert(entry: LibraryEntity)

    @Query("DELETE FROM library WHERE `key` = :key") suspend fun delete(key: String)

    @Query("DELETE FROM library") suspend fun clear()
}

@Dao
interface PlaybackDao {
    @Query("SELECT * FROM playback_history ORDER BY openedAt DESC LIMIT 300")
    fun observe(): Flow<List<PlaybackEntity>>

    @Insert suspend fun insert(entry: PlaybackEntity): Long

    @Query("UPDATE playback_history SET leftAt = :at, status = 'LEFT' WHERE id = :id")
    suspend fun close(id: Long, at: Long)

    @Query("DELETE FROM playback_history WHERE id = :id") suspend fun delete(id: Long)

    @Query("DELETE FROM playback_history") suspend fun clear()

    @Query(
        "DELETE FROM playback_history WHERE id NOT IN (SELECT id FROM playback_history ORDER BY openedAt DESC, id DESC LIMIT 300)"
    )
    suspend fun trim()
}

@Dao
interface EpisodeDao {
    @Query("SELECT * FROM episode_watches WHERE ownerKey = :key ORDER BY season, episode")
    fun observe(key: String): Flow<List<EpisodeWatchEntity>>

    @Upsert suspend fun upsert(entry: EpisodeWatchEntity)

    @Query(
        "DELETE FROM episode_watches WHERE ownerKey = :key AND season = :season AND episode = :episode"
    )
    suspend fun delete(key: String, season: Int, episode: Int)

    @Query("DELETE FROM episode_watches") suspend fun clear()
}

@Dao
interface SearchDao {
    @Query("SELECT * FROM search_history ORDER BY usedAt DESC LIMIT 20")
    fun observe(): Flow<List<SearchEntity>>

    @Upsert suspend fun upsert(entry: SearchEntity)

    @Query(
        "DELETE FROM search_history WHERE normalized NOT IN (SELECT normalized FROM search_history ORDER BY usedAt DESC LIMIT 20)"
    )
    suspend fun trim()

    @Query("DELETE FROM search_history") suspend fun clear()
}

@Database(
    entities =
        [
            LibraryEntity::class,
            PlaybackEntity::class,
            EpisodeWatchEntity::class,
            SearchEntity::class,
        ],
    version = 1,
    exportSchema = true,
)
abstract class MireviaDatabase : RoomDatabase() {
    abstract fun library(): LibraryDao

    abstract fun playback(): PlaybackDao

    abstract fun episodes(): EpisodeDao

    abstract fun searches(): SearchDao
}
