package com.jamid.workconnect.message

import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.workconnect.R
import com.jamid.workconnect.SupportFragment
import com.jamid.workconnect.adapter.paging2.UserHorizontalAdapter
import com.jamid.workconnect.convertDpToPx
import com.jamid.workconnect.databinding.FragmentProjectDetailBinding
import com.jamid.workconnect.model.ChatChannel
import me.everything.android.ui.overscroll.IOverScrollState
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class ProjectDetailFragment : SupportFragment(R.layout.fragment_project_detail, TAG, false) {

    private lateinit var binding: FragmentProjectDetailBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentProjectDetailBinding.bind(view)
        val chatChannel = arguments?.getParcelable<ChatChannel>(ARG_CHAT_CHANNEL) ?: return

        viewModel.getCachedPost(chatChannel.postId).observe(viewLifecycleOwner) {
            if (it != null) {
                binding.projectDetailImg.setImageURI(it.thumbnail)
                binding.projectDetailContent.projectDetailTitle.text = it.title
                binding.projectDetailContent.pdContent.text = it.content

                binding.projectDetailContent.guidelinesBtn.setOnClickListener { _ ->
                    val fragment = ProjectGuidelinesFragment.newInstance(chatChannel, it)
                    activity.toFragment(fragment, ProjectGuidelinesFragment.TAG)
                }
            } else {
                viewModel.getPost(chatChannel.postId)
            }
        }


        val contributorAdapter = UserHorizontalAdapter(activity)

        binding.projectDetailContent.projectDetailContributorsList.apply {
            adapter = contributorAdapter
            layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        }

        OverScrollDecoratorHelper.setUpOverScroll(binding.projectDetailContent.projectDetailContributorsList, OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL)

        viewModel.projectContributors(chatChannel.chatChannelId).observe(viewLifecycleOwner) { contributors ->
            if (contributors.isNotEmpty()) {
                contributorAdapter.submitList(contributors)
            } else {
                /*
				TODO("Do something when there is no contributor, but it's not possible
				 because the creator is also a contributor")
				* */
            }
        }

        val decorX = OverScrollDecoratorHelper.setUpOverScroll(binding.projectDetialScroller)

        var prevState = IOverScrollState.STATE_IDLE

        decorX.setOverScrollUpdateListener { decor, state, offset ->
            if (state == IOverScrollState.STATE_DRAG_START_SIDE) {
                prevState = state
                binding.projectDetailImg.scaleX = 1 + offset/800
                binding.projectDetailImg.scaleY = 1 + offset/800
            }
            if (state == IOverScrollState.STATE_BOUNCE_BACK && prevState == IOverScrollState.STATE_DRAG_START_SIDE) {
                binding.projectDetailImg.scaleX = 1 + offset/800
                binding.projectDetailImg.scaleY = 1 + offset/800
            }
            if (state == IOverScrollState.STATE_IDLE || state == IOverScrollState.STATE_DRAG_END_SIDE) {
                prevState = state
            }
        }



        binding.projectDetailContent.imageLinkDocBtn.setOnClickListener {
            val fragment = MediaFragment.newInstance(chatChannel)
            activity.toFragment(fragment, MediaFragment.TAG)
        }


        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            binding.projectDetialScroller.setPadding(0, convertDpToPx(200), 0, bottom + convertDpToPx(8))

            val params = binding.exitProjectDetail.layoutParams as ConstraintLayout.LayoutParams
            params.setMargins(convertDpToPx(8), top + convertDpToPx(8), 0, 0)
            binding.exitProjectDetail.layoutParams = params
        }

        binding.exitProjectDetail.setOnClickListener {
            activity.onBackPressed()
        }

    }

    companion object {
        const val TAG = "ProjectDetail"
        const val ARG_CHAT_CHANNEL = "ARG_CHAT_CHANNEL"

        @JvmStatic
        fun newInstance(chatChannel: ChatChannel) = ProjectDetailFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_CHAT_CHANNEL, chatChannel)
            }
        }
    }
}