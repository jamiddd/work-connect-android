package com.jamid.workconnect.message

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.jamid.workconnect.IMAGE
import com.jamid.workconnect.InsetControlFragment
import com.jamid.workconnect.R
import com.jamid.workconnect.adapter.paging2.LocalMessageAdapter
import com.jamid.workconnect.databinding.FragmentMediaImageBinding
import com.jamid.workconnect.model.ChatChannel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class MediaImageFragment : InsetControlFragment(R.layout.fragment_media_image) {

    private lateinit var binding: FragmentMediaImageBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMediaImageBinding.bind(view)
        setInsetView(binding.mediaImageRecycler, mapOf(INSET_TOP to 104, INSET_BOTTOM to 0))
        val chatChannel = arguments?.getParcelable<ChatChannel>(ARG_CHAT_CHANNEL) ?: return

        var job: Job? = null
        val imageMessageAdapter = LocalMessageAdapter(activity, viewModel)

        val manager = GridLayoutManager(activity, 3)
        binding.mediaImageRecycler.apply {
            adapter = imageMessageAdapter
            layoutManager = manager
        }
        OverScrollDecoratorHelper.setUpOverScroll(binding.mediaImageRecycler, OverScrollDecoratorHelper.ORIENTATION_VERTICAL)
        OverScrollDecoratorHelper.setUpOverScroll(binding.noImageMessageScrollLayout)

        viewModel.messagesByType(chatChannel.chatChannelId, IMAGE).observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                job?.cancel()
                activity.mainBinding.primaryProgressBar.visibility = View.GONE
                binding.noImageMessageScrollLayout.visibility = View.GONE
                binding.mediaImageRecycler.visibility = View.VISIBLE
                imageMessageAdapter.submitList(it)
            } else {
                activity.mainBinding.primaryProgressBar.visibility = View.VISIBLE
                job = lifecycleScope.launch {
                    delay(2000)
                    activity.mainBinding.primaryProgressBar.visibility = View.GONE
                    binding.noImageMessageScrollLayout.visibility = View.VISIBLE
                    binding.mediaImageRecycler.visibility = View.GONE
                }
            }
        }

    }

    companion object {

        const val ARG_CHAT_CHANNEL = "ARG_CHAT_CHANNEL"

        @JvmStatic
        fun newInstance(chatChannel: ChatChannel) = MediaImageFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_CHAT_CHANNEL, chatChannel)
            }
        }
    }
}