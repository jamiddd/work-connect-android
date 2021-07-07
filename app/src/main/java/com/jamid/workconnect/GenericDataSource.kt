package com.jamid.workconnect

import android.util.Log
import androidx.paging.DataSource
import androidx.paging.ItemKeyedDataSource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.data.MainRepository
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.User

class GenericDataSource<T: Any>(private val lim: Long, private val initialQuery: Query, private val repository: MainRepository, private val clazz: Class<T>): ItemKeyedDataSource<String, T>() {

    private var lastSnapshot: DocumentSnapshot? = null
    private var firstSnapshot: DocumentSnapshot? = null

    private val db = Firebase.firestore

    init {
        Log.d("GenericDataSource", "Data Source started with initial query - $initialQuery")
    }

    override fun getKey(item: T): String {
        return when (clazz) {
            User::class.java -> {
                val user = item as User
                user.id
            }
            Post::class.java -> {
                val post = item as Post
                post.id
            }
            else -> throw ClassCastException("Class not found in the given options in Generic Data Source.")
        }
    }

    override fun loadInitial(params: LoadInitialParams<String>, callback: LoadInitialCallback<T>) {
        Log.d("GenericDataSource", "OnLoadInitial")
        /*repository.getItemsWithoutCaching(initialQuery.limit(lim)) {
            firstSnapshot = it.firstOrNull()
            lastSnapshot = it.lastOrNull()
            Log.d("GenericDataSource", "${firstSnapshot?.id}, ${lastSnapshot?.id}")
            callback.onResult(it.toObjects(clazz))
        }*/
    }

    override fun loadAfter(params: LoadParams<String>, callback: LoadCallback<T>) {
        if (lastSnapshot != null) {
            /*repository.getItemsWithoutCaching(initialQuery.startAfter(lastSnapshot).limit(lim)) {
                firstSnapshot = it.firstOrNull()
                lastSnapshot = it.lastOrNull()
                callback.onResult(it.toObjects(clazz))
            }*/
        } else {
            val query = when (clazz) {
                User::class.java -> {
                    db.collection(USERS).document(params.key)
                }
                Post::class.java -> {
                    db.collection(POSTS).document(params.key)
                }
                else -> throw ClassCastException("Class not found in the given options in Generic Data Source.")
            }
            /*repository.getSnapshot(query) { doc ->
                repository.getItemsWithoutCaching(initialQuery.startAfter(doc).limit(lim)) {
                    firstSnapshot = it.firstOrNull()
                    lastSnapshot = it.lastOrNull()
                    callback.onResult(it.toObjects(clazz))
                }
            }*/
        }
    }

    override fun loadBefore(params: LoadParams<String>, callback: LoadCallback<T>) {
        //
    }

}

class GenericDataSourceFactory<T: Any>(private val lim: Long, private val initialQuery: Query, private val repository: MainRepository, private val clazz: Class<T>): DataSource.Factory<String, T>() {
    override fun create(): DataSource<String, T> {
        return GenericDataSource(lim, initialQuery, repository, clazz)
    }
}