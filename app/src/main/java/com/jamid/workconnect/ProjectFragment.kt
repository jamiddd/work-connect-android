package com.jamid.workconnect

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.chip.Chip
import com.jamid.workconnect.adapter.GenericAdapter
import com.jamid.workconnect.databinding.FragmentProjectBinding
import com.jamid.workconnect.interfaces.OnChipClickListener
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.Result
import com.jamid.workconnect.model.User
import com.jamid.workconnect.profile.ProfileFragment
import java.text.SimpleDateFormat
import java.util.*


class ProjectFragment : BasePostFragment(R.layout.fragment_project, TAG, false) {

    private lateinit var binding: FragmentProjectBinding
    private var time: Long = 0
    private lateinit var onChipClickListener: OnChipClickListener

    @SuppressLint("ShowToast")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentProjectBinding.bind(view)
        time = System.currentTimeMillis()
        postId = arguments?.getString(ARG_POST_ID)
        bottomBinding = binding.projectMetadata
        initLayoutChanges(binding.projectContent.projectScrollContainer)

        onChipClickListener = activity

        binding.projectFragmentToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        hideKeyboard()

        if (postId == null) {
            post = arguments?.getParcelable(ARG_POST)
            setPost()
            initProject(post!!)
        } else {
            /*TODO("Posts are not cached in database anymore. Download the post directly from firebase. Since it came from an id, most probably" +
                    "it doesn't require back propagation")*/
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
                    binding.projectMetadata.postJoinBtn.text = "Sent Request"
                    binding.projectMetadata.postJoinBtn.isEnabled = false

                    Toast.makeText(requireContext(), "Sent request to join project!", Toast.LENGTH_SHORT).show()
                    /*
                    Snackbar.make(binding.root, "Sent request to join project!", Snackbar.LENGTH_LONG)
                        .setAnchorView(binding.projectData)
                        .show()*/
                }
                is Result.Error -> {
                    binding.projectMetadata.postJoinBtn.isEnabled = true
                    Toast.makeText(requireContext(), "Something went wrong !", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun adjustLikeText(joinBtnVisibility: Boolean = true) {

        val params = binding.projectMetadata.likeDislikeText.layoutParams as ConstraintLayout.LayoutParams

        if (joinBtnVisibility) {
            params.endToEnd = binding.projectMetadata.postJoinBtn.id
        } else {
            params.endToEnd = binding.projectMetadata.linearLayout.id
        }

        params.startToStart = binding.projectMetadata.linearLayout.id
        params.topToBottom = binding.projectMetadata.genericMenuHandle.id
        params.bottomToTop = binding.projectMetadata.divider20.id
        binding.projectMetadata.likeDislikeText.layoutParams = params
    }

    private fun initProject(post: Post) {
        viewModel.extras[ARG_POST] = post

        val joinBtn = binding.projectMetadata.postJoinBtn

        val currentUser = viewModel.user.value
        if (currentUser != null) {
            joinBtn.visibility = View.VISIBLE

            when {
                post.uid == currentUser.id -> {
                    joinBtn.visibility = View.GONE
                    adjustLikeText(false)
                }
                currentUser.userPrivate.collaborationIds.contains(post.id) -> {
                    joinBtn.visibility = View.GONE
                    adjustLikeText(false)
                }
                currentUser.userPrivate.activeRequests.contains(post.id) -> {
                    joinBtn.visibility = View.VISIBLE
                    joinBtn.text = getString(R.string.sent_request)
                    joinBtn.isEnabled = false
                    adjustLikeText()
                }
                else -> {
                    joinBtn.visibility = View.VISIBLE
                    joinBtn.isEnabled = true
                    adjustLikeText()
                }
            }

        } else {
            joinBtn.visibility = View.GONE
        }

        joinBtn.setOnClickListener {
            viewModel.joinProject(post)
        }

        binding.projectImg.setImageURI(post.images[0])
        binding.projectImg.setColorFilter(ContextCompat.getColor(activity, R.color.light_black_overlay))

        val name = post.admin.name
        val photo = post.admin.photo

        binding.projectFragmentToolbar.title = ""
        binding.projectFragmentToolbar.subtitle = ""
        binding.projectAdminImg.setImageURI(photo)
        binding.projectAdminImg.visibility = View.GONE

        val options = navOptions {
            anim {
                enter = R.anim.slide_in_right
                exit = R.anim.slide_out_left
                popEnter = R.anim.slide_in_left
                popExit = R.anim.slide_out_right
            }
        }

        binding.projectAdminImg.setOnClickListener {
            findNavController().navigate(R.id.profileFragment, Bundle().apply { putParcelable(ProfileFragment.ARG_USER, post.admin) }, options)
        }

        binding.projectContent.projectTitle.text = post.title

        binding.projectFragmentAppBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            if (verticalOffset == 0) {
                binding.projectFragmentToolbar.setNavigationIconTint(ContextCompat.getColor(activity, R.color.white))
                binding.projectFragmentToolbar.setTitleTextColor(ContextCompat.getColor(activity, R.color.white))
                binding.projectFragmentToolbar.setSubtitleTextColor(ContextCompat.getColor(activity, R.color.white))
            } else {
                binding.projectFragmentToolbar.setTitleTextColor(ContextCompat.getColor(activity, R.color.black))
                binding.projectFragmentToolbar.setSubtitleTextColor(ContextCompat.getColor(activity, R.color.black))
                binding.projectFragmentToolbar.setNavigationIconTint(ContextCompat.getColor(activity, R.color.black))
            }
        })

        binding.projectContent.projectScrollContainer.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (scrollY > binding.projectContent.projectTitle.measuredHeight + binding.projectContent.projectAuthorLayout.measuredHeight) {
                binding.projectAdminImg.visibility = View.VISIBLE
            } else {
                binding.projectAdminImg.visibility = View.GONE
            }

            if (scrollY > binding.projectContent.projectTitle.measuredHeight) {
                binding.projectFragmentToolbar.title = post.title
                binding.projectFragmentToolbar.subtitle = "- $name"
            } else {
                binding.projectFragmentToolbar.title = ""
                binding.projectFragmentToolbar.subtitle = ""
            }
        }

