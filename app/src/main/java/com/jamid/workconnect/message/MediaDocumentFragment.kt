package com.jamid.workconnect.message

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.workconnect.DOCUMENT
import com.jamid.workconnect.InsetControlFragment
import com.jamid.workconnect.R
import com.jamid.workconnect.adapter.paging2.LocalMessageAdapter
import com.jamid.workconnect.databinding.FragmentMediaDocumentBinding
import com.jamid.workconnect.model.ChatChannel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class MediaDocumentFragment : InsetControlFragment(R.layout.fragment_media_document) {

    private lateinit var binding: FragmentMediaDocumentBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMediaDocumentBinding.bind(view)

        setInsetView(binding.mediaDocumentRecycler, mapOf(INSET_TOP to 104, INSET_BOTTOM to 0))
        var job: Job? = null
        val chatChannel = arguments?.getParcelable<ChatChannel>(ARG_CHAT_CHANNEL) ?: return

        val documentAdapter = LocalMessageAdapter(activity, viewModel)

        OverScrollDecoratorHelper.setUpOverScroll(binding.noDocumentScrollLayout)

        binding.mediaDocumentRecycler.apply {
            adapter = documentAdapter
            layoutManager = LinearLayoutManager(activity)
            addItemDecoration(DividerItemDecoration(activity, LinearLayoutManager.VERTICAL))
        }

        OverScrollDecoratorHelper.setUpOverScroll(binding.mediaDocumentRecycler, OverScrollDecoratorHelper.ORIENTATION_VERTICAL)

        viewModel.messagesByType(chatChannel.chatChannelId, DOCUMENT).observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                job?.cancel()
                activity.mainBinding.primaryProgressBar.visibility = View.GONE
                binding.noDocumentScrollLayout.visibility = View.GONE
                binding.mediaDocumentRecycler.visibility = View.VISIBLE
                documentAdapter.submitList(it)
            } else {
                activity.mainBinding.primaryProgressBar.visibility = View.VISIBLE
                job = lifecycleScope.launch {
                    delay(2000)
                    activity.mainBinding.primaryProgressBar.visibility = View.GONE
                    binding.noDocumentScrollLayout.visibility = View.VISIBLE
                    binding.mediaDocumentRecycler.visibility = View.GONE
                }
            }
        }

    }

    companion object {

        const val ARG_CHAT_CHANNEL = "ARG_CHAT_CHANNEL"

        @JvmStatic
        fun newInstance(chatChannel: ChatChannel) = MediaDocumentFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_CHAT_CHANNEL, chatChannel)
            }
        }
    }
}