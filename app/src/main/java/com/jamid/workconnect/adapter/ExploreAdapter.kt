package com.jamid.workconnect.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.*
import com.jamid.workconnect.interfaces.PostItemClickListener
import com.jamid.workconnect.interfaces.UserItemClickListener
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.UserMinimal
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class ExploreAdapter(
    val items: List<Any>,
    val lifecycleOwner: LifecycleOwner,
    val viewModel: MainViewModel,
    val postItemClickListener: PostItemClickListener,
    val userItemClickListener: UserItemClickListener
): RecyclerView.Adapter<ExploreAdapter.VH>() {

    private val db = Firebase.firestore

    inner class VH(val view: View): RecyclerView.ViewHolder(view) {
        fun bind(item: Any?) {
            val root = view.findViewById<ConstraintLayout>(R.id.childRecyclerRoot)
            val recycler = view.findViewById<RecyclerView>(R.id.childRecyclerView)
            val header = view.findViewById<TextView>(R.id.recyclerViewHeader)
            val helper: SnapHelper = LinearSnapHelper()
            if (item != null) {
                val config = PagedList.Config.Builder()
                    .setPrefetchDistance(5)
                    .setPageSize(10)
                    .setEnablePlaceholders(false)
                    .setInitialLoadSizeHint(15)
                    .build()

                if (item is Post) {
                    Log.d("ExploreAdapter", "Item is post")
                    val query = db.collection(POSTS)
                        .whereEqualTo(TYPE, item.type)
                        .orderBy(CREATED_AT, Query.Direction.DESCENDING)

                    val option = FirestorePagingOptions.Builder<Post>()
                        .setQuery(query, config, Post::class.java)
                        .setLifecycleOwner(lifecycleOwner)
                        .build()

                    val postAdapterHorizontal = FirebasePostAdapterHorizontal(option, viewModel, lifecycleOwner, postItemClickListener)

                    recycler.apply {
                        adapter = postAdapterHorizontal
                        onFlingListener = null
                        layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.HORIZONTAL, false)
                        helper.attachToRecyclerView(this)
                    }

                    if (item.type == PROJECT) {
                        header.text = "Top projects"
                    } else {
                        header.text = "Top blogs"
                    }
                } else if (item is UserMinimal){
                    Log.d("ExploreAdapter", "Item is User")
                    header.text = "Popular users"

                    val query = Firebase.firestore.collection(USER_MINIMALS)

                    val option = FirestorePagingOptions.Builder<UserMinimal>()
                        .setQuery(query, config, UserMinimal::class.java)
                        .setLifecycleOwner(lifecycleOwner)
                        .build()

                    val userAdapter = FirebaseHorizontalUsersAdapter(option, userItemClickListener)

                    recycler.apply {
                        adapter = userAdapter
                        layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.HORIZONTAL, false)
                    }
                }

                OverScrollDecoratorHelper.setUpOverScroll(recycler, OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(LayoutInflater.from(parent.context).inflate(R.layout.nested_child_recycler_item, parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

}
