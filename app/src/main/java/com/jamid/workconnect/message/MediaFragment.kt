package com.jamid.workconnect.message

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.jamid.workconnect.MainActivity
import com.jamid.workconnect.R
import com.jamid.workconnect.SupportFragment
import com.jamid.workconnect.databinding.FragmentMediaBinding
import com.jamid.workconnect.model.ChatChannel
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class MediaFragment : SupportFragment(R.layout.fragment_media, TAG, false) {

    private lateinit var binding: FragmentMediaBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentMediaBinding.bind(view)

        val chatChannel = arguments?.getParcelable<ChatChannel>(ARG_CHAT_CHANNEL) ?: return

        binding.mediaViewPager.adapter = MediaPager(chatChannel, activity)

        TabLayoutMediator(activity.mainBinding.primaryTabs, binding.mediaViewPager) { tab, pos ->
            when (pos) {
                0 -> tab.text = "Image"
                1 -> tab.text = "Links"
                2 -> tab.text = "Docs"
            }
        }.attach()

        OverScrollDecoratorHelper.setUpOverScroll(binding.mediaViewPager.getChildAt(0) as RecyclerView, OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL)

    }

    inner class MediaPager(val chatChannel: ChatChannel, fa: FragmentActivity): FragmentStateAdapter(fa) {
        override fun getItemCount() = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> MediaImageFragment.newInstance(chatChannel)
                1 -> MediaLinksFragment.newInstance(chatChannel)
                2 -> MediaDocumentFragment.newInstance(chatChannel)
                else -> MediaDocumentFragment.newInstance(chatChannel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity?)?.mainBinding?.primaryProgressBar?.visibility = View.GONE
    }

    companion object {

        const val TAG = "MediaFragment"
        private const val ARG_CHAT_CHANNEL = "ARG_CHAT_CHANNEL"
        const val TITLE = "Media"

        @JvmStatic
        fun newInstance(chatChannel: ChatChannel) = MediaFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_CHAT_CHANNEL, chatChannel)
            }
        }
    }
}