package com.jamid.workconnect

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.workconnect.adapter.GenericAdapter
import com.jamid.workconnect.databinding.FragmentContributorsBinding
import com.jamid.workconnect.model.ChatChannel
import com.jamid.workconnect.model.User
import java.util.*

class ContributorsFragment : SupportFragment(R.layout.fragment_contributors, "", false) {

    private lateinit var binding: FragmentContributorsBinding
    private lateinit var contributorsAdapter: GenericAdapter<User>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentContributorsBinding.bind(view)

        binding.contributorsMotionLayout.transitionToEnd()

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, _) ->
            binding.contributorsMotionLayout.updateLayout(marginTop = top)
        }

        binding.cancelSearchBtn.setOnClickListener {
            findNavController().navigateUp()
        }

        val chatChannel = arguments?.getParcelable<ChatChannel>(ARG_CHAT_CHANNEL) ?: return

        viewModel.channelContributorsLive(chatChannel.chatChannelId).observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                initContributors(it, chatChannel.administrators)

                binding.searchBarText.doAfterTextChanged { t ->
                    if (!t.isNullOrBlank()) {
                        val capitalized = t.toString()
                            .replaceFirstChar { it1 -> if (it1.isLowerCase()) it1.titlecase(Locale.getDefault()) else it1.toString() }
                        val upperCase = t.toString().uppercase()
                        val lowerCase = t.toString().lowercase()

                        contributorsAdapter.submitList(it.filter { it1 -> (it1.name.contains(capitalized) || it1.name.contains(upperCase) || it1.name.contains(lowerCase))})
                    } else {
                        contributorsAdapter.submitList(it)
                    }
                }

            }
        }

    }

    private fun initContributors(contributors: List<User>, administrators: List<String>) {
        contributorsAdapter = GenericAdapter(User::class.java, mapOf("isHorizontal" to true, "administrators" to administrators))
        binding.contributorsRecycler.apply {
            itemAnimator = null
            layoutManager = LinearLayoutManager(activity)
            adapter = contributorsAdapter
        }

        contributorsAdapter.submitList(contributors)
    }

    companion object {

        const val TAG = "ContributorsFragment"
        const val TITLE = "Contributors"
        const val ARG_CHAT_CHANNEL = "ChatChannel"

        @JvmStatic
        fun newInstance() = ContributorsFragment().apply {
                arguments = Bundle()
            }
    }
}