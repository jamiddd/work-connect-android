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

    // Deprecated
    @Query("SELECT * FROM users WHERE isCurrentUser = 0")
    abstract fun randomUsers(): DataSource.Factory<Int, User>

	// Deprecated
    @Query("SELECT * FROM users WHERE name LIKE :item")
    abstract fun getSearchedUsers(item: String): DataSource.Factory<Int, User>


    @Query("SELECT * FROM users WHERE isCurrentUser = 1")
    abstract fun currentUser(): LiveData<User>

	// Deprecated
//	@Query("SELECT * FROM users WHERE user_followings LIKE :uid")
//	abstract fun getFollowers(uid: String): DataSource.Factory<Int, User>

	// Deprecated
	/*@Query("SELECT * FROM users WHERE user_followings LIKE :uid AND name LIKE :query")
	abstract fun getFollowers(uid: String, query: String): DataSource.Factory<Int, User>*/

	// Deprecated
	/*@Query("SELECT * FROM users WHERE user_followers LIKE :uid")
	abstract fun getFollowings(uid: String): DataSource.Factory<Int, User>*/

	// Deprecated
	/*@Query("SELECT * FROM users WHERE user_followers LIKE :uid AND name LIKE :query")
	abstract fun getFollowings(uid: String, query: String): DataSource.Factory<Int, User>*/

	// Deprecated
	/*@Query("SELECT * FROM users WHERE isUserFollowingMe = 1")
	abstract fun getFollowersForCurrentUser(): DataSource.Factory<Int, User>*/

	// Deprecated
	@Query("SELECT * FROM users WHERE isUserFollowingMe = 1 AND name LIKE :query")
	abstract fun getFollowersForCurrentUserWithQuery(query: String): DataSource.Factory<Int, User>

	// Deprecated
	/*@Query("SELECT * FROM users WHERE isUserFollowed = 1")
	abstract fun getFollowingsForCurrentUser(): DataSource.Factory<Int, User>*/

	// Deprecated
	@Query("SELECT * FROM users WHERE isUserFollowed = 1 AND name LIKE :query")
	abstract fun getFollowingsForCurrentUserWithQuery(query: String): DataSource.Factory<Int, User>

	@Query("SELECT * FROM users")
	abstract suspend fun getAllUsers(): List<User>

	// Deprecated
	@Query("SELECT * FROM users WHERE user_chatChannels LIKE :chatChannelId")
	abstract fun getPagedContributors(chatChannelId: String): DataSource.Factory<Int, User>

	@Query("SELECT * FROM users WHERE user_chatChannels LIKE :channelId")
	abstract suspend fun getChannelContributors(channelId: String): List<User>

	@Query("SELECT * FROM users WHERE user_chatChannels LIKE :channelId")
	abstract fun getChannelContributorsLive(channelId: String): LiveData<List<User>>

	@Query("DELETE FROM users WHERE id != :id")
	abstract suspend fun deleteOtherUsers(id: String)

	@Query("DELETE FROM users")
	abstract suspend fun deleteAllUsers()

	@Query("SELECT * FROM users WHERE isUserFollowed = 1 ORDER BY name ASC LIMIT :lim")
	abstract suspend fun getFollowingsForCurrentUser(lim: Int): List<User>

	@Query("SELECT * FROM users WHERE isUserFollowed = 1 AND name LIKE :query ORDER BY name ASC LIMIT :lim")
	abstract suspend fun getFollowingsForCurrentUserWithQuery(query: String, lim: Int): List<User>

	@Query("SELECT * FROM users WHERE user_followers LIKE :uid ORDER BY name ASC LIMIT :lim")
	abstract suspend fun getFollowings(uid: String, lim: Int): List<User>

	@Query("SELECT * FROM users WHERE user_followings LIKE :uid AND name LIKE :query ORDER BY name ASC LIMIT :lim")
	abstract suspend fun getFollowingsWithQuery(query: String, uid: String, lim: Int): List<User>

	@Query("SELECT * FROM users WHERE isUserFollowed = 1 AND name > :name ORDER BY name ASC LIMIT :lim")
	abstract suspend fun getFollowingsForCurrentUserAfter(name: String, lim: Int): List<User>

	@Query("SELECT * FROM users WHERE isUserFollowed = 1 AND name LIKE :query AND name > :name ORDER BY name ASC LIMIT :lim")
	abstract suspend fun getFollowingsForCurrentUserAfterWithQuery(query: String, name: String, lim: Int): List<User>

	@Query("SELECT * FROM users WHERE user_followings LIKE :uid AND name > :name ORDER BY name ASC LIMIT :lim")
	abstract suspend fun getFollowingsAfter(uid: String, name: String, lim: Int): List<User>

	@Query("SELECT * FROM users WHERE user_followings LIKE :uid AND name LIKE :query AND name > :name ORDER BY name ASC LIMIT :lim")
	abstract suspend fun getFollowingsWithQueryAfter(uid: String, query: String, name: String, lim: Int): List<User>

	@Query("SELECT * FROM users WHERE isUserFollowed = 1 AND name < :name ORDER BY name ASC LIMIT :lim")
	abstract suspend fun getFollowingsForCurrentUserBefore(name: String, lim: Int): List<User>

	@Query("SELECT * FROM users WHERE isUserFollowed = 1 AND name LIKE :query AND name < :name ORDER BY name ASC LIMIT :lim")
	abstract suspend fun getFollowingsForCurrentUserBeforeWithQuery(query: String, name: String, lim: Int): List<User>

	@Query("SELECT * FROM users WHERE user_followings LIKE :uid AND name < :name ORDER BY name ASC LIMIT :lim")
	abstract suspend fun getFollowingsBefore(uid: String, name: String, lim: Int): List<User>

	@Query("SELECT * FROM users WHERE user_followings LIKE :uid AND name LIKE :query AND name < :name ORDER BY name ASC LIMIT :lim")
	abstract suspend fun getFollowingsWithQueryBefore(uid: String, query: String, name: String, lim: Int): List<User>

	@Query("SELECT * FROM users WHERE isUserFollowingMe = 1 ORDER BY name ASC LIMIT :lim")
	abstract suspend fun getFollowersForCurrentUser(lim: Int): List<User>

	@Query("SELECT * FROM users WHERE isUserFollowingMe = 1 AND name LIKE :query ORDER BY name ASC LIMIT :lim")
	abstract suspend fun getFollowersForCurrentUserWithQuery(query: String, lim: Int): List<User>

	@Query("SELECT * FROM users WHERE isUserFollowingMe = 1 AND name > :name ORDER BY name ASC LIMIT :lim")
	abstract suspend fun getFollowersForCurrentUserAfter(name: String, lim: Int): List<User>

	@Query("SELECT * FROM users WHERE isUserFollowingMe = 1 AND name LIKE :query AND name > :name ORDER BY name ASC LIMIT :lim")
	abstract suspend fun getFollowersForCurrentUserAfterWithQuery(query: String, name: String, lim: Int): List<User>

	@Query("SELECT * FROM users WHERE isUserFollowingMe = 1 AND name < :name ORDER BY name ASC LIMIT :lim")
	abstract suspend fun getFollowersForCurrentUserBefore(name: String, lim: Int): List<User>

	@Query("SELECT * FROM users WHERE isUserFollowingMe = 1 AND name LIKE :query AND name < :name ORDER BY name ASC LIMIT :lim")
	abstract suspend fun getFollowersForCurrentUserBeforeWithQuery(query: String, name: String, lim: Int): List<User>


	@Query("SELECT * FROM users WHERE user_followings LIKE :uid ORDER BY name ASC LIMIT :lim")
	abstract suspend fun getFollowers(uid: String, lim: Int): List<User>

	@Query("SELECT * FROM users WHERE user_followings LIKE :uid AND name LIKE :query ORDER BY name ASC LIMIT :lim")
	abstract suspend fun getFollowersWithQuery(uid: String, query: String, lim: Int): List<User>

	@Query("SELECT * FROM users WHERE user_followings LIKE :uid AND name < :name ORDER BY name ASC LIMIT :lim")
	abstract suspend fun getFollowersBefore(uid: String, name: String, lim: Int): List<User>

	@Query("SELECT * FROM users WHERE user_followings LIKE :uid AND name LIKE :query AND name < :name ORDER BY name ASC LIMIT :lim")
	abstract suspend fun getFollowersBeforeWithQuery(uid: String, query: String, name: String, lim: Int): List<User>

	@Query("SELECT * FROM users WHERE user_followings LIKE :uid AND name > :name ORDER BY name ASC LIMIT :lim")
	abstract suspend fun getFollowersAfter(uid: String, name: String, lim: Int): List<User>

	@Query("SELECT * FROM users WHERE user_followings LIKE :uid AND name LIKE :query AND name > :name ORDER BY name ASC LIMIT :lim")
	abstract suspend fun getFollowersAfterWithQuery(uid: String, query: String, name: String, lim: Int): List<User>

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract suspend fun insertUsers(users: List<User>)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract suspend fun insertUser(user: User)

	@Query("SELECT * FROM users WHERE id = :senderId")
	abstract fun getMessageSender(senderId: String): LiveData<User>

    @Query("DELETE FROM users")
    abstract suspend fun clearUsers()

    @Query("SELECT * FROM users WHERE isCurrentUser = 1")
    abstract suspend fun getCachedUser(): User?

}