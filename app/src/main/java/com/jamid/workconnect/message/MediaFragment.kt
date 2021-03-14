package com.jamid.workconnect.message

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.jamid.workconnect.MainActivity
import com.jamid.workconnect.MainViewModel
import com.jamid.workconnect.R
import com.jamid.workconnect.databinding.FragmentMediaBinding
import com.jamid.workconnect.model.ChatChannel

class MediaFragment : Fragment(R.layout.fragment_media) {

    private lateinit var binding: FragmentMediaBinding
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentMediaBinding.bind(view)
        val activity = requireActivity() as MainActivity
        val viewModel: ProjectDetailViewModel by navGraphViewModels(R.id.project_detail_navigation)

        viewModel.currentChatChannel.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.mediaViewPager.adapter = MediaPager(it.chatChannelId, activity)

                val mediaTabs = activity.findViewById<TabLayout>(R.id.mediaTabs)

                TabLayoutMediator(mediaTabs, binding.mediaViewPager) { tab, pos ->
                    when (pos) {
                        0 -> tab.text = "Image"
                        1 -> tab.text = "Links"
                        2 -> tab.text = "Docs"
                    }
                }
            }
        }

        activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

       /* mainViewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            val windowHeight = getWindowHeight() + top + bottom

            val params = binding.root.layoutParams as ViewGroup.LayoutParams
            params.height = windowHeight
            params.width = ViewGroup.LayoutParams.MATCH_PARENT

            binding.root.layoutParams = params

        }*/
    }

    inner class MediaPager(val chatChannelId: String, fa: FragmentActivity): FragmentStateAdapter(fa) {
        override fun getItemCount() = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> MediaImageFragment.newInstance(chatChannelId)
                1 -> MediaLinksFragment.newInstance()
                2 -> MediaDocumentFragment.newInstance()
                else -> MediaDocumentFragment.newInstance()
            }
        }

    }

    companion object {

        const val TAG = "MediaFragment"
        const val ARG_CHAT_CHANNEL = "ARG_CHAT_CHANNEL"

        @JvmStatic
        fun newInstance(chatChannel: ChatChannel) = MediaFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_CHAT_CHANNEL, chatChannel)
            }
        }
    }
}