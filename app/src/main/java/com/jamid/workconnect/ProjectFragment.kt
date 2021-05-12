package com.jamid.workconnect

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.jamid.workconnect.adapter.GenericAdapter
import com.jamid.workconnect.databinding.FragmentProjectBinding
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.Result
import com.jamid.workconnect.model.User
import com.jamid.workconnect.profile.ProfileFragment
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*


class ProjectFragment : BasePostFragment(R.layout.fragment_project, TAG, false) {

    private lateinit var binding: FragmentProjectBinding
    private var time: Long = 0

    @SuppressLint("ShowToast")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentProjectBinding.bind(view)
        time = System.currentTimeMillis()
        postId = arguments?.getString(ARG_POST_ID)
        bottomBinding = binding.projectMetadata
        initLayoutChanges(binding.projectBottomBlur, binding.projectScrollContainer)

        if (Build.VERSION.SDK_INT <= 27) {
            val tempView = View(activity)
            tempView.setBackgroundColor(Color.WHITE)
            tempView.elevation = convertDpToPx(4).toFloat()
            binding.projectFragmentRoot.addView(tempView)
            val params = tempView.layoutParams as CoordinatorLayout.LayoutParams
            params.gravity = Gravity.BOTTOM
            params.height = viewModel.windowInsets.value!!.second
            tempView.layoutParams = params
        }

        hideKeyboard()

        if (postId == null) {
            post = arguments?.getParcelable(ARG_POST)
            setPost()
            initProject(post!!)
        } else {
            viewModel.getCachedPost(postId ?: "").observe(viewLifecycleOwner) { p0 ->
                if (p0 != null) {
                    post = p0
                    setPost()
                    initProject(post!!)
                } else {
                    viewModel.getPost(postId ?: "")
                }
            }
        }

