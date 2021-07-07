package com.jamid.workconnect.profile

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.workconnect.*
import com.jamid.workconnect.adapter.paging3.PostAdapter
import com.jamid.workconnect.databinding.ErrorNoOutputLayoutBinding
import com.jamid.workconnect.databinding.FragmentProjectListBinding
import com.jamid.workconnect.home.CreateProjectFragment
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(androidx.paging.ExperimentalPagingApi::class)
class ProjectListFragment : PagingListFragment(R.layout.fragment_project_list) {

    private lateinit var postAdapter: PostAdapter
    private lateinit var binding: FragmentProjectListBinding
    private var errorView: View? = null
    private lateinit var user: User

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
        user = arguments?.getParcelable(ARG_USER) ?: return
        binding.projectsListRefresher.isRefreshing = true

        postAdapter = PostAdapter()
        binding.projectsRecycler.setListAdapter(pagingAdapter = postAdapter, clazz = Post::class.java,
        onComplete = {
            getUserProjects(user)
        },
        onEmptySet = {
            binding.projectsListRefresher.isRefreshing = false
            showEmptyNotificationsUI()
        }, onNonEmptySet = {
            binding.projectsListRefresher.isRefreshing = false
            hideEmptyNotificationsUI()
        })

        binding.projectsListRefresher.setSwipeRefresher {
            getUserProjects(user)
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (_, bottom) ->
            if (activity.mainBinding.bottomCard.translationY != 0f) {
                binding.projectsRecycler.setPadding(0, convertDpToPx(8), 0, bottom)
            } else {
                binding.projectsRecycler.setPadding(0, convertDpToPx(8), 0, bottom + convertDpToPx(56))
            }
        }

    }

    private fun hideEmptyNotificationsUI() {
        if (errorView != null) {
            binding.projectListRoot.removeView(errorView)
        }
        binding.projectsListRefresher.visibility = View.VISIBLE
    }

    private fun showEmptyNotificationsUI() {
        binding.projectsListRefresher.visibility = View.GONE

        if (errorView != null) {
            binding.projectListRoot.removeView(errorView)
            errorView = null
        }

        val errorViewBinding = if (viewModel.user.value != null) {
            if (user.id == viewModel.user.value!!.id) {
                setErrorLayout(binding.projectListRoot, "No projects yet.\nYour projects will show up here.", errorActionLabel = "Create Project", margin = convertDpToPx(120)) { b, p ->
                    b.visibility = View.VISIBLE
                    p.visibility = View.GONE
                    findNavController().navigate(R.id.createProjectFragment, null, options)
                }
            } else {
                setErrorLayout(binding.projectListRoot, "No projects yet.", errorActionEnabled = false, margin = convertDpToPx(120))
            }
        } else {
            setErrorLayout(binding.projectListRoot, "No projects yet.\nYour projects will show up here.", errorActionEnabled = false, margin = convertDpToPx(120))
        }

        val errorViewParams = errorViewBinding.errorItemsContainer.layoutParams as FrameLayout.LayoutParams
        errorViewParams.gravity = Gravity.TOP
        errorViewBinding.errorItemsContainer.layoutParams = errorViewParams

        errorView = errorViewBinding.root

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