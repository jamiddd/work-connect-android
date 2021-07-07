package com.jamid.workconnect.adapter.paging2

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.*
import com.google.android.material.button.MaterialButton
import com.jamid.workconnect.BLOG
import com.jamid.workconnect.PROJECT
import com.jamid.workconnect.R
import com.jamid.workconnect.adapter.GenericAdapter
import com.jamid.workconnect.convertDpToPx
import com.jamid.workconnect.interfaces.ExploreClickListener
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.TagsHolder
import com.jamid.workconnect.model.User
import com.jamid.workconnect.model.UserMinimal

class ExploreAdapter(
    val items: List<Any>
): RecyclerView.Adapter<ExploreAdapter.VH>() {

    var projectsAdapterHorizontal: PostAdapterHorizontal? = null
    var blogsAdapter: GenericAdapter<Post>? = null
    var userAdapter: GenericAdapter<User>? = null
    var tagsAdapter: GenericAdapter<TagsHolder>? = null

    inner class VH(val view: View): RecyclerView.ViewHolder(view) {

        private val exploreClickListener = view.context as ExploreClickListener

        fun bind(item: Any?) {
            val root = view.findViewById<ConstraintLayout>(R.id.childRecyclerRoot)
            val recycler = view.findViewById<RecyclerView>(R.id.childRecyclerView)
            val header = view.findViewById<TextView>(R.id.recyclerViewHeader)

            val context = view.context

            if (Build.VERSION.SDK_INT <= 27) {
                header.setTextColor(ContextCompat.getColor(context, R.color.black))
            }

            val seeMoreBtn = view.findViewById<MaterialButton>(R.id.seeMoreBtn)

            val helper: SnapHelper = LinearSnapHelper()
            if (item != null) {
                if (item is Post) {
                    if (item.type == PROJECT) {
                        header.text = "Top projects"
                        projectsAdapterHorizontal = PostAdapterHorizontal()
                        recycler.adapter = projectsAdapterHorizontal

                        recycler.apply {
                            onFlingListener = null
                            itemAnimator = null
                            layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.HORIZONTAL, false)
                            helper.attachToRecyclerView(this)
                        }

                        seeMoreBtn.setOnClickListener {
                            exploreClickListener.onSeeMoreClick(Post::class.java, PROJECT)
                        }

                    } else {
                        header.text = "Top blogs"
                        blogsAdapter = GenericAdapter(Post::class.java)
                        recycler.adapter = blogsAdapter

                        recycler.apply {
                            itemAnimator = null
                            layoutManager = LinearLayoutManager(view.context)
                            addItemDecoration(DividerItemDecoration(view.context, DividerItemDecoration.VERTICAL))
                        }

                        seeMoreBtn.setOnClickListener {
                            exploreClickListener.onSeeMoreClick(Post::class.java, BLOG)
                        }

                        recycler.setPadding(0, convertDpToPx(8, view.context), 0, 0)
                    }
                } else if (item is UserMinimal){

                    header.text = "Popular users"

                    userAdapter = GenericAdapter(User::class.java)

                    recycler.apply {
                        itemAnimator = null
                        adapter = userAdapter
                        layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.HORIZONTAL, false)
                    }

                    seeMoreBtn.setOnClickListener {
                        exploreClickListener.onSeeMoreClick(User::class.java)
                    }

                } else if (item is TagsHolder) {
                    header.text = "Tags to follow"

                    tagsAdapter = GenericAdapter(TagsHolder::class.java)
                    recycler.apply {
                        adapter = tagsAdapter
                        itemAnimator = null
                        // orientation doesn't matter as there will be only one child
                        layoutManager = LinearLayoutManager(view.context)
                    }

                    seeMoreBtn.setOnClickListener {
                        exploreClickListener.onSeeMoreClick(TagsHolder::class.java)
                    }
                }

//                OverScrollDecoratorHelper.setUpOverScroll(recycler, OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL)
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
