package com.jamid.workconnect.message

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.workconnect.InsetControlFragment
import com.jamid.workconnect.R
import com.jamid.workconnect.adapter.GenericAdapter
import com.jamid.workconnect.convertDpToPx
import com.jamid.workconnect.databinding.FragmentChatChannelBinding
import com.jamid.workconnect.model.ChatChannel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class ChatChannelFragment : InsetControlFragment(R.layout.fragment_chat_channel) {

    private lateinit var binding: FragmentChatChannelBinding
    private lateinit var channelAdapter: GenericAdapter<ChatChannel>
    private var job: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentChatChannelBinding.bind(view)

        setInsetView(binding.chatChannelRecycler, mapOf(INSET_TOP to 56, INSET_BOTTOM to 64))
//        setRefreshListener(this)

        OverScrollDecoratorHelper.setUpOverScroll(binding.noChannelsLayoutScroll)

        binding.noChannelsExploreBtn.setOnClickListener {
            activity.mainBinding.bottomNav.selectedItemId = R.id.explore_navigation
        }

        viewModel.miniUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                channelAdapter = GenericAdapter(ChatChannel::class.java)

                binding.chatChannelRecycler.apply {
                    adapter = channelAdapter
                    layoutManager = LinearLayoutManager(activity)
                    addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
                }

                binding.chatChannelRefresher.setProgressViewOffset(false, 0, viewModel.windowInsets.value!!.first + convertDpToPx(64))
                binding.chatChannelRefresher.setSlingshotDistance(convertDpToPx(54))
                binding.chatChannelRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.blue_500))
//                setOverScrollView(binding.chatChannelRecycler)

                viewModel.chatChannelsLiveData.observe(viewLifecycleOwner) { list ->
                    if (list.isNotEmpty()) {
                        job?.cancel()
                        binding.chatChannelRefresher.isRefreshing = false
                        binding.noChannelsLayoutScroll.visibility = View.GONE
                        binding.chatChannelRecycler.visibility = View.VISIBLE
                        channelAdapter.submitList(list)
                    } else {
                        viewModel.getChatChannels()
                        binding.chatChannelRefresher.isRefreshing = true
                    }
                }
            } else {
                binding.chatChannelRecycler.visibility = View.GONE
                binding.noChannelsLayoutScroll.visibility = View.VISIBLE
            }
        }

        if (viewModel.user.value == null) {
            binding.chatChannelRecycler.visibility = View.GONE
            binding.noChannelsLayoutScroll.visibility = View.VISIBLE
        }

        binding.chatChannelRefresher.setOnRefreshListener {

            viewLifecycleOwner.lifecycleScope.launchWhenCreated {
                delay(3000)
                binding.chatChannelRefresher.isRefreshing = false
            }
        }

    }

    companion object {

        const val TAG = "ChatChannelFragment"

        @JvmStatic
        fun newInstance() = ChatChannelFragment()
    }

   /* override fun onRefreshStart() {
        viewModel.getChatChannels()
        lifecycleScope.launch {
            delay(2000)
            setOverScrollView(binding.chatChannelRecycler)
        }
    }*/

}