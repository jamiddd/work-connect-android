package com.jamid.workconnect.adapter.paging2

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.jamid.workconnect.PROJECT
import com.jamid.workconnect.R
import com.jamid.workconnect.adapter.GenericAdapter
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.User
import com.jamid.workconnect.model.UserMinimal
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class ExploreAdapter(
    val items: List<Any>,
    val actContext: Context
): RecyclerView.Adapter<ExploreAdapter.VH>() {

    var projectsAdapterHorizontal: PostAdapterHorizontal? = null
    var blogsAdapterHorizontal: PostAdapterHorizontal? = null
    var userAdapter: GenericAdapter<User>? = null

    inner class VH(val view: View): RecyclerView.ViewHolder(view) {
        fun bind(item: Any?) {
            val root = view.findViewById<ConstraintLayout>(R.id.childRecyclerRoot)
            val recycler = view.findViewById<RecyclerView>(R.id.childRecyclerView)
            val header = view.findViewById<TextView>(R.id.recyclerViewHeader)

            if (Build.VERSION.SDK_INT <= 27) {
                header.setTextColor(ContextCompat.getColor(view.context, R.color.black))
            }

            val helper: SnapHelper = LinearSnapHelper()
            if (item != null) {
                if (item is Post) {
                    if (item.type == PROJECT) {
                        header.text = "Top projects"
                        projectsAdapterHorizontal = PostAdapterHorizontal(actContext)
                        recycler.adapter = projectsAdapterHorizontal
                    } else {
                        header.text = "Top blogs"
                        blogsAdapterHorizontal = PostAdapterHorizontal(actContext)
                        recycler.adapter = blogsAdapterHorizontal
                    }

                    recycler.apply {
                        onFlingListener = null
                        layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.HORIZONTAL, false)
                        helper.attachToRecyclerView(this)
                    }
                } else if (item is UserMinimal){

                    header.text = "Popular users"

                    userAdapter = GenericAdapter(User::class.java)

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
