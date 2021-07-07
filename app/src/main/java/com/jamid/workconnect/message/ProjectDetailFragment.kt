package com.jamid.workconnect.message

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.*
import com.jamid.workconnect.adapter.GenericAdapter
import com.jamid.workconnect.adapter.paging2.MessageAdapter
import com.jamid.workconnect.adapter.paging2.SimpleMessageAdapter
import com.jamid.workconnect.databinding.FragmentProjectDetailBinding
import com.jamid.workconnect.model.ChatChannel
import com.jamid.workconnect.model.SimpleMessage
import com.jamid.workconnect.model.User
import kotlinx.coroutines.launch

class ProjectDetailFragment : SupportFragment(R.layout.fragment_project_detail, TAG, false) {

    private lateinit var binding: FragmentProjectDetailBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentProjectDetailBinding.bind(view)
        val chatChannel = arguments?.getParcelable<ChatChannel>(ARG_CHAT_CHANNEL) ?: return

        binding.projectDetailFragmentToolbar.title = chatChannel.postTitle

        val options = navOptions {
            anim {
                enter = R.anim.slide_in_right
                exit = R.anim.slide_out_left
                popEnter = R.anim.slide_in_left
                popExit = R.anim.slide_out_right
            }
        }

        viewModel.getCachedPost(chatChannel.postId).observe(viewLifecycleOwner) {
            if (it != null) {
                binding.projectDetailImg.setImageURI(it.thumbnail)
                binding.projectDetailContent.pdContent.text = it.content

                binding.projectDetailContent.guidelinesBtn.setOnClickListener { _ ->
                    val bundle = Bundle().apply {
                        putParcelable(ProjectGuidelinesFragment.ARG_CHAT_CHANNEL, chatChannel)
                        putParcelable(ProjectGuidelinesFragment.ARG_POST, it)
                    }
                    findNavController().navigate(R.id.projectGuidelinesFragment, bundle, options)
                }

                if (it.guidelines.isNullOrBlank()) {
                    if (it.uid == viewModel.user.value?.id) {
                        binding.projectDetailContent.guidelinesBtn.text = "Create Project Guidelines"
                        binding.projectDetailContent.guidelinesBtn.isEnabled = true
                        binding.projectDetailContent.projectGuidelinesPreview.text = "Create guidelines for other contributors for the appropriate code of conduct for this purpose."
                        binding.projectDetailContent.projectGuidelinesPreview.visibility = View.VISIBLE
                    } else {
                        binding.projectDetailContent.guidelinesBtn.isEnabled = false
                        binding.projectDetailContent.projectGuidelinesPreview.visibility = View.GONE
                    }
                } else {
                    binding.projectDetailContent.projectGuidelinesPreview.text = it.guidelines
                    binding.projectDetailContent.projectGuidelinesPreview.visibility = View.VISIBLE
                }

            } else {
                viewModel.getPost(chatChannel.postId)
            }
        }


        /*val contributorAdapter = UserHorizontalAdapter(activity)

        binding.projectDetailContent.projectDetailContributorsList.apply {
            adapter = contributorAdapter
            layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        }*/

//        OverScrollDecoratorHelper.setUpOverScroll(binding.projectDetailContent.projectDetailContributorsList, OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL)

        binding.projectDetailContent.contributorsHorizontalHeader.setOnClickListener {
            findNavController().navigate(R.id.contributorsFragment, Bundle().apply { putParcelable(ContributorsFragment.ARG_CHAT_CHANNEL, chatChannel)}, options)
        }

        viewModel.channelContributorsLive(chatChannel.chatChannelId).observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                initContributors(it, chatChannel.administrators)
            }
        }

        binding.projectDetailFragmentToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }


        /*viewModel.projectContributors(chatChannel.chatChannelId).observe(viewLifecycleOwner) { contributors ->
            if (contributors.isNotEmpty()) {
                contributorAdapter.submitList(contributors)
            } else {
                *//*
				TODO("Do something when there is no contributor, but it's not possible
				 because the creator is also a contributor")
				* *//*
            }
        }*/

        /*val decorX = OverScrollDecoratorHelper.setUpOverScroll(binding.projectDetialScroller)

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
        }*/

       /* Firebase.firestore.collection(CHAT_CHANNELS).document(chatChannel.chatChannelId)
            .collection(MESSAGES)
            .whereEqualTo(TYPE, IMAGE)
            .orderBy(CREATED_AT, Query.Direction.DESCENDING)
            .limit(6)
            .get()
            .addOnSuccessListener {
                if (!it.isEmpty) {

                }
            }.addOnFailureListener {
                Toast.makeText(activity, it.localizedMessage, Toast.LENGTH_LONG).show()
            }*/

        initImageAdapter(chatChannel)

        binding.projectDetailImg.setColorFilter(ContextCompat.getColor(activity, R.color.light_black_overlay))

        binding.projectDetailContent.imageLinkDocBtn.setOnClickListener {
            findNavController().navigate(R.id.mediaFragment, Bundle().apply { putParcelable(MediaFragment.ARG_CHAT_CHANNEL, chatChannel) }, options)
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            binding.projectDetailScroller.setPadding(0, 0, 0, bottom + convertDpToPx(8))
            binding.projectDetailFragmentToolbar.updateLayout(marginTop = top)
        }

        binding.projectFragmentAppBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            if (verticalOffset == 0) {
                binding.projectDetailFragmentToolbar.setNavigationIconTint(ContextCompat.getColor(activity, R.color.white))
            } else {
                binding.projectDetailFragmentToolbar.setNavigationIconTint(ContextCompat.getColor(activity, R.color.black))
            }
        })

    }

    private fun initImageAdapter(chatChannel: ChatChannel) {
        val messageAdapter = MessageAdapter(viewModel)

        binding.projectDetailContent.sampleMediaList.apply {
            adapter = messageAdapter
            layoutManager = GridLayoutManager(activity, 3)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val images = viewModel.localMessages(IMAGE, chatChannel.chatChannelId, 6)
            if (images.isNullOrEmpty()) {
                binding.projectDetailContent.sampleMediaList.visibility = View.GONE
                binding.projectDetailContent.imageLinkDocBtn.isEnabled = false
            } else {
                messageAdapter.submitList(images)
            }
        }

    }

    private fun initContributors(contributors: List<User>, administrators: List<String>) {
        val contributorsAdapter = GenericAdapter(User::class.java, mapOf("isHorizontal" to true, "administrators" to administrators))
        binding.projectDetailContent.projectDetailContributorsList.apply {
            itemAnimator = null
            layoutManager = LinearLayoutManager(activity)
            adapter = contributorsAdapter
        }

        contributorsAdapter.submitList(contributors)
    }

    companion object {
        const val TAG = "ProjectDetail"
        const val ARG_CHAT_CHANNEL = "ARG_CHAT_CHANNEL"
        const val ARG_CONTRIBUTORS = "ARG_CONTRIBUTORS"

        @JvmStatic
        fun newInstance(chatChannel: ChatChannel, contributors: ArrayList<User>) = ProjectDetailFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_CHAT_CHANNEL, chatChannel)
                putParcelableArrayList(ARG_CONTRIBUTORS, contributors)
            }
        }
    }
}