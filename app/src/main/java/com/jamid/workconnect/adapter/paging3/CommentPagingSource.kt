package com.jamid.workconnect.adapter.paging3

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.COMMENTS
import com.jamid.workconnect.COMMENT_CHANNELS
import com.jamid.workconnect.data.MainRepository
import com.jamid.workconnect.model.PageKey
import com.jamid.workconnect.model.Result
import com.jamid.workconnect.model.SimpleComment

class CommentPagingSource(val commentChannelId: String, private val repo: MainRepository): PagingSource<PageKey, SimpleComment>() {
    override fun getRefreshKey(state: PagingState<PageKey, SimpleComment>): PageKey? {
        return state.anchorPosition?.let {
            val page = state.closestPageToPosition(it)
            page?.prevKey ?: page?.nextKey
        }
    }

    override suspend fun load(params: LoadParams<PageKey>): LoadResult<PageKey, SimpleComment> {
        val key = params.key
        val baseQuery = Firebase.firestore.collection(COMMENT_CHANNELS).document(commentChannelId).collection(
            COMMENTS)

        return when (val commentsSnapshotResult = repo.firebaseUtility.getComments(baseQuery, key?.endBefore, key?.startAfter)) {
            is Result.Success -> {
                val commentsSnapshot = commentsSnapshotResult.data
                if (commentsSnapshot.isEmpty) {
                    return LoadResult.Page(emptyList(), null, null)
                }

                val comments = commentsSnapshot.toObjects(SimpleComment::class.java)

                val currentUser = repo.currentLocalUser.value
                if (currentUser != null) {
                    for (comment in comments) {
                        comment.isLiked = currentUser.userPrivate.likedComments.contains(comment.commentId)
                    }
                }

                val firstSnapshot = commentsSnapshot.first()
                val lastSnapshot = commentsSnapshot.last()

                repo.mapOfDocumentSnapshots[firstSnapshot.id] = firstSnapshot
                repo.mapOfDocumentSnapshots[lastSnapshot.id] = lastSnapshot

                return LoadResult.Page(comments, PageKey(null, firstSnapshot), PageKey(lastSnapshot, null))
            }
            is Result.Error -> {
                LoadResult.Error(commentsSnapshotResult.exception)
            }
        }

    }
}