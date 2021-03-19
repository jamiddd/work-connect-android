package com.jamid.workconnect.message

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.paging.PagedList
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.*
import com.jamid.workconnect.adapter.ChatChannelAdapter
import com.jamid.workconnect.databinding.FragmentChatChannelBinding
import com.jamid.workconnect.model.ChatChannel
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class ChatChannelFragment : Fragment(R.layout.fragment_chat_channel) {

    private lateinit var binding: FragmentChatChannelBinding
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var chatChannelAdapter: ChatChannelAdapter
    private lateinit var activity: MainActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = requireActivity() as MainActivity
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentChatChannelBinding.bind(view)
        var query = Firebase.firestore.collection(CHAT_CHANNELS)
            .whereArrayContains(CONTRIBUTORS_LIST, " ")
            .orderBy(UPDATED_AT, Query.Direction.DESCENDING)

        val config = PagedList.Config.Builder().setPageSize(10).setPrefetchDistance(5).setEnablePlaceholders(false).build()
        var options = FirestorePagingOptions.Builder<ChatChannel>()
            .setLifecycleOwner(viewLifecycleOwner)
            .setDiffCallback(ChatChannelComparator())
            .setQuery(query, config, ChatChannel::class.java)
            .build()

        chatChannelAdapter = ChatChannelAdapter(options, activity)

        viewModel.windowInsets.observe(viewLifecycleOwner) { (status, nav) ->
            binding.chatChannelRecycler.setPadding(0, status + convertDpToPx(56), 0, nav + convertDpToPx(64))
        }

        binding.chatChannelRecycler.apply {
            adapter = chatChannelAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }

        OverScrollDecoratorHelper.setUpOverScroll(binding.chatChannelRecycler, OverScrollDecoratorHelper.ORIENTATION_VERTICAL)
       /* binding.chatChannelRefresher.setOnRefreshListener {
            chatChannelAdapter.refresh()
            binding.chatChannelRefresher.isRefreshing = false
        }*/

        viewModel.user.observe(viewLifecycleOwner) {
            if (it != null) {
                query = Firebase.firestore.collection(CHAT_CHANNELS)
                    .whereArrayContains(CONTRIBUTORS_LIST, it.id)
                    .orderBy(UPDATED_AT, Query.Direction.DESCENDING)

                options = FirestorePagingOptions.Builder<ChatChannel>()
                    .setLifecycleOwner(viewLifecycleOwner)
                    .setDiffCallback(ChatChannelComparator())
                    .setQuery(query, config, ChatChannel::class.java)
                    .build()

                chatChannelAdapter.updateOptions(options)

            }
        }

        viewModel.hasConversationsUpdated.observe(viewLifecycleOwner) {
            if (it != null && it) {
                chatChannelAdapter.refresh()
                viewModel.hasConversationsUpdated.postValue(false)
            }
        }

    }

    companion object {

        @JvmStatic
        fun newInstance() = ChatChannelFragment()
    }

}