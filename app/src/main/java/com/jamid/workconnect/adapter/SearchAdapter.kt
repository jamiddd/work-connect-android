package com.jamid.workconnect.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.paging.PagedList
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.firebase.ui.firestore.paging.FirestorePagingAdapter
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.google.firebase.firestore.DocumentSnapshot
import com.jamid.workconnect.R
import com.jamid.workconnect.interfaces.SearchItemClickListener
import com.jamid.workconnect.model.SearchResult

class SearchAdapter(
    options: FirestorePagingOptions<SearchResult>,
    val searchItemClickListener: SearchItemClickListener
): FirestorePagingAdapter<SearchResult, SearchAdapter.SearchItemViewHolder>(options){

    override fun onCurrentListChanged(
        previousList: PagedList<DocumentSnapshot>?,
        currentList: PagedList<DocumentSnapshot>?
    ) {
        super.onCurrentListChanged(previousList, currentList)
        Log.d("SearchAdapter", currentList?.size.toString())
    }

    inner class SearchItemViewHolder(val item: View): RecyclerView.ViewHolder(item) {
        fun bind(searchResult: SearchResult?) {
            if (searchResult != null) {
                item.apply {
                    val searchImg = findViewById<SimpleDraweeView>(R.id.search_img)
                    val searchTextResult = findViewById<TextView>(R.id.search_text_result)
                    val searchAddAction = findViewById<Button>(R.id.search_result_action)
                    if (searchResult.type != null) {
                        // post
                        if (searchResult.type == "Project") {
                            // project
                            searchImg.setImageURI(searchResult.img)
                            searchImg.visibility = View.VISIBLE
                        } else {
                            // blog
                        }
                    } else {
                        // user
                        searchImg.setImageURI(searchResult.img)
                        searchImg.visibility = View.VISIBLE
                    }
                    searchTextResult.text = searchResult.title

                    setOnClickListener {
                        searchItemClickListener.onSearchItemClick(searchResult.id, searchResult.type)
                    }

                    searchAddAction.setOnClickListener {
                        searchItemClickListener.onSearchAdded(searchResult.title)
                    }
                }
            }
        }
    }

    override fun onError(e: Exception) {
        super.onError(e)
        Log.e("SearchAdapter", e.localizedMessage.toString())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchItemViewHolder {
        return SearchItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.search_item_layout, parent, false))
    }

    override fun onBindViewHolder(
        holder: SearchItemViewHolder,
        position: Int,
        model: SearchResult
    ) {
        holder.bind(getItem(position)?.toObject(model::class.java))
    }

}