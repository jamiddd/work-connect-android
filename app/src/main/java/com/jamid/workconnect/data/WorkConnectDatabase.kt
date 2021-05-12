package com.jamid.workconnect.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jamid.workconnect.BUG_TAG
import com.jamid.workconnect.Converters
import com.jamid.workconnect.dao.*
import com.jamid.workconnect.home.BlogItem
import com.jamid.workconnect.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [
    User::class,
    SimpleMessage::class,
    UserMinimal::class,
    MessageKey::class,
    BlogItem::class,
    ChatChannelContributor::class,
    SimpleMedia::class,
    ChatChannel::class,
    SimpleRequest::class,
    SimpleNotification::class,
    Post::class,
    SimpleTag::class], version = 1, exportSchema=false)
@TypeConverters(Converters::class)
abstract class WorkConnectDatabase: RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun messageKeyDao(): MessageKeyDao
    abstract fun chatChannelContributorDao(): ChatChannelContributorDao
    abstract fun simpleMediaDao(): SimpleMediaDao
    abstract fun userDao(): UserDao
    abstract fun chatChannelDao(): ChatChannelDao
    abstract fun postDao(): PostDao
    abstract fun userMinimalDao(): UserMinimalDao
    abstract fun notificationDao(): NotificationDao
    abstract fun activeRequestDao(): ActiveRequestDao
    abstract fun simpleTagsDao(): SimpleTagDao

	companion object {
        @Volatile private var instance: WorkConnectDatabase? = null

        fun getInstance(context: Context, scope: CoroutineScope): WorkConnectDatabase {
            return instance ?: synchronized(this) {
                instance ?: createDatabase(context, scope)
            }
        }

        private fun createDatabase(applicationContext: Context, scope: CoroutineScope) : WorkConnectDatabase {
            return Room.databaseBuilder(applicationContext, WorkConnectDatabase::class.java, "work_connect_database")
                .addCallback(DatabaseCallback(scope))
                .fallbackToDestructiveMigration()
                .build()
        }
    }

    private class DatabaseCallback(val scope: CoroutineScope): RoomDatabase.Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            scope.launch (Dispatchers.IO) {
                instance?.apply {
                    Log.d(BUG_TAG, "OnOpenDatabase")
                    postDao().clearPosts()
                }
            }
        }
    }

}