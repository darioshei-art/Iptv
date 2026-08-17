package com.example.m3uplayer.data.db

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM saved_playlists ORDER BY lastOpenedAt DESC")
    fun observePlaylists(): Flow<List<SavedPlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(playlist: SavedPlaylistEntity): Long

    @Query("UPDATE saved_playlists SET lastOpenedAt = :time WHERE id = :id")
    suspend fun touch(id: Long, time: Long = System.currentTimeMillis())

    @Delete
    suspend fun delete(playlist: SavedPlaylistEntity)

    @Query("SELECT * FROM favorite_items ORDER BY addedAt DESC")
    fun observeFavorites(): Flow<List<FavoriteItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(item: FavoriteItemEntity)

    @Query("DELETE FROM favorite_items WHERE uri = :uri")
    suspend fun removeFavorite(uri: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_items WHERE uri = :uri)")
    suspend fun isFavorite(uri: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(entry: PlaybackHistoryEntity)

    @Query("SELECT * FROM playback_history WHERE uri = :uri LIMIT 1")
    suspend fun getProgress(uri: String): PlaybackHistoryEntity?
}

@Database(
    entities = [SavedPlaylistEntity::class, FavoriteItemEntity::class, PlaybackHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "m3uplayer.db"
                ).build().also { INSTANCE = it }
            }
    }
}
