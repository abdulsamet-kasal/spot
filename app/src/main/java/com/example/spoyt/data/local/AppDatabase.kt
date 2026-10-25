package com.example.spoyt.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.spoyt.data.local.dao.DownloadDao
import com.example.spoyt.data.local.dao.PlayHistoryDao
import com.example.spoyt.data.local.dao.PlaylistDao
import com.example.spoyt.data.local.dao.PlaylistTrackDao
import com.example.spoyt.data.local.dao.TrackDao
import com.example.spoyt.data.local.entities.DownloadEntity
import com.example.spoyt.data.local.entities.PlayHistoryEntity
import com.example.spoyt.data.local.entities.PlaylistEntity
import com.example.spoyt.data.local.entities.PlaylistTrackCrossRef
import com.example.spoyt.data.local.entities.TrackEntity

@Database(
    entities = [
        TrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        DownloadEntity::class,
        PlayHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistTrackDao(): PlaylistTrackDao
    abstract fun downloadDao(): DownloadDao
    abstract fun playHistoryDao(): PlayHistoryDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "spoyt_database"
                )
                    // No schema export directory configured; schema export is disabled via exportSchema = false.
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }
}
