package com.jamid.workconnect.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jamid.workconnect.Converters
import com.jamid.workconnect.dao.ChatChannelContributorDao
import com.jamid.workconnect.dao.MessageDao
import com.jamid.workconnect.dao.MessageKeyDao
import com.jamid.workconnect.model.ChatChannelContributor
import com.jamid.workconnect.model.SimpleMessage
import kotlinx.coroutines.CoroutineScope

@Database(entities = [SimpleMessage::class, MessageKey::class, ChatChannelContributor::class, ChannelIds::class], version = 9, exportSchema=false)
@TypeConverters(Converters::class)
abstract class WorkConnectDatabase: RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun messageKeyDao(): MessageKeyDao
    abstract fun chatChannelContributorDao(): ChatChannelContributorDao

    companion object {
        @Volatile private var instance: WorkConnectDatabase? = null

        fun getInstance(context: Context, scope: CoroutineScope): WorkConnectDatabase {
            return instance ?: synchronized(this) {
                instance ?: createDatabase(context)
            }
        }

        private fun createDatabase(applicationContext: Context) : WorkConnectDatabase {
            return Room.databaseBuilder(applicationContext, WorkConnectDatabase::class.java, "work_connect_database").fallbackToDestructiveMigration()
                .build()
        }
    }

}