package com.jamid.workconnect.message

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.jamid.workconnect.*
import com.jamid.workconnect.adapter.GenericAdapter
import com.jamid.workconnect.adapter.paging2.MessageAdapter
import com.jamid.workconnect.databinding.FragmentProjectDetailBinding
import com.jamid.workconnect.model.ChatChannel
import com.jamid.workconnect.model.User
import kotlinx.coroutines.launch

class ProjectDetailFragment : SupportFragment(R.layout.fragment_project_detail) {

    private lateinit var binding: FragmentProjectDetailBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentProjectDetailBinding.bind(view)
        val chatChannel = arguments?.getParcelable<ChatChannel>(ARG_CHAT_CHANNEL) ?: return

        binding.projectDetailFragmentToolbar.title = chatChannel.postTitle

        viewModel.getCachedPost(chatChannel.postId).observe(viewLifecycleOwner) {
            if (it != null) {
                binding.projectDetailImg.setImageURI(it.images[0])
                binding.projectDetailContent.pdContent.text = it.content

                binding.projectDetailContent.guidelinesBtn.setOnClickListener { _ ->
                    val bundle = Bundle().apply {
                        putParcelable(ProjectGuidelinesFragment.ARG_CHAT_CHANNEL, chatChannel)
                        putParcelable(ProjectGuidelinesFragment.ARG_POST, it)
                    }
                    findNavController().navigate(R.id.projectGuidelinesFragment, bundle, slideRightNavOptions())
                }

                if (it.guidelines.isNullOrBlank()) {
                    if (it.uid == viewModel.user.value?.id) {
                        binding.projectDetailContent.guidelinesBtn.text = getString(R.string.project_guidelines_info)
                        binding.projectDetailContent.guidelinesBtn.isEnabled = true
                        binding.projectDetailContent.projectGuidelinesPreview.text = getString(R.string.project_guidelines_info_long)
                        binding.projectDetailContent.projectGuidelinesPreview.visibility = View.VISIBLE
                    } else {
                        binding.projectDetailContent.guidelinesBtn.isEnabled = false
                        binding.projectDetailContent.projectGuidelinesPreview.visibility = View.GONE
                    }
                } else {
                    binding.projectDetailContent.guidelinesBtn.isEnabled = true
                    binding.projectDetailContent.projectGuidelinesPreview.text = it.guidelines
                    binding.projectDetailContent.projectGuidelinesPreview.visibility = View.VISIBLE
                }

            } else {
                viewModel.getPost(chatChannel.postId)
            }
        }

        binding.projectDetailContent.contributorsHorizontalHeader.setOnClickListener {
            findNavController().navigate(R.id.contributorsFragment, Bundle().apply { putParcelable(ContributorsFragment.ARG_CHAT_CHANNEL, chatChannel)}, slideRightNavOptions())
        }

        viewModel.channelContributorsLive(chatChannel.chatChannelId).observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                initContributors(it, chatChannel.administrators)
            }
        }

        binding.projectDetailFragmentToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        initImageAdapter(chatChannel)

        binding.projectDetailFragmentToolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.refresh -> {
                    viewModel.getPost(chatChannel.postId)
                }
            }
            true
        }

        binding.projectDetailImg.setColorFilter(ContextCompat.getColor(activity, R.color.light_black_overlay))

        binding.projectDetailContent.imageLinkDocBtn.setOnClickListener {
            findNavController().navigate(R.id.mediaFragment, Bundle().apply { putParcelable(MediaFragment.ARG_CHAT_CHANNEL, chatChannel) }, slideRightNavOptions())
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            binding.projectDetailScroller.setPadding(0, 0, 0, bottom + convertDpToPx(8))
            binding.projectDetailFragmentToolbar.updateLayout(marginTop = top)
        }

        binding.projectFragmentAppBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
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
        private const val ARG_CONTRIBUTORS = "ARG_CONTRIBUTORS"

        @JvmStatic
        fun newInstance(chatChannel: ChatChannel, contributors: ArrayList<User>) = ProjectDetailFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_CHAT_CHANNEL, chatChannel)
                putParcelableArrayList(ARG_CONTRIBUTORS, contributors)
            }
        }
    }
}