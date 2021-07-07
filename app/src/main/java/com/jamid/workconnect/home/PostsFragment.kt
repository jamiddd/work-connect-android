package com.jamid.workconnect.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.chip.Chip
import com.jamid.workconnect.*
import com.jamid.workconnect.adapter.paging3.PostAdapter
import com.jamid.workconnect.databinding.FragmentProjectsBinding
import com.jamid.workconnect.model.GenericMenuItem
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.User
import com.jamid.workconnect.profile.ProfileFragment
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


@OptIn(androidx.paging.ExperimentalPagingApi::class)
class PostsFragment : PagingListFragment(R.layout.fragment_projects) {

    private lateinit var binding: FragmentProjectsBinding
    private lateinit var postAdapter: PostAdapter
    private var emptyPostsRetryCount = 0
    private var errorView: View? = null
    private var cachedUser: User? = null

    fun getPosts(tag: String? = null) {
        Log.d(TAG, "Getting posts paging ... ")
        job?.cancel()
        job = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.postsFlow(tag).collect {
                postAdapter.submitData(it)
            }
        }
    }

    private var counter = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentProjectsBinding.bind(view)

        postAdapter = PostAdapter()
        binding.postsRefresher.isRefreshing = true

        stopRefreshProgress(binding.postsRefresher)

        binding.projectsFragmentList.setListAdapter(pagingAdapter = postAdapter,
            clazz = Post::class.java,
            onComplete = {
                binding.projectsFragmentList.addItemDecoration(
                    DividerItemDecoration(
                        activity,
                        DividerItemDecoration.VERTICAL
                    )
                )
                /*viewModel.user.observe(viewLifecycleOwner) {
                    if (cachedUser == null) {
                        Log.d(TAG, "Setting adapter and getting posts ${++counter}")
                        getPosts(viewModel.currentHomeTag.value)
                        cachedUser = it
                    }
                    if (it != null) {
                        binding.accountImg.setImageURI(it.photo)
                        binding.accountImg.setOnClickListener {
                            findNavController().navigate(
                                R.id.profileFragment,
                                Bundle().apply {
                                    putParcelable(
                                        ProfileFragment.ARG_USER,
                                        viewModel.user.value
                                    )
                                },
                                options
                            )
                        }
                        initInterests(it.userPrivate.interests)
                        binding.horizontalTagsContainer.visibility = View.VISIBLE
                    } else {
                        binding.horizontalTagsContainer.visibility = View.GONE
                        binding.accountImg.setOnClickListener {
                            findNavController().navigate(R.id.signInFragment, null, options)
                        }
                    }
                }*/
            },
            onEmptySet = {
                showEmptyListUI()
            },
            onNonEmptySet = {
                showNonEmptyListUI()
            }
        )

        viewModel.user.observe(viewLifecycleOwner) { currentUser ->
            // getting posts when the user is signed in
            Log.d(TAG, "User observer is invoked.")

            if (cachedUser == null || (currentUser?.name != cachedUser?.name) ||
                (currentUser?.username != cachedUser?.username) ||
                (currentUser?.photo != cachedUser?.photo)) {

                getPosts()

            }

            if (currentUser != null) {
                cachedUser = currentUser
                binding.accountImg.setImageURI(currentUser.photo)
                initInterests(currentUser.userPrivate.interests)
                binding.horizontalTagsContainer.visibility = View.VISIBLE
                binding.accountImg.setOnClickListener {
                    findNavController().navigate(
                        R.id.profileFragment,
                        Bundle().apply { putParcelable(ProfileFragment.ARG_USER, currentUser) },
                        options
                    )
                }
            } else {
                binding.horizontalTagsContainer.visibility = View.GONE
                binding.accountImg.setOnClickListener {
                    findNavController().navigate(R.id.signInFragment, null, options)
                }
            }
        }



        binding.postsRefresher.setSwipeRefresher {
            Log.d(TAG, "Refreshing posts")
            getPosts(viewModel.currentHomeTag.value)
            stopRefreshProgress(it)
        }

        // this should not get fired at start
        viewModel.currentHomeTag.observe(viewLifecycleOwner) {
            Log.d(TAG, "Current Home Tag observer called")
            getPosts(it)
        }


        binding.createNewButton.setOnClickListener {
            val _tag = CREATE_MENU
            val item1 = GenericMenuItem(_tag, "Create new Blog", R.drawable.ic_baseline_note_24, 0)
            val item2 = GenericMenuItem(
                _tag,
                "Create new Project",
                R.drawable.ic_baseline_architecture_24,
                1
            )
            val fragment =
                GenericMenuFragment.newInstance(_tag, "Create New ...", arrayListOf(item1, item2))
            activity.showBottomSheet(fragment, _tag)
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            binding.postsFragmentToolbar.updateLayout(
                marginTop = top,
                marginBottom = convertDpToPx(8),
                marginLeft = convertDpToPx(8),
                marginRight = convertDpToPx(8)
            )
            binding.accountImg.updateLayout(
                marginTop = top + convertDpToPx(10),
                marginRight = convertDpToPx(16)
            )
            binding.createNewButton.updateLayout(
                marginTop = top + convertDpToPx(4),
                marginLeft = convertDpToPx(12)
            )
            binding.projectsFragmentList.setPadding(
                0,
                convertDpToPx(8),
                0,
                bottom + convertDpToPx(56)
            )
        }

        binding.homeInterestsGroup.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == R.id.first_chip) {
                viewModel.setCurrentHomeTag(null)
            } else {
                val chip = group.findViewById<Chip>(checkedId)
                viewModel.setCurrentHomeTag(chip?.text?.toString())
            }
        }

    }

    private fun initInterests(interests: List<String>) {
        interests.forEachIndexed { index, s ->
            addChip(index, s)
        }
    }

    private fun addChip(index: Int, interest: String) {
        val chip = LayoutInflater.from(activity).inflate(R.layout.chip, null) as Chip
        chip.text = interest
        chip.id = index + 1
        chip.isCheckable = true
        chip.isCheckedIconVisible = false

        chip.setOnClickListener {
            emptyPostsRetryCount = 0
            chip.isChecked = true
        }

        binding.homeInterestsGroup.addView(chip)
    }

    private fun showEmptyListUI() {
        binding.postsRefresher.visibility = View.GONE
        emptyPostsRetryCount++
        val currentTag = viewModel.currentHomeTag.value
        if (currentTag == null && emptyPostsRetryCount < 2) {
            getPosts()
        } else {
            if (errorView != null) {
                binding.postsFragmentRoot.removeView(errorView)
                errorView = null
            }
            errorView = setErrorLayout(
                binding.postsFragmentRoot,
                "No posts at the moment.\nTry again later.",
                dependantView = binding.postsAppBar,
                errorActionLabel = "Try again"
            ) { b, p ->
                getPosts()
            }.root
        }
    }

    private fun showNonEmptyListUI() {
        if (errorView != null) {
            binding.postsFragmentRoot.removeView(errorView)
        }
        binding.postsRefresher.visibility = View.VISIBLE
    }


    companion object {

        private const val TAG = "PostsFragment"

        @JvmStatic
        fun newInstance() = PostsFragment()
    }

}

/*if (!sharedPreference.getBoolean(IS_FIRST_TIME, true)) {
		  binding.postsRefresher.isRefreshing = true
		  getPosts()
	  } else {
		  getPosts()
		  viewLifecycleOwner.lifecycleScope.launchWhenCreated {
			  delay(2000)
			  val editor = sharedPreference.edit()
			  editor.putBoolean(IS_FIRST_TIME, false)
			  editor.apply()
			  if (!binding.postsRefresher.isRefreshing) {
				  postAdapter.refresh()
			  }
		  }
	  }*/