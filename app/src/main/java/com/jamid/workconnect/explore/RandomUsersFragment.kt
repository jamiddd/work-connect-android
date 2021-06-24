package com.jamid.workconnect.explore

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.workconnect.R
import com.jamid.workconnect.SupportFragment
import com.jamid.workconnect.adapter.paging3.UserAdapter
import com.jamid.workconnect.databinding.FragmentRandomUsersBinding
import com.jamid.workconnect.updateLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class RandomUsersFragment : SupportFragment(R.layout.fragment_random_users, TAG, false) {

    private lateinit var binding: FragmentRandomUsersBinding
    private var job: Job? = null
    private lateinit var userAdapter: UserAdapter

    private fun getRandomUsers() {
        job?.cancel()
        job = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getRandomUsers().collectLatest {
                userAdapter.submitData(it)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentRandomUsersBinding.bind(view)
        activity.currentFeedFragment = this

        binding.randomUsersToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        initAdapter()
        initRefresher()

        getRandomUsers()

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, _) ->
            binding.randomUsersToolbar.updateLayout(marginTop = top)
        }

    }

    private fun initAdapter() {

        userAdapter = UserAdapter()
        activity.currentAdapter = userAdapter

        binding.randomUsersRecycler.apply {
            adapter = userAdapter
            itemAnimator = null
            layoutManager = LinearLayoutManager(activity)
        }
    }


    private fun initRefresher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (activity.resources?.configuration?.isNightModeActive == true) {
                binding.randomUsersRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.darkestGrey))
                binding.randomUsersRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.purple_200))
            } else {
                binding.randomUsersRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.white))
                binding.randomUsersRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.blue_500))
            }
        } else {
            if (activity.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
                binding.randomUsersRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.darkerGrey))
                binding.randomUsersRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.purple_200))
            } else {
                binding.randomUsersRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.white))
                binding.randomUsersRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.blue_500))
            }
        }

        binding.randomUsersRefresher.setOnRefreshListener {
            getRandomUsers()
            hideProgressBar()
        }

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {

            userAdapter.loadStateFlow.collectLatest { loadStates ->
                binding.randomUsersRefresher.isRefreshing = loadStates.refresh is LoadState.Loading
            }

            userAdapter.loadStateFlow.distinctUntilChangedBy { it.refresh }
                .filter { it.refresh is LoadState.NotLoading }
                .collect { binding.randomUsersRecycler.scrollToPosition(0) }

        }
    }

    private fun hideProgressBar()  = viewLifecycleOwner.lifecycleScope.launch {
        delay(5000)
        binding.randomUsersRefresher.isRefreshing = false
    }

    companion object {

        const val TAG = "RandomUsersFragment"
        const val TITLE = "Users to follow"

        @JvmStatic
        fun newInstance() =
            RandomUsersFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }
}