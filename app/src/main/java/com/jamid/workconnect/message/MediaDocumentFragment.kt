package com.jamid.workconnect.message

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.paging.FirestorePagingAdapter
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.jamid.workconnect.*
import com.jamid.workconnect.databinding.FragmentMediaDocumentBinding
import com.jamid.workconnect.interfaces.MessageItemClickListener
import com.jamid.workconnect.model.SimpleMedia
import com.jamid.workconnect.model.SimpleMessage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MediaDocumentFragment : Fragment(R.layout.fragment_media_document) {

    private lateinit var binding: FragmentMediaDocumentBinding
    private val db = Firebase.firestore
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var chatChannelId: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMediaDocumentBinding.bind(view)

        chatChannelId = arguments?.getString("chatChannelId") ?: return

        val query = Firebase.firestore
            .collection(CHAT_CHANNELS)
            .document(chatChannelId)
            .collection(MEDIA)
            .whereEqualTo(TYPE, DOCUMENT)
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

        binding.mediaDocumentRecycler.apply {
            adapter = VerticalDocumentAdapter(option)
            layoutManager = LinearLayoutManager(requireContext())
        }

    }

    inner class VerticalDocumentAdapter(options: FirestorePagingOptions<SimpleMedia>): FirestorePagingAdapter<SimpleMedia, VerticalDocumentAdapter.VerticalDocumentViewHolder>(options) {

        val messageItemClickListener = (requireActivity() as MainActivity) as MessageItemClickListener

        inner class VerticalDocumentViewHolder(val view: View): RecyclerView.ViewHolder(view) {
            fun bind(media: SimpleMedia?) {
                if (media != null) {
                    val name = view.findViewById<TextView>(R.id.doc_name_text)
                    val meta = view.findViewById<TextView>(R.id.doc_meta)

                    val words = media.mediaLocation.split("%2F")
                    val fullName = words.last().split('?')[0]
                    val nameText = fullName.split('_').last()

                    name.text = nameText

                    db.collection(CHAT_CHANNELS).document(chatChannelId).collection(MESSAGES).document(media.id).get()
                        .addOnSuccessListener {
                            it?.let {
                                if (it.exists()) {
                                    val message = it.toObject(SimpleMessage::class.java)
                                    if (message != null) {

                                        lifecycleScope.launch {
                                            val sender = viewModel.getContributor(message.senderId)
                                            if (sender != null) {
                                                val metaText = "Sent by ${sender.name} ● " + SimpleDateFormat("hh:mm a dd/MM/yy", Locale.UK).format(media.createdAt)
                                                meta.text = metaText

                                                val med = viewModel.checkIfFileDownloaded(message.messageId)
                                                if (med != null) {
                                                    view.setOnClickListener {
                                                        messageItemClickListener.onDocumentClick(message, fullName, med.size)
                                                    }
                                                } else {
                                                    val childRef = "${message.chatChannelId}/documents/messages/$fullName"
                                                    val objectRef = Firebase.storage.reference.child(childRef)

                                                    objectRef.metadata.addOnSuccessListener {
                                                        view.setOnClickListener { v ->
                                                            messageItemClickListener.onDocumentClick(message, nameText, it.sizeBytes)
                                                        }
                                                    }.addOnFailureListener { exc ->
                                                        Log.d("SimpleAdapter", exc.localizedMessage.toString())
                                                        viewModel.setCurrentError(exc)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }.addOnFailureListener {
                            viewModel.setCurrentError(it)
                        }

                }
            }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): VerticalDocumentViewHolder {
            return VerticalDocumentViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.vertical_document_layout, parent, false))
        }

        override fun onBindViewHolder(
            holder: VerticalDocumentViewHolder,
            position: Int,
            model: SimpleMedia
        ) {
            holder.bind(getItem(position)?.toObject(model::class.java))
        }
    }

    companion object {

        @JvmStatic
        fun newInstance(chatChannelId: String) = MediaDocumentFragment().apply {
            arguments = Bundle().apply {
                putString("chatChannelId", chatChannelId)
            }
        }
    }
}