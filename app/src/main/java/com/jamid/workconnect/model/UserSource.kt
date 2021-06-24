package com.jamid.workconnect.model

import com.google.firebase.firestore.Query

sealed class UserSource {
	data class Contributor(val query: Query, val chatChannel: ChatChannel): UserSource()
	data class Follower(val query: Query, val searchQuery: String? = null, val otherUser: User): UserSource()
	data class Following(val query: Query, val searchQuery: String? = null, val otherUser: User): UserSource()
	data class Search(val query: Query, val searchQuery: String? = null): UserSource()
	data class Acceptance(val query: Query, val post: Post): UserSource()
	data class Random(val query: Query): UserSource()
}
