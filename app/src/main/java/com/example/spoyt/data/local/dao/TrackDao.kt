package com.example.spoyt.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.spoyt.data.local.entities.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {

    // IGNORE (not REPLACE): SQLite REPLACE deletes the conflicting row, which would cascade
    // away this track's playlist_tracks / play_history children on every re-save.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(track: TrackEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(tracks: List<TrackEntity>)

    @Query("DELETE FROM tracks WHERE videoId = :videoId")
    suspend fun delete(videoId: String)

    @Query("SELECT * FROM tracks WHERE liked = 1 ORDER BY addedAt DESC")
    fun getLikedTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY addedAt DESC")
    fun getAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE videoId = :videoId")
    suspend fun getTrack(videoId: String): TrackEntity?

    @Query("SELECT COUNT(*) FROM tracks WHERE videoId = :videoId")
    suspend fun exists(videoId: String): Boolean

    @Query("UPDATE tracks SET liked = :liked WHERE videoId = :videoId")
    suspend fun setLiked(videoId: String, liked: Boolean)

    // Rows written before search results carried an uploader stay blank forever, because
    // insert() is IGNORE on conflict. These two queries let the app heal them.
    @Query("SELECT * FROM tracks WHERE artist = '' ORDER BY addedAt DESC LIMIT :limit")
    suspend fun getTracksMissingArtist(limit: Int): List<TrackEntity>

    @Query("UPDATE tracks SET artist = :artist WHERE videoId = :videoId AND artist = ''")
    suspend fun fillArtist(videoId: String, artist: String)
}
