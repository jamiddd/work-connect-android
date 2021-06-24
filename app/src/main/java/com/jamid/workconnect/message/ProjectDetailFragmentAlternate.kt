package com.jamid.workconnect.message

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.*
import com.jamid.workconnect.adapter.GenericAdapter
import com.jamid.workconnect.databinding.FragmentProjectDetailBinding
import com.jamid.workconnect.model.ChatChannel
import com.jamid.workconnect.model.User

class ProjectDetailFragmentAlternate : DialogFragment() {

    private lateinit var binding: FragmentProjectDetailBinding
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_project_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentProjectDetailBinding.bind(view)
        val chatChannel = arguments?.getParcelable<ChatChannel>(ARG_CHAT_CHANNEL) ?: return
        val contributors = arguments?.getParcelableArrayList<User>(ARG_CONTRIBUTORS) ?: return

        viewModel.getCachedPost(chatChannel.postId).observe(viewLifecycleOwner) {
            if (it != null) {
                binding.projectDetailImg.setImageURI(it.thumbnail)
                binding.projectDetailContent.pdContent.text = it.content

                binding.projectDetailContent.guidelinesBtn.setOnClickListener { _ ->
                    val fragment = ProjectGuidelinesFragment.newInstance(chatChannel, it)
//                    activity.toFragment(fragment, ProjectGuidelinesFragment.TAG)
                }

                binding.projectDetailScroller.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->

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

//        initContributors(contributors)

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

        Firebase.firestore.collection(CHAT_CHANNELS).document(chatChannel.chatChannelId)
            .collection(MESSAGES)
            .whereEqualTo(TYPE, IMAGE)
            .orderBy(CREATED_AT, Query.Direction.DESCENDING)
            .limit(6)
            .get()
            .addOnSuccessListener {
                if (!it.isEmpty) {
                   /* val messages = it.toObjects(SimpleMessage::class.java)
                    val messageAdapter = MessageAdapter(viewModel)

                    binding.projectDetailContent.sampleMediaList.apply {
                        adapter = messageAdapter
                        layoutManager = GridLayoutManager(requireContext(), 3)
                    }

                    messageAdapter.submitList(messages)*/
                }
            }.addOnFailureListener {
                Toast.makeText(activity, it.localizedMessage, Toast.LENGTH_LONG).show()
            }

        binding.projectDetailImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.light_black_overlay))

        binding.projectDetailContent.imageLinkDocBtn.setOnClickListener {
            val fragment = MediaFragment.newInstance(chatChannel)
//            activity.toFragment(fragment, MediaFragment.TAG)
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            binding.projectDetailScroller.setPadding(0, 0, 0, bottom + convertDpToPx(8))
            binding.projectDetailFragmentToolbar.updateLayout(marginTop = top)
        }

        binding.projectFragmentAppBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            if (verticalOffset == 0) {
                binding.projectDetailFragmentToolbar.setNavigationIconTint(ContextCompat.getColor(requireContext(), R.color.white))
                binding.projectDetailFragmentToolbar.setTitleTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            } else {
                binding.projectDetailFragmentToolbar.setTitleTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                binding.projectDetailFragmentToolbar.setNavigationIconTint(ContextCompat.getColor(requireContext(), R.color.black))
            }
        })



    }

    private fun initContributors(contributors: ArrayList<User>) {
        val contributorsAdapter = GenericAdapter(User::class.java, mapOf("isHorizontal" to true))
        binding.projectDetailContent.projectDetailContributorsList.apply {
            itemAnimator = null
            layoutManager = LinearLayoutManager(activity)
            adapter = contributorsAdapter
        }

        contributorsAdapter.submitList(contributors)

        /*viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            when(val contributorsResult = viewModel.getProjectContributors(post!!)) {
                is Result.Success -> {
                    val contributors = contributorsResult.data.toObjects(User::class.java)
                    if (contributors.isNotEmpty()) {
                        binding.projectContent.projectContributorsHeader.visibility = View.VISIBLE
                        binding.projectContent.projectContributorsList.visibility = View.VISIBLE
                        contributorsAdapter.submitList(contributors)
                    } else {
                        binding.projectContent.projectContributorsHeader.visibility = View.GONE
                        binding.projectContent.projectContributorsList.visibility = View.GONE
                    }
                }
                is Result.Error -> {
                    binding.projectContent.projectContributorsHeader.visibility = View.GONE
                    binding.projectContent.projectContributorsList.visibility = View.GONE
                }
            }
        }*/
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