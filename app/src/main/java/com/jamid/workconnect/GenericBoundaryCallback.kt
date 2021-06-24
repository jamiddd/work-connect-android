package com.jamid.workconnect

import android.util.Log
import androidx.paging.PagedList
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.jamid.workconnect.data.MainRepository

class GenericBoundaryCallback<T : Any>(
    private val lim: Long,
    private val initialQuery: Query,
    private val repo: MainRepository,
    private val clazz: Class<T>,
    private val extras: Map<String, Any?>? = null
): PagedList.BoundaryCallback<T>() {

    private var firstSnapshot: DocumentSnapshot? = null
    private var lastSnapshot: DocumentSnapshot? = null
    private var isEnd = false

    companion object {
        const val TAG = "BoundaryCallback"

        fun <T: Any> newInstance(lim: Long, initialQuery: Query, repo: MainRepository, clazz: Class<T>) =
            GenericBoundaryCallback(lim, initialQuery, repo, clazz)
    }

    private fun fetchItemsAndCache(query: Query, extras: Map<String, Any?>?) {
        Log.d(BUG_TAG, "Before fetching posts .. ")
        repo.getItems(lim, query, clazz, extras) { doc1, doc2, e ->
            Log.d(BUG_TAG, "After fetching posts .. ")
            firstSnapshot = doc1
            lastSnapshot = doc2
            isEnd = e
        }
    }

    override fun onZeroItemsLoaded() {
        super.onZeroItemsLoaded()
        fetchItemsAndCache(initialQuery.limit(lim), extras)
    }

    override fun onItemAtEndLoaded(itemAtEnd: T) {
        super.onItemAtEndLoaded(itemAtEnd)
        if (!isEnd) {
            if (lastSnapshot != null) {
                fetchItemsAndCache(initialQuery.startAfter(lastSnapshot).limit(lim), extras)
            } else {
                repo.getSnapshot(itemAtEnd, clazz) { doc ->
                    lastSnapshot = doc
                    fetchItemsAndCache(initialQuery.startAfter(lastSnapshot).limit(lim), extras)
                }
            }
        }
    }

    override fun onItemAtFrontLoaded(itemAtFront: T) {
        super.onItemAtFrontLoaded(itemAtFront)
        if (!isEnd) {
            if (firstSnapshot != null) {
                fetchItemsAndCache(initialQuery.endAt(firstSnapshot).limit(lim), extras)
            } else {
                repo.getSnapshot(itemAtFront, clazz) { doc ->
                    firstSnapshot = doc
                    fetchItemsAndCache(initialQuery.endAt(firstSnapshot).limit(lim), extras)
                }
            }
        }
    }

}