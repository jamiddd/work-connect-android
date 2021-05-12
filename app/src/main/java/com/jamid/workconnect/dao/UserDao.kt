package com.jamid.workconnect.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jamid.workconnect.model.User

@Dao
abstract class UserDao: BaseDao<User>() {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCurrentUser(user: User?)

    @Query("SELECT * FROM users WHERE id = :uid")
    abstract suspend fun getUser(uid: String): User?

    @Query("SELECT * FROM users WHERE isCurrentUser = 0")
    abstract fun randomUsers(): DataSource.Factory<Int, User>

    @Query("SELECT * FROM users WHERE name LIKE :item")
    abstract fun getSearchedUsers(item: String): DataSource.Factory<Int, User>

	@Query("SELECT * FROM users WHERE user_followings LIKE :uid")
	abstract fun getFollowers(uid: String): DataSource.Factory<Int, User>

	@Query("SELECT * FROM users WHERE user_followings LIKE :uid AND name LIKE :query")
	abstract fun getFollowers(uid: String, query: String): DataSource.Factory<Int, User>

	@Query("SELECT * FROM users WHERE user_followers LIKE :uid")
	abstract fun getFollowings(uid: String): DataSource.Factory<Int, User>

	@Query("SELECT * FROM users WHERE user_followers LIKE :uid AND name LIKE :query")
	abstract fun getFollowings(uid: String, query: String): DataSource.Factory<Int, User>

	@Query("SELECT * FROM users WHERE isUserFollowingMe = 1")
	abstract fun getFollowersForCurrentUser(): DataSource.Factory<Int, User>

	@Query("SELECT * FROM users WHERE isUserFollowingMe = 1 AND name LIKE :query")
	abstract fun getFollowersForCurrentUserWithQuery(query: String): DataSource.Factory<Int, User>

	@Query("SELECT * FROM users WHERE isUserFollowed = 1")
	abstract fun getFollowingsForCurrentUser(): DataSource.Factory<Int, User>

	@Query("SELECT * FROM users WHERE isUserFollowed = 1 AND name LIKE :query")
	abstract fun getFollowingsForCurrentUserWithQuery(query: String): DataSource.Factory<Int, User>

	@Query("SELECT * FROM users")
	abstract suspend fun getAllUsers(): List<User>

	@Query("SELECT * FROM users WHERE user_chatChannels LIKE :chatChannelId")
	abstract fun getPagedContributors(chatChannelId: String): DataSource.Factory<Int, User>

	@Query("SELECT * FROM users WHERE user_chatChannels LIKE :channelId")
	abstract suspend fun getChannelContributors(channelId: String): List<User>

	@Query("SELECT * FROM users WHERE user_chatChannels LIKE :channelId")
	abstract fun getChannelContributorsLive(channelId: String): LiveData<List<User>>

}