        binding.projectMetadata.apply {

            binding.projectContent.projectAuthorPhoto.setImageURI(photo)
            binding.projectContent.projectAuthorName.text = name

            if (Build.VERSION.SDK_INT <= 27) {
                binding.projectContent.projectAuthorName.setTextColor(ContextCompat.getColor(activity, R.color.black))
                binding.projectContent.projectTitle.setTextColor(ContextCompat.getColor(activity, R.color.black))
            }

        }

        binding.projectContent.apply {

            projectContent.text = post.content
            if (Build.VERSION.SDK_INT <= 27) {
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

            initContributors()
        }



        binding.projectContent.projectAuthorName.setOnClickListener {
            findNavController().navigate(R.id.profileFragment, Bundle().apply { putParcelable(ProfileFragment.ARG_USER, post.admin) }, options)
        }

        binding.projectContent.projectAuthorPhoto.setOnClickListener {
            findNavController().navigate(R.id.profileFragment, Bundle().apply { putParcelable(ProfileFragment.ARG_USER, post.admin) }, options)
        }
    }

    private fun initContributors() {
        val contributorsAdapter = GenericAdapter(User::class.java, mapOf(ADMINISTRATORS to listOf(post!!.uid)))
        binding.projectContent.projectContributorsList.apply {
            itemAnimator = null
            layoutManager = LinearLayoutManager(
                activity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = contributorsAdapter
        }

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            when(val contributorsResult = viewModel.getProjectContributors(post!!)) {
                is Result.Success -> {
                    val contributors = viewModel.filterUsers(contributorsResult.data)
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

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, _) ->
            binding.projectFragmentToolbar.updateLayout(marginTop = top)
        }

    }

    private fun addNewChip(s: String) {
        val chip = LayoutInflater.from(activity).inflate(R.layout.chip, null) as Chip
        chip.text = s

        chip.isChecked = true

        chip.setOnClickListener {
            chip.isChecked = true
            onChipClickListener.onChipClick(s)
//            activity.toFragment(TagPostsFragment.newInstance(s), TagPostsFragment.TAG)
        }

        binding.projectContent.projectTagsList.addView(chip)
    }

    override fun onDestroy() {
        super.onDestroy()
       /* time = System.currentTimeMillis() - time
        if (time > IS_INTERESTED_DURATION) {
            viewModel.increaseProjectWeight(post)
        }*/
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

/*binding.projectScrollContainer.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
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
	  }*/

//        OverScrollDecoratorHelper.setUpOverScroll(binding.projectContent.projectContributorsList, OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL)

/*val user = viewModel.user.value
    if (user != null) {
        activity.mainBinding.primaryMenuBtn.isEnabled = false


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
    }*/


/*binding.projectScrollContainer.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
    if (scrollY > binding.projectContent.projectImg.measuredHeight + binding.projectContent.projectTitle.measuredHeight) {
        activity.setFragmentTitle(post.title)
    } else {
        activity.setFragmentTitle("")
    }
}*/