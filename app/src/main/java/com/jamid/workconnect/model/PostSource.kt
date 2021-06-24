package com.jamid.workconnect.model

import com.google.firebase.firestore.Query

sealed class PostSource {

	val defaultPageSize: Long = 20

	// for home feed
	data class FeedRandom(val query: Query) : PostSource()
	data class FeedWithFollowings(val query: Query, val currentUser: User) : PostSource()
	data class FeedWithTags(val query: Query, val tags: List<String>) : PostSource()


	// for profile fragments
	data class FeedWithOtherUserAndType(val query: Query, val otherUser: User, val type: String) : PostSource()
	data class FeedWithOtherUserCollaborations(val query: Query, val otherUser: User) : PostSource()


	// for explore
	data class FeedTopPostsWithType(val query: Query, val type: String): PostSource()

	data class Search(val query: Query, val type: String): PostSource()
	data class TagPosts(val query: Query, val tag: String): PostSource()
}
