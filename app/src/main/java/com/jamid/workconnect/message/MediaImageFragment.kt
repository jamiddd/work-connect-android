package com.jamid.workconnect.message

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.SharedElementCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.jamid.workconnect.IMAGE
import com.jamid.workconnect.InsetControlFragment
import com.jamid.workconnect.MainViewModel
import com.jamid.workconnect.R
import com.jamid.workconnect.adapter.paging2.MessageAdapter
import com.jamid.workconnect.databinding.FragmentMediaImageBinding
import com.jamid.workconnect.model.ChatChannel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MediaImageFragment : Fragment(R.layout.fragment_media_image) {

    private lateinit var binding: FragmentMediaImageBinding
    private val viewModel: MainViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentMediaImageBinding.bind(view)

        val chatChannel = arguments?.getParcelable<ChatChannel>(ARG_CHAT_CHANNEL) ?: return
        initImageAdapter(chatChannel)

    }

    private fun initImageAdapter(chatChannel: ChatChannel) {
        val messageAdapter = MessageAdapter(viewModel)

        binding.mediaImageRecycler.apply {
            adapter = messageAdapter
            layoutManager = GridLayoutManager(activity, 3)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val images = viewModel.localMessages(IMAGE, chatChannel.chatChannelId)
            messageAdapter.submitList(images)
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