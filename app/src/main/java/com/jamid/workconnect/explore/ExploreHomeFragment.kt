package com.jamid.workconnect.explore

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.jamid.workconnect.*
import com.jamid.workconnect.adapter.paging2.ExploreAdapter
import com.jamid.workconnect.databinding.FragmentExploreHomeBinding
import com.jamid.workconnect.model.*
import com.jamid.workconnect.profile.ProfileFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

class ExploreHomeFragment : InsetControlFragment(R.layout.fragment_explore_home) {

    private lateinit var binding: FragmentExploreHomeBinding
    private var job: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentExploreHomeBinding.bind(view)

        val options = navOptions {
            anim {
                enter = R.anim.slide_in_right
                exit = R.anim.slide_out_left
                popEnter = R.anim.slide_in_left
                popExit = R.anim.slide_out_right
            }
        }

        job = initAdapter()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (activity.resources?.configuration?.isNightModeActive == true) {
                binding.exploreRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.purple_200))
                binding.exploreRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.darkestGrey))
            } else {
                binding.exploreRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.blue_500))
                binding.exploreRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.white))
            }
        } else {
            if (activity.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
                binding.exploreRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.darkerGrey))
                binding.exploreRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.purple_200))
            } else {
                binding.exploreRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.blue_500))
                binding.exploreRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.white))
            }
        }

        binding.exploreRefresher.setOnRefreshListener {
            job?.cancel()
            job = initAdapter()
            viewLifecycleOwner.lifecycleScope.launchWhenCreated {
                delay(4000)
                binding.exploreRefresher.isRefreshing = false
            }
        }

        binding.searchEditText.setOnClickListener {
            findNavController().navigate(R.id.searchFragment)
        }

        binding.exploreAppBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            when (abs(verticalOffset)) {
                0 -> {
                    binding.searchButton.visibility = View.GONE
                }
                appBarLayout.totalScrollRange -> {
                    binding.searchButton.visibility = View.VISIBLE
                }
                else -> {
                    binding.searchButton.visibility = View.GONE
                }
            }
        })

        binding.searchButton.setOnClickListener {
            findNavController().navigate(R.id.searchFragment)
        }

        viewModel.user.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.accountImg.setImageURI(it.photo)
                binding.accountImg.setOnClickListener { _ ->
                    findNavController().navigate(R.id.profileFragment, Bundle().apply { putParcelable(ProfileFragment.ARG_USER, it) }, options)
                }
            } else {
                binding.accountImg.setOnClickListener {
                    findNavController().navigate(R.id.signInFragment, null, options)
                }
            }
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            binding.searchButton.updateLayout(marginTop = top + convertDpToPx(10), marginLeft = convertDpToPx(16))
            binding.exploreToolbar.updateLayout(marginTop = top)
            binding.accountImg.updateLayout(marginTop = top + convertDpToPx(10), marginRight = convertDpToPx(16))
            binding.exploreRecycler.setPadding(0, convertDpToPx(8), 0, bottom + convertDpToPx(56))
        }

    }

    private fun initAdapter() = lifecycleScope.launch {

        val post1 = Post()
        post1.type = PROJECT

        val post2 = Post()
        post2.type = BLOG

        val exploreAdapter = ExploreAdapter(listOf(post1, post2, UserMinimal(), TagsHolder()))

        binding.exploreRecycler.apply {
            adapter = exploreAdapter
            layoutManager = LinearLayoutManager(activity)
        }

        when (val topProjectsResult = viewModel.topProjects()) {
            is Result.Error -> {
                Toast.makeText(activity, topProjectsResult.exception.localizedMessage, Toast.LENGTH_LONG).show()
            }
            is Result.Success -> {
                val topProjectsSnapshot = topProjectsResult.data
                val topProjects = viewModel.filterPosts(topProjectsSnapshot.toObjects(Post::class.java))
                exploreAdapter.projectsAdapterHorizontal?.submitList(topProjects)
            }
        }

        when (val topBlogsResult = viewModel.topBlogs()) {
            is Result.Error -> {
                Toast.makeText(activity, topBlogsResult.exception.localizedMessage, Toast.LENGTH_LONG).show()
            }
            is Result.Success -> {
                val topBlogsSnapshot = topBlogsResult.data
                val topBlogs = viewModel.filterPosts(topBlogsSnapshot.toObjects(Post::class.java))
                exploreAdapter.blogsAdapter?.submitList(topBlogs)
            }
        }


        when (val usersSnapshotResult = viewModel.randomTopUsers()) {
            is Result.Success -> {
                val snapshot = usersSnapshotResult.data
                val users = viewModel.filterUsers(snapshot.toObjects(User::class.java))

                exploreAdapter.userAdapter?.submitList(users.filter { !it.isCurrentUser })
            }
            is Result.Error -> {
                Toast.makeText(activity, usersSnapshotResult.exception.localizedMessage, Toast.LENGTH_LONG).show()
            }
        }


        viewModel.user.observe(viewLifecycleOwner) {
            val tags = listOf(
                "Artificial Intelligence",
                "Science",
                "Android",
                "Vector",
                "Computer Science",
                "Machine Learning",
                "Google",
                "Projects",
                "Physics",
                "Chemistry",
                "iOS Development",
                "Github",
                "Culinary",
                "Fashion Technology",
                "Neural Network",
                "Deep Learning",
                "Cryptocurrency",
                "Blockchain"
            )

            val finalList = if (it != null) {
                tags.filter { tag ->
                    !it.userPrivate.interests.contains(tag)
                }.distinct().shuffled()
            } else {
                tags.distinct().shuffled()
            }

            val tagsHolder = TagsHolder(finalList)
            val tagsHolderList = listOf(tagsHolder)

            exploreAdapter.tagsAdapter?.submitList(tagsHolderList)

        }

    }

    companion object {

        @JvmStatic
        fun newInstance() =
            ExploreHomeFragment()
    }

}