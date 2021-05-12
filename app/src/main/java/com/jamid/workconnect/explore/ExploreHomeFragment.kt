package com.jamid.workconnect.explore

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.workconnect.*
import com.jamid.workconnect.adapter.paging2.ExploreAdapter
import com.jamid.workconnect.databinding.FragmentExploreHomeBinding
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.Result
import com.jamid.workconnect.model.User
import com.jamid.workconnect.model.UserMinimal
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ExploreHomeFragment : InsetControlFragment(R.layout.fragment_explore_home) {

    private lateinit var binding: FragmentExploreHomeBinding
    private var job: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentExploreHomeBinding.bind(view)
        setInsetView(binding.exploreRecycler, mapOf(INSET_TOP to 56, INSET_BOTTOM to 56))

        binding.exploreRefresher.setProgressViewOffset(false, 0, viewModel.windowInsets.value!!.first + convertDpToPx(64))
        binding.exploreRefresher.setSlingshotDistance(convertDpToPx(54))
        binding.exploreRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.blue_500))

        job = initAdapter()

        binding.exploreRefresher.setOnRefreshListener {
            job?.cancel()
            job = initAdapter()
            viewLifecycleOwner.lifecycleScope.launchWhenCreated {
                delay(4000)
                binding.exploreRefresher.isRefreshing = false
            }
        }

    }

    private fun initAdapter() = lifecycleScope.launch {

        val post1 = Post()
        post1.type = PROJECT

        val post2 = Post()
        post2.type = BLOG

        val exploreAdapter = ExploreAdapter(listOf(post1, post2, UserMinimal()), activity)

        binding.exploreRecycler.apply {
            adapter = exploreAdapter
            layoutManager = LinearLayoutManager(activity)
        }

//        setOverScrollView(binding.exploreRecycler)

        viewModel.projects().observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                exploreAdapter.projectsAdapterHorizontal?.submitList(it)
            }
        }

        viewModel.blogs().observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                exploreAdapter.blogsAdapterHorizontal?.submitList(it)
            }
        }

        when (val usersSnapshotResult = viewModel.randomTopUsers()) {
            is Result.Success -> {
                val snapshot = usersSnapshotResult.data
                val users = snapshot.toObjects(User::class.java)

                exploreAdapter.userAdapter?.submitList(users)

            }
            is Result.Error -> {
                Toast.makeText(activity, usersSnapshotResult.exception.localizedMessage, Toast.LENGTH_LONG).show()
            }
        }

    }

    companion object {

        @JvmStatic
        fun newInstance() =
            ExploreHomeFragment()
    }

}