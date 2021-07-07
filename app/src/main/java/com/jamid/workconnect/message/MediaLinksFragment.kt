package com.jamid.workconnect.message

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.workconnect.InsetControlFragment
import com.jamid.workconnect.LINKS
import com.jamid.workconnect.R
import com.jamid.workconnect.adapter.paging2.MessageAdapter
import com.jamid.workconnect.databinding.FragmentMediaLinksBinding
import com.jamid.workconnect.model.ChatChannel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class MediaLinksFragment : InsetControlFragment(R.layout.fragment_media_links) {

    private lateinit var binding: FragmentMediaLinksBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMediaLinksBinding.bind(view)
        setInsetView(binding.linksRecycler, mapOf(INSET_TOP to 0, INSET_BOTTOM to 0))
        val chatChannel = arguments?.getParcelable<ChatChannel>(ARG_CHAT_CHANNEL) ?: return
        var job: Job? = null
        val linksAdapter = MessageAdapter(viewModel)

        OverScrollDecoratorHelper.setUpOverScroll(binding.noLinksScrollLayout)

        binding.linksRecycler.apply {
            adapter = linksAdapter
            layoutManager = LinearLayoutManager(activity)
            addItemDecoration(DividerItemDecoration(activity, LinearLayoutManager.VERTICAL))
        }

        OverScrollDecoratorHelper.setUpOverScroll(binding.linksRecycler, OverScrollDecoratorHelper.ORIENTATION_VERTICAL)

        /*viewModel.messagesByType(chatChannel.chatChannelId, LINKS).observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                job?.cancel()
//                activity.mainBinding.primaryProgressBar.visibility = View.GONE
                binding.noLinksScrollLayout.visibility = View.GONE
                binding.linksRecycler.visibility = View.VISIBLE
                linksAdapter.submitList(it)
            } else {
//                activity.mainBinding.primaryProgressBar.visibility = View.VISIBLE
                job = lifecycleScope.launch {
                    delay(2000)
//                    activity.mainBinding.primaryProgressBar.visibility = View.GONE
                    binding.noLinksScrollLayout.visibility = View.VISIBLE
                    binding.linksRecycler.visibility = View.GONE
                }
            }
        }*/

    }

    companion object {

        const val ARG_CHAT_CHANNEL = "ARG_CHAT_CHANNEL"

        @JvmStatic
        fun newInstance(chatChannel: ChatChannel) = MediaLinksFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_CHAT_CHANNEL, chatChannel)
            }
        }
    }
}