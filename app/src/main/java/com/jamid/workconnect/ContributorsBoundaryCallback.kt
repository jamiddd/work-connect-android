package com.jamid.workconnect

import androidx.paging.PagedList
import com.google.firebase.firestore.DocumentSnapshot
import com.jamid.workconnect.data.MainRepository
import com.jamid.workconnect.data.MessageBoundaryCallback
import com.jamid.workconnect.model.ChatChannelContributor
import kotlinx.coroutines.CoroutineScope

class ContributorsBoundaryCallback(private val channelId: String, private val repo: MainRepository): PagedList.BoundaryCallback<ChatChannelContributor>() {

    private var hasReachedEndFromStart = false
    private var hasReachedEndFromEnd = false
    private var firstDoc: DocumentSnapshot? = null
    private var lastDoc: DocumentSnapshot? = null

    companion object {

        const val PAGE_SIZE: Long = 10
        const val TAG = "ContributorsBoundary"

        fun newInstance(chatChannelId: String, repo: MainRepository, scope: CoroutineScope)
            = MessageBoundaryCallback(chatChannelId, repo, scope)

    }

    override fun onZeroItemsLoaded() {
        super.onZeroItemsLoaded()
        repo.getChannelContributors(channelId, PAGE_SIZE) {
            if (!it.isEmpty) {
                hasReachedEndFromEnd = it.size() < PAGE_SIZE
                hasReachedEndFromStart = it.size() < PAGE_SIZE
                firstDoc = it.first()
                lastDoc = it.last()
            }
        }
    }

    override fun onItemAtFrontLoaded(itemAtFront: ChatChannelContributor) {
        super.onItemAtFrontLoaded(itemAtFront)
        val doc = firstDoc
        if (doc == null) {
            if (!hasReachedEndFromStart) {
                repo.getContributorSnapshot(channelId, itemAtFront.id) {
                    getContributorsBefore(it)
                }
            }
        } else {
            getContributorsBefore(doc)
        }
    }

    private fun getContributorsBefore(documentSnapshot: DocumentSnapshot) {
        repo.getChannelContributors(channelId, PAGE_SIZE, documentSnapshot, true) {
            if (!it.isEmpty) {
                hasReachedEndFromStart = it.size() < PAGE_SIZE
                firstDoc = it.first()
            }
        }
    }

    private fun getContributorsAfter(documentSnapshot: DocumentSnapshot) {
        repo.getChannelContributors(channelId, PAGE_SIZE, documentSnapshot) {
            if (!it.isEmpty) {
                hasReachedEndFromEnd = it.size() < PAGE_SIZE
                lastDoc = it.last()
            }
        }
    }

    override fun onItemAtEndLoaded(itemAtEnd: ChatChannelContributor) {
        super.onItemAtEndLoaded(itemAtEnd)
        val doc = lastDoc
        if (doc == null) {
            if (!hasReachedEndFromEnd) {
                repo.getContributorSnapshot(channelId, itemAtEnd.id) {
                    getContributorsAfter(it)
                }
            }
        } else {
            getContributorsAfter(doc)
        }
    }
}