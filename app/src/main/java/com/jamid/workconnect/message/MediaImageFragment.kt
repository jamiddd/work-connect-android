package com.jamid.workconnect.message

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.paging.PagedList
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.firebase.ui.firestore.paging.FirestorePagingAdapter
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.*
import com.jamid.workconnect.databinding.FragmentMediaImageBinding
import com.jamid.workconnect.model.SimpleMedia

class MediaImageFragment : Fragment(R.layout.fragment_media_image) {

    private lateinit var binding: FragmentMediaImageBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMediaImageBinding.bind(view)

        val chatChannelId = arguments?.getString("chatChannelId") ?: return
        val query = Firebase.firestore
            .collection(CHAT_CHANNELS)
            .document(chatChannelId)
            .collection(MEDIA)
            .whereEqualTo(TYPE, IMAGE)
            .orderBy(CREATED_AT, Query.Direction.DESCENDING)

        val config = PagedList.Config.Builder()
            .setPageSize(25)
            .setPrefetchDistance(10)
            .setInitialLoadSizeHint(30)
            .setEnablePlaceholders(false)
            .build()

        val option = FirestorePagingOptions.Builder<SimpleMedia>()
            .setQuery(query, config, SimpleMedia::class.java)
            .setLifecycleOwner(viewLifecycleOwner)
            .build()

        val gridImageAdapter = GridImageAdapter(option)
        val manager = GridLayoutManager(requireContext(), 3)
        binding.mediaImageRecycler.apply {
            adapter = gridImageAdapter
            layoutManager = manager
        }

    }

    inner class GridImageAdapter(options: FirestorePagingOptions<SimpleMedia>): FirestorePagingAdapter<SimpleMedia, GridImageAdapter.GridImageViewHolder>(options){
        inner class GridImageViewHolder(val view: View): RecyclerView.ViewHolder(view) {
            fun bind(media: SimpleMedia?) {
                if (media != null) {
                    val imageView = view.findViewById<SimpleDraweeView>(R.id.square_image_grid_item)
                    val width = getWindowWidth() / 3
                    imageView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, width)
                    imageView.setImageURI(media.mediaLocation)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridImageViewHolder {
            return GridImageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.square_image_grid, parent, false))
        }

        override fun onBindViewHolder(
            holder: GridImageViewHolder,
            position: Int,
            model: SimpleMedia
        ) {
            holder.bind(getItem(position)?.toObject(model::class.java))
        }
    }

    companion object {

        @JvmStatic
        fun newInstance(chatChannelId: String) = MediaImageFragment().apply {
            arguments = Bundle().apply {
                putString("chatChannelId", chatChannelId)
            }
        }
    }
}