        viewModel.requestSentResult.observe(viewLifecycleOwner) {
            val result = it ?: return@observe

            when (result) {
                is Result.Success -> {
                    activity.mainBinding.primaryMenuBtn.isEnabled = false
                    Snackbar.make(binding.root, "Sent request to join project!", Snackbar.LENGTH_LONG)
                        .setAnchorView(binding.projectData)
                        .show()
                }
                is Result.Error -> {
                    activity.mainBinding.primaryMenuBtn.isEnabled = true
                    Toast.makeText(requireContext(), "Something went wrong !", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        activity.mainBinding.primaryMenuBtn.setOnClickListener {
            val user = viewModel.user.value
            if (user != null) {
                activity.mainBinding.primaryMenuBtn.isEnabled = false
                viewModel.joinProject(post!!)

                if (user.id != post!!.uid) {
                    when {
                        user.userPrivate.collaborationIds.contains(post!!.id) -> {
                            activity.mainBinding.primaryMenuBtn.visibility = View.GONE
                        }
                        user.userPrivate.activeRequests.contains(post!!.id) -> {
                            activity.mainBinding.primaryMenuBtn.isEnabled = false
                        }
                        else -> {
                            activity.mainBinding.primaryMenuBtn.visibility = View.VISIBLE
                            activity.mainBinding.primaryMenuBtn.isEnabled = true
                        }
                    }
                } else {
                    activity.mainBinding.primaryMenuBtn.visibility = View.GONE
                }

            } else {
                activity.showSignInDialog(POST)
            }
        }
    }

    private fun initProject(post: Post) {
        viewModel.extras[ARG_POST] = post
        activity.setFragmentTitle(post.title)

        binding.projectContent.projectImg.setImageURI(post.thumbnail)

        val name = post.admin.name
        val photo = post.admin.photo

        binding.projectMetadata.apply {
            adminPhoto.setImageURI(photo)
            authorName.text = name

            if (Build.VERSION.SDK_INT <= 27) {
                authorName.setTextColor(ContextCompat.getColor(activity, R.color.black))
            }

        }

        binding.projectRefresher.setOnRefreshListener {
            viewLifecycleOwner.lifecycleScope.launchWhenCreated {
                delay(3000)
                binding.projectRefresher.isRefreshing = false
            }
        }

        binding.projectRefresher.setProgressViewOffset(false, 0, activity.mainBinding.primaryToolbar.measuredHeight/2 + viewModel.windowInsets.value!!.first)
        binding.projectRefresher.setSlingshotDistance(activity.mainBinding.primaryToolbar.measuredHeight/2 + viewModel.windowInsets.value!!.first)
        binding.projectRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.blue_500))

        binding.projectContent.apply {

            projectTitle.text = post.title
            projectContent.text = post.content

            if (Build.VERSION.SDK_INT <= 27) {
                projectTitle.setTextColor(ContextCompat.getColor(activity, R.color.black))
                projectContent.setTextColor(ContextCompat.getColor(activity, R.color.black))
            }

            if (post.location == null) {
                projectMetaText.text = SimpleDateFormat("hh:mm a", Locale.UK).format(post.updatedAt)
            } else {
                val metaText = SimpleDateFormat("hh:mm a", Locale.UK).format(post.updatedAt) + " • " + post.location!!.place
                projectMetaText.text = metaText
            }


            val tags = post.tags
            if (!tags.isNullOrEmpty()) {

                projectTagHeader.visibility = View.VISIBLE
                projectTagsList.visibility = View.VISIBLE

                binding.projectContent.projectTagsList.removeAllViews()

                tags.forEach {
                    addNewChip(it)
                }

            } else {
                projectTagHeader.visibility = View.GONE
                projectTagsList.visibility = View.GONE
            }

            /*val contributorAdapter = UserHorizontalAdapter(activity)

            projectContributorsList.apply {
                itemAnimator = null
                layoutManager = LinearLayoutManager(
                    activity,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
                adapter = contributorAdapter
            }*/

            initContributors()
        }

        binding.projectMetadata.authorName.setOnClickListener {
            activity.toFragment(ProfileFragment.newInstance(user=post.admin), ProfileFragment.TAG)
        }

        binding.projectMetadata.adminPhoto.setOnClickListener {
            activity.toFragment(ProfileFragment.newInstance(user=post.admin), ProfileFragment.TAG)
        }
    }

    private fun initContributors() {
        val contributorsAdapter = GenericAdapter(User::class.java)
        binding.projectContent.projectContributorsList.apply {
            itemAnimator = null
            layoutManager = LinearLayoutManager(
                activity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = contributorsAdapter
        }

        binding.projectScrollContainer.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (scrollY > activity.mainBinding.primaryAppBar.measuredHeight + binding.projectContent.projectTitle.measuredHeight) {
                activity.mainBinding.apply {
                    primaryAppBar.apply {
                        visibility = View.VISIBLE
                        alpha = 1f
                    }
                    topDivider.apply {
                        visibility = View.VISIBLE
                        alpha = 1f
                    }
                    primaryToolbar.visibility = View.VISIBLE
                }
            } else {
                activity.mainBinding.apply {
                    primaryAppBar.apply {
                        visibility = View.INVISIBLE
                        alpha = 0f
                    }
                    topDivider.apply {
                        visibility = View.INVISIBLE
                        alpha = 0f
                    }
                    primaryToolbar.visibility = View.GONE
                }
            }
        }

//        OverScrollDecoratorHelper.setUpOverScroll(binding.projectContent.projectContributorsList, OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL)

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
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
        }
    }

    private fun addNewChip(s: String) {
        val chip = Chip(requireContext())
        chip.text = s
        binding.projectContent.projectTagsList.addView(chip)
    }

    override fun onDestroy() {
        super.onDestroy()
        time = System.currentTimeMillis() - time
        if (time > IS_INTERESTED_DURATION) {
            viewModel.increaseProjectWeight(post)
        }
        viewModel.clearProjectFragmentResults()
    }

    companion object {

        const val TAG = "ProjectFragment"
        const val ARG_POST = "ARG_POST"
        const val ARG_POST_ID = "ARG_POST_ID"
        const val TITLE = ""

        @JvmStatic
        fun newInstance(id: String? = null, post: Post? = null) = ProjectFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_POST, post)
                putString(ARG_POST_ID, id)
            }
        }
    }

}