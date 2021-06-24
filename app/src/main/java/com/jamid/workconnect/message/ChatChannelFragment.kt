package com.jamid.workconnect.message

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.workconnect.InsetControlFragment
import com.jamid.workconnect.R
import com.jamid.workconnect.adapter.GenericAdapter
import com.jamid.workconnect.convertDpToPx
import com.jamid.workconnect.databinding.FragmentChatChannelBinding
import com.jamid.workconnect.model.ChatChannel
import com.jamid.workconnect.profile.ProfileFragment
import com.jamid.workconnect.updateLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class ChatChannelFragment : InsetControlFragment(R.layout.fragment_chat_channel) {

    private lateinit var binding: FragmentChatChannelBinding
    private lateinit var channelAdapter: GenericAdapter<ChatChannel>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentChatChannelBinding.bind(view)

        initChannelsAdapter()

        initRefresher()

        viewModel.chatChannelsLiveData.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                hideEmptyChatUI()

                binding.chatSearchEditText.doAfterTextChanged { s ->
                    if (s.isNullOrBlank()) {
                        channelAdapter.submitList(it)
                    } else {
                        val query = "%$s%"
                        viewLifecycleOwner.lifecycleScope.launch {
                            val chatChannels = viewModel.getChannelsForQuery(query)
                            channelAdapter.submitList(chatChannels)
                        }
                    }
                }

                channelAdapter.submitList(it)
            } else {
                showEmptyChatUI()
            }
        }

        binding.noChannelsExploreBtn.setOnClickListener {
            activity.mainBinding.bottomNav.selectedItemId = R.id.explore_navigation
        }

        val options = navOptions {
            anim {
                enter = R.anim.slide_in_right
                exit = R.anim.slide_out_left
                popEnter = R.anim.slide_in_left
                popExit = R.anim.slide_out_right
            }
        }

        viewModel.user.observe(viewLifecycleOwner) {
            if (it != null) {
                viewModel.setChatChannelListeners()
                binding.accountImg.setImageURI(it.photo)
                binding.accountImg.setOnClickListener { _ ->
                    findNavController().navigate(R.id.profileFragment, Bundle().apply { putParcelable(ProfileFragment.ARG_USER, it) }, options)
                }
            } else {
                showEmptyChatUI()
                binding.accountImg.setOnClickListener {
                    findNavController().navigate(R.id.signInFragment, null, options)
                }
            }
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, _) ->
            binding.archiveButton.updateLayout(marginTop = top + convertDpToPx(4), marginLeft = convertDpToPx(16))
            binding.accountImg.updateLayout(marginTop = top + convertDpToPx(10), marginRight = convertDpToPx(16))
            binding.chatChannelFragmentToolbar.updateLayout(marginTop = top)
        }


    }

    private fun showEmptyChatUI() {
        binding.chatChannelRecycler.visibility = View.GONE
        binding.noChannelsLayoutScroll.visibility = View.VISIBLE
    }

    private fun hideEmptyChatUI() {
        binding.chatChannelRecycler.visibility = View.VISIBLE
        binding.noChannelsLayoutScroll.visibility = View.GONE
    }

    private fun initRefresher() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (activity.resources?.configuration?.isNightModeActive == true) {
                binding.chatChannelRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.purple_200))
                binding.chatChannelRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.darkestGrey))
            } else {
                binding.chatChannelRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.blue_500))
                binding.chatChannelRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.white))
            }
        } else {
            if (activity.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
                binding.chatChannelRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.purple_200))
                binding.chatChannelRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.darkerGrey))
            } else {
                binding.chatChannelRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.blue_500))
                binding.chatChannelRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.white))
            }
        }

        binding.chatChannelRefresher.setOnRefreshListener {

            val externalDir = activity.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            if (externalDir != null) {
                viewModel.setChatChannelListeners()
            }
            viewLifecycleOwner.lifecycleScope.launchWhenCreated {
                delay(3000)
                binding.chatChannelRefresher.isRefreshing = false
            }
        }
    }

    private fun initChannelsAdapter() {
        channelAdapter = GenericAdapter(ChatChannel::class.java)

        binding.chatChannelRecycler.apply {
            adapter = channelAdapter
            layoutManager = LinearLayoutManager(activity)
            addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
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