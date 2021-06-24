package com.jamid.workconnect.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jamid.workconnect.model.Post

@Dao
abstract class PostDao: BaseDao<Post>() {

    @Query("SELECT * FROM posts WHERE id = :postId")
    abstract fun getPostLive(postId: String): LiveData<Post>

    @Query("SELECT * FROM posts WHERE id = :postId")
    abstract suspend fun getPost(postId: String): Post?

    // Deprecated
    @Query("SELECT * FROM posts WHERE post_local_inFeed = 1 ORDER BY createdAt DESC")
    abstract fun getPagedPosts(): DataSource.Factory<Int, Post>

    // Deprecated
    @Query("SELECT * FROM posts WHERE tags LIKE :tag AND post_local_inFeed = 1 ORDER BY createdAt DESC")
    abstract fun getPagedPostsWithTag(tag: String): DataSource.Factory<Int, Post>

    // Deprecated
    @Query("SELECT * FROM posts WHERE type = :type ORDER BY createdAt DESC")
    abstract fun getPagedProjects(type: String = "Project"): DataSource.Factory<Int, Post>

    // Deprecated
    @Query("SELECT * FROM posts WHERE type = :type AND uid = :uid ORDER BY createdAt DESC")
    abstract fun getUserPagedProjects(uid: String, type: String = "Project"): DataSource.Factory<Int, Post>

    // Deprecated
    @Query("SELECT * FROM posts WHERE post_local_isUserFollowed = 1 AND post_local_inFeed = 1 ORDER BY createdAt DESC")
    abstract fun getPagedPostsFromFollowings(): DataSource.Factory<Int, Post>

    // Deprecated
    @Query("SELECT * FROM posts WHERE type = :type ORDER BY createdAt DESC")
    abstract fun getPagedBlogs(type: String = "Blog"): DataSource.Factory<Int, Post>

    // Deprecated
    @Query("SELECT * FROM posts WHERE type = :type AND uid = :uid ORDER BY createdAt DESC")
    abstract fun getUserPagedBlogs(uid: String, type: String = "Blog"): DataSource.Factory<Int, Post>

    // Deprecated
//    @Query("SELECT * FROM posts WHERE contributors LIKE :uid ORDER BY createdAt DESC")
//    abstract fun getUserCollaborations(uid: String): DataSource.Factory<Int, Post>


    @Query("SELECT * FROM posts")
    abstract suspend fun allPosts(): List<Post>?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPost(post: Post)

    @Query("SELECT * FROM posts WHERE uid = :uid")
    abstract fun getPostForUser(uid: String): List<Post>?

    /*@Query("SELECT * FROM posts WHERE indices LIKE :item AND type = :type")
	abstract fun getSearchedPosts(item: String, type: String): DataSource.Factory<Int, Post>*/

    @Query("SELECT * FROM posts WHERE indices LIKE :item AND type = :type")
    abstract fun getSearchedPosts(item: String, type: String): PagingSource<Int, Post>


	@Query("SELECT * FROM posts WHERE post_local_isSaved = 1 ORDER BY createdAt DESC")
	abstract fun getPagedSavedPosts(): DataSource.Factory<Int, Post>

	@Query("DELETE FROM posts")
	abstract suspend fun clearPosts()

    @Query("SELECT * FROM posts WHERE post_local_inFeed = 1 AND createdAt < :anchorTime ORDER BY createdAt DESC LIMIT :lim")
    abstract fun getPostsBasedOnTime(anchorTime: Long, lim: Int): List<Post>?

    @Query("SELECT * FROM posts WHERE post_local_inFeed = 1 AND createdAt > :endBeforeTime ORDER BY createdAt DESC LIMIT :lim")
    abstract suspend fun getPostsBefore(endBeforeTime: Long, lim: Int): List<Post>

    @Query("SELECT * FROM posts WHERE post_local_inFeed = 1 AND createdAt < :startAfterTime ORDER BY createdAt DESC LIMIT :lim")
    abstract suspend fun getPostsAfter(startAfterTime: Long, lim: Int): List<Post>

    @Query("SELECT * FROM posts WHERE post_local_inFeed = 1 ORDER BY createdAt DESC LIMIT :lim")
    abstract suspend fun getPosts(lim: Int): List<Post>

    @Query("SELECT * FROM posts WHERE (post_local_inFeed = 1 AND post_local_isUserFollowed = 1 AND createdAt > :endBeforeTime) OR uid = :userId ORDER BY createdAt DESC LIMIT :lim")
    abstract suspend fun getPostsBasedOnUserBefore(userId: String, endBeforeTime: Long, lim: Int): List<Post>

    @Query("SELECT * FROM posts WHERE (post_local_inFeed = 1 AND post_local_isUserFollowed = 1 AND createdAt < :startAfterTime) OR uid = :userId ORDER BY createdAt DESC LIMIT :lim")
    abstract suspend fun getPostsBasedOnUserAfter(userId: String, startAfterTime: Long, lim: Int): List<Post>

    @Query("SELECT * FROM posts WHERE type = :type AND uid = :id ORDER BY createdAt DESC LIMIT :lim")
    abstract suspend fun getUserProjects(id: String, type: String, lim: Int): List<Post>

    @Query("SELECT * FROM posts WHERE type = :type AND uid = :id AND createdAt < :time ORDER BY createdAt DESC LIMIT :lim")
    abstract suspend fun getUserProjectsAfter(id: String, type: String, lim: Int, time: Long): List<Post>

    @Query("SELECT * FROM posts WHERE type = :type AND uid = :id AND createdAt > :time ORDER BY createdAt DESC LIMIT :lim")
    abstract suspend fun getUserProjectsBefore(id: String, type: String, lim: Int, time: Long): List<Post>

    @Query("SELECT * FROM posts WHERE contributors LIKE :uid ORDER BY createdAt DESC LIMIT :lim")
    abstract suspend fun getUserCollaborations(uid: String, lim: Int): List<Post>

    @Query("SELECT * FROM posts WHERE contributors LIKE :uid AND createdAt < :time ORDER BY createdAt DESC LIMIT :lim")
    abstract suspend fun getUserCollaborationsAfter(uid: String, time: Long, lim: Int): List<Post>

    @Query("SELECT * FROM posts WHERE contributors LIKE :uid AND createdAt > :time ORDER BY createdAt DESC LIMIT :lim")
    abstract suspend fun getUserCollaborationsBefore(uid: String, time: Long, lim: Int): List<Post>



}