package com.jamid.workconnect.message

import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.jamid.workconnect.PagingListFragment
import com.jamid.workconnect.R
import com.jamid.workconnect.adapter.GenericAdapter
import com.jamid.workconnect.convertDpToPx
import com.jamid.workconnect.databinding.FragmentChatChannelBinding
import com.jamid.workconnect.model.ChatChannel
import com.jamid.workconnect.model.User
import com.jamid.workconnect.profile.ProfileFragment
import com.jamid.workconnect.updateLayout
import kotlinx.coroutines.launch

class ChatChannelFragment : PagingListFragment(R.layout.fragment_chat_channel) {

    private lateinit var binding: FragmentChatChannelBinding
    private lateinit var channelAdapter: GenericAdapter<ChatChannel>
    private var errorView: View? = null
    private var cachedUser: User? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentChatChannelBinding.bind(view)

        channelAdapter = GenericAdapter(ChatChannel::class.java)
        binding.chatChannelRecycler.setListAdapter(channelAdapter, onComplete = {
            viewModel.user.observe(viewLifecycleOwner) {
                if (it != null) {
                    if (cachedUser == null) {
                        viewModel.setChatChannelListeners()
                        cachedUser = it
                    }
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
        })

        binding.chatChannelRecycler.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))

        binding.chatChannelRefresher.setSwipeRefresher {
            val externalDir = activity.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            if (externalDir != null) {
                viewModel.setChatChannelListeners()
            }
            stopRefreshProgress(it)
        }

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

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            binding.archiveButton.updateLayout(marginTop = top + convertDpToPx(4), marginLeft = convertDpToPx(16))
            binding.accountImg.updateLayout(marginTop = top + convertDpToPx(10), marginRight = convertDpToPx(16))
            binding.chatChannelFragmentToolbar.updateLayout(marginTop = top)
            binding.chatChannelRecycler.setPadding(0, convertDpToPx(8), 0, bottom + convertDpToPx(56))
        }

    }

    private fun showEmptyChatUI() {
        binding.chatChannelRefresher.visibility = View.GONE
        if (errorView != null) {
            binding.chatChannelFragmentRoot.removeView(errorView)
            errorView = null
        }
        errorView = setErrorLayout(binding.chatChannelFragmentRoot, "No chats at the moment.\nTry collaborating in a project or Create a project.", errorImg = R.drawable.ic_empty_chat_channels, dependantView = binding.chatChannelAppBar, errorActionEnabled = false).root
    }

    private fun hideEmptyChatUI() {
        binding.chatChannelRefresher.visibility = View.VISIBLE
        errorView?.let {
            binding.chatChannelFragmentRoot.removeView(it)
        }
    }

    companion object {

        const val TAG = "ChatChannelFragment"

        @JvmStatic
        fun newInstance() = ChatChannelFragment()
    }

}