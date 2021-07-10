package com.jamid.workconnect.message

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.jamid.workconnect.R
import com.jamid.workconnect.SupportFragment
import com.jamid.workconnect.databinding.FragmentMediaBinding
import com.jamid.workconnect.model.ChatChannel
import com.jamid.workconnect.updateLayout

class MediaFragment : SupportFragment(R.layout.fragment_media) {

    private lateinit var binding: FragmentMediaBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_media, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentMediaBinding.bind(view)

        binding.mediaFragmentToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        val chatChannel = arguments?.getParcelable<ChatChannel>(ARG_CHAT_CHANNEL) ?: return

        binding.mediaViewPager.adapter = MediaPager(chatChannel, activity)

        TabLayoutMediator(binding.mediaFragmentTabLayout, binding.mediaViewPager) { tab, pos ->
            when (pos) {
                0 -> tab.text = "Images"
                1 -> tab.text = "Documents"
                2 -> tab.text = "Links"
            }
        }.attach()


        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, _) ->
            binding.mediaFragmentToolbar.updateLayout(marginTop = top)
        }
    }

    inner class MediaPager(val chatChannel: ChatChannel, fa: FragmentActivity): FragmentStateAdapter(fa) {
        override fun getItemCount() = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> MediaImageFragment.newInstance(chatChannel)
                1 -> MediaDocumentFragment.newInstance(chatChannel)
                2 -> MediaLinksFragment.newInstance(chatChannel)
                else -> MediaDocumentFragment.newInstance(chatChannel)
            }
        }
    }

    companion object {

        const val TAG = "MediaFragment"
        const val ARG_CHAT_CHANNEL = "ARG_CHAT_CHANNEL"
        const val TITLE = "Media"

        @JvmStatic
        fun newInstance(chatChannel: ChatChannel) = MediaFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_CHAT_CHANNEL, chatChannel)
            }
        }
    }
}