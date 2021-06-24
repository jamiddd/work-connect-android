package com.jamid.workconnect.home

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.jamid.workconnect.*
import com.jamid.workconnect.adapter.paging3.PostAdapter
import com.jamid.workconnect.adapter.paging3.PostsLoadStateAdapter
import com.jamid.workconnect.databinding.FragmentProjectsBinding
import com.jamid.workconnect.model.GenericMenuItem
import com.jamid.workconnect.profile.ProfileFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

@OptIn(androidx.paging.ExperimentalPagingApi::class)
class PostsFragment : InsetControlFragment(R.layout.fragment_projects) {

    private lateinit var binding: FragmentProjectsBinding
    private lateinit var postAdapter: PostAdapter
    private var job: Job? = null

    fun getPosts(tag: String? = null) {
        job?.cancel()
        job = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.postsFlow(tag).collect {
                postAdapter.submitData(it)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentProjectsBinding.bind(view)

        activity.currentFeedFragment = this

        initAdapter()

        initRefresher()

        val options = navOptions {
            anim {
                enter = R.anim.slide_in_right
                exit = R.anim.slide_out_left
                popEnter = R.anim.slide_in_left
                popExit = R.anim.slide_out_right
            }
        }

        // this should not get fired at start
        viewModel.currentHomeTag.observe(viewLifecycleOwner) {
            Log.d(BUG_TAG, "Getting posts from current home tag")
            getPosts(it)
        }


        binding.createNewButton.setOnClickListener {
            val _tag = CREATE_MENU
            val item1 = GenericMenuItem(_tag, "Create new Blog", R.drawable.ic_baseline_note_24, 0)
            val item2 = GenericMenuItem(_tag, "Create new Project", R.drawable.ic_baseline_architecture_24, 1)
            val fragment = GenericMenuFragment.newInstance(_tag, "Create New ...", arrayListOf(item1, item2))
            activity.showBottomSheet(fragment, _tag)
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            binding.postsFragmentToolbar.updateLayout(marginTop = top, marginBottom = convertDpToPx(8), marginLeft = convertDpToPx(8), marginRight = convertDpToPx(8))
            binding.accountImg.updateLayout(marginTop = top + convertDpToPx(10), marginRight = convertDpToPx(16))
            binding.createNewButton.updateLayout(marginTop = top + convertDpToPx(4), marginLeft = convertDpToPx(16))
            binding.projectsFragmentList.setPadding(0, convertDpToPx(8), 0, bottom + convertDpToPx(56))
        }

        viewModel.user.observe(viewLifecycleOwner) {
            getPosts(viewModel.currentHomeTag.value)
            if (it != null) {
                binding.accountImg.setImageURI(it.photo)
                binding.horizontalTagsContainer.visibility = View.VISIBLE
                binding.postsAppBar.updateLayout(convertDpToPx(218))
                binding.accountImg.setOnClickListener {
                    findNavController().navigate(R.id.profileFragment, Bundle().apply { putParcelable(ProfileFragment.ARG_USER, viewModel.user.value)}, options)
                }
                initInterests(it.userPrivate.interests)
            } else {
                binding.horizontalTagsContainer.visibility = View.GONE
                binding.postsAppBar.updateLayout(convertDpToPx(170))
                binding.accountImg.setOnClickListener {
                    findNavController().navigate(R.id.signInFragment, null, options)
                }
            }
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
            chip.isChecked = true
        }

        binding.homeInterestsGroup.addView(chip)
    }

    private fun initAdapter() {

        postAdapter = PostAdapter()
        activity.currentAdapter = postAdapter

        binding.projectsFragmentList.apply {
            setRecycledViewPool(activity.recyclerViewPool)
            adapter = postAdapter.withLoadStateFooter(
                footer = PostsLoadStateAdapter(postAdapter)
            )
            itemAnimator = null
            layoutManager = LinearLayoutManager(activity)
        }
    }

    private fun initRefresher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (activity.resources?.configuration?.isNightModeActive == true) {
                binding.postsRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.darkestGrey))
                binding.postsRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.purple_200))
            } else {
                binding.postsRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.white))
                binding.postsRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.blue_500))
            }
        } else {
            if (activity.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
                binding.postsRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.darkerGrey))
                binding.postsRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.purple_200))
            } else {
                binding.postsRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.white))
                binding.postsRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.blue_500))
            }
        }

        binding.postsRefresher.setOnRefreshListener {
            getPosts(viewModel.currentHomeTag.value)
            hideProgressBar()
        }

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {

            postAdapter.loadStateFlow.collectLatest { loadStates ->
                binding.postsRefresher.isRefreshing = false

                /*if (loadStates.refresh is LoadState.Loading) {
                    binding.postsRefresher.isRefreshing = false
                }

                if (loadStates.append is LoadState.Loading) {
                    binding.postsRefresher.isRefreshing = true
                }*/

                if (loadStates.refresh is LoadState.NotLoading || loadStates.append is LoadState.NotLoading || loadStates.prepend is LoadState.NotLoading) {
                    delay(1000)
//                    binding.postsRefresher.isRefreshing = false

                    if (postAdapter.itemCount == 0) {
                        val currentTag = viewModel.currentHomeTag.value
                        if (currentTag == null) {
                            getPosts()
                        }
                    } else {
                        binding.projectsFragmentList.visibility = View.VISIBLE
                    }
                }
            }

            postAdapter.loadStateFlow.distinctUntilChangedBy { it.refresh }
                .filter { it.refresh is LoadState.NotLoading }
                .collect { binding.projectsFragmentList.scrollToPosition(0) }

        }
    }

    private fun hideProgressBar()  = viewLifecycleOwner.lifecycleScope.launch {
        delay(1000)
        binding.postsRefresher.isRefreshing = false
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