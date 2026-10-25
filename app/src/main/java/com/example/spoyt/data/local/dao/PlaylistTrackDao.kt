package com.example.spoyt.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.spoyt.data.local.entities.PlaylistTrackCrossRef
import com.example.spoyt.data.local.entities.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistTrackDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(crossRef: PlaylistTrackCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(crossRefs: List<PlaylistTrackCrossRef>)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun remove(playlistId: Int, videoId: String)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun removeAll(playlistId: Int)

    @Query("""
        SELECT t.* FROM tracks t
        JOIN playlist_tracks pt ON t.videoId = pt.videoId
        WHERE pt.playlistId = :playlistId
        ORDER BY pt.position ASC
    """)
    fun getTracksInPlaylist(playlistId: Int): Flow<List<TrackEntity>>

    @Query("SELECT pt.videoId FROM playlist_tracks pt WHERE pt.playlistId = :playlistId ORDER BY pt.position ASC")
    suspend fun getVideoIdsInPlaylist(playlistId: Int): List<String>

    @Query("SELECT * FROM playlist_tracks")
    suspend fun getAllCrossRefs(): List<PlaylistTrackCrossRef>

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE videoId = :videoId")
    suspend fun isInAnyPlaylist(videoId: String): Boolean
}
