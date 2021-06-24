package com.jamid.workconnect.profile

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.workconnect.InsetControlFragment
import com.jamid.workconnect.R
import com.jamid.workconnect.adapter.paging3.PostAdapter
import com.jamid.workconnect.databinding.FragmentProjectListBinding
import com.jamid.workconnect.home.CreateProjectFragment
import com.jamid.workconnect.model.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(androidx.paging.ExperimentalPagingApi::class)
class ProjectListFragment : InsetControlFragment(R.layout.fragment_project_list) {

    private lateinit var postAdapter: PostAdapter
    private lateinit var binding: FragmentProjectListBinding
    private var job: Job? = null

    private fun getUserProjects(user: User) {
        job?.cancel()
        job = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.otherUserProjectsFlow(user).collectLatest {
                postAdapter.submitData(it)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentProjectListBinding.bind(view)
        val user = arguments?.getParcelable<User>(ARG_USER)

        if (user != null) {

            initAdapter()

            if (user.id == viewModel.user.value?.id) {
                binding.noUserPostsCreatePost.visibility = View.VISIBLE
            } else {
                binding.noUserPostsText.text = "No projects"
                binding.noUserPostsCreatePost.visibility = View.GONE
            }

            val options = navOptions {
                anim {
                    enter = R.anim.slide_in_right
                    exit = R.anim.slide_out_left
                    popEnter = R.anim.slide_in_left
                    popExit = R.anim.slide_out_right
                }
            }

            binding.noUserPostsCreatePost.setOnClickListener {
//                activity.toFragment(CreateProjectFragment.newInstance(), CreateProjectFragment.TAG)
                findNavController().navigate(R.id.createProjectFragment, null, options)
            }

            setRefresher(user)

            getUserProjects(user)

        }
    }

    private fun initAdapter() {
        postAdapter = PostAdapter()

        binding.projectsRecycler.apply {
            adapter = postAdapter
            itemAnimator = null
            setRecycledViewPool(activity.recyclerViewPool)
            layoutManager = LinearLayoutManager(activity)
        }

    }

    private fun setRefresher(user: User) {
        binding.projectsListRefresher.setOnRefreshListener {
            getUserProjects(user)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            postAdapter.loadStateFlow.collectLatest { loadStates ->
                binding.projectsListRefresher.isRefreshing = true
                if (loadStates.refresh is LoadState.NotLoading) {
                    delay(1000)
                    binding.projectsListRefresher.isRefreshing = false
                    if (postAdapter.itemCount == 0) {
                        showEmptyNotificationsUI()
                    } else {
                        hideEmptyNotificationsUI()
                    }
                }
            }
        }
    }

    private fun hideEmptyNotificationsUI() {
        binding.projectsListRefresher.visibility = View.VISIBLE
        binding.noUserPostsLayoutScroll.visibility = View.GONE
    }

    private fun showEmptyNotificationsUI() {
        binding.projectsListRefresher.visibility = View.GONE
        binding.noUserPostsLayoutScroll.visibility = View.VISIBLE
    }

    companion object {

        private const val ARG_USER = "ARG_USER"

        @JvmStatic
        fun newInstance(user: User) = ProjectListFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_USER, user)
            }
        }
    }

}