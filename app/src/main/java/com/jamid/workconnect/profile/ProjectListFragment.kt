package com.jamid.workconnect.profile

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.workconnect.InsetControlFragment
import com.jamid.workconnect.R
import com.jamid.workconnect.adapter.paging2.PostAdapter
import com.jamid.workconnect.databinding.FragmentProjectListBinding
import com.jamid.workconnect.home.CreateProjectFragment
import com.jamid.workconnect.model.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class ProjectListFragment : InsetControlFragment(R.layout.fragment_project_list) {

    private lateinit var postAdapter: PostAdapter
    private lateinit var binding: FragmentProjectListBinding
    private var job: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentProjectListBinding.bind(view)
        val user = arguments?.getParcelable<User>(ARG_USER)

        if (user != null) {

            postAdapter = PostAdapter()

            binding.projectsRecycler.apply {
                adapter = postAdapter
                itemAnimator = null
                layoutManager = LinearLayoutManager(activity)
            }

            OverScrollDecoratorHelper.setUpOverScroll(binding.projectsRecycler, OverScrollDecoratorHelper.ORIENTATION_VERTICAL)

            if (user.id == viewModel.user.value?.id) {
                binding.noUserPostsCreatePost.visibility = View.VISIBLE
            } else {
                binding.noUserPostsCreatePost.visibility = View.GONE
            }

            binding.noUserPostsCreatePost.setOnClickListener {
                activity.toFragment(CreateProjectFragment.newInstance(), CreateProjectFragment.TAG)
            }

            viewModel.userProjects(user.id).observe(viewLifecycleOwner) {
                if (it.isNotEmpty()) {
                    job?.cancel()
                    activity.mainBinding.primaryProgressBar.visibility = View.GONE
                    binding.projectsRecycler.visibility = View.VISIBLE
                    binding.noUserPostsLayoutScroll.visibility = View.GONE
                    postAdapter.submitList(it)
                } else {
                    activity.mainBinding.primaryProgressBar.visibility = View.VISIBLE
                    job = lifecycleScope.launch {
                        delay(2000)
                        activity.mainBinding.primaryProgressBar.visibility = View.GONE
                        binding.projectsRecycler.visibility = View.GONE
                        OverScrollDecoratorHelper.setUpOverScroll(binding.noUserPostsLayoutScroll)
                        binding.noUserPostsLayoutScroll.visibility = View.VISIBLE
                    }
                }
            }
        }
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