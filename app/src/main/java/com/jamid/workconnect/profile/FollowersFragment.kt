package com.jamid.workconnect.profile

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.workconnect.FOLLOWERS
import com.jamid.workconnect.R
import com.jamid.workconnect.SupportFragment
import com.jamid.workconnect.adapter.paging2.UserAdapter
import com.jamid.workconnect.databinding.FragmentFollowersBinding
import com.jamid.workconnect.model.User
import com.jamid.workconnect.updateLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(androidx.paging.ExperimentalPagingApi::class)
class FollowersFragment : SupportFragment(R.layout.fragment_followers, TAG, false) {

    private lateinit var binding: FragmentFollowersBinding
    private var job: Job? = null
    private lateinit var userAdapter: UserAdapter

    private fun getFollowers(user: User, query: String? = null) {
        Log.d(FOLLOWERS, "Cancelling prev job ..")
        job?.cancel()
        job = viewLifecycleOwner.lifecycleScope.launch {
            Log.d(FOLLOWERS, "Starting coroutines")
            viewModel.userFollowers(user, query).collectLatest {
                userAdapter.submitData(it)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d(FOLLOWERS, "Started followers fragment")

        binding = FragmentFollowersBinding.bind(view)

        binding.followersMotionLayout.transitionToEnd()

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, _) ->
            binding.followersMotionLayout.updateLayout(marginTop = top)
        }

        val user = arguments?.getParcelable<User>(ARG_USER)
        Log.d(FOLLOWERS, "After getting user ..")

        if (user != null) {

            binding.cancelSearchBtn.setOnClickListener {
                findNavController().navigateUp()
            }

            binding.searchBarText.doAfterTextChanged {
                if (!it.isNullOrBlank()) {
                    getFollowers(user, it.toString())
                }
            }

            initAdapter(user)

        } else {
            Log.d(FOLLOWERS, "User is null")
        }


    }

    private fun initAdapter(user: User, query: String? = null) = viewLifecycleOwner.lifecycleScope.launch {

        userAdapter = UserAdapter()
        Log.d(FOLLOWERS, "Initiating adapter ... ")

        getFollowers(user, query)

        binding.followersRecycler.apply {
            adapter = userAdapter
            layoutManager = LinearLayoutManager(activity)
        }


        binding.followersRefresher.setOnRefreshListener {
            userAdapter.refresh()
            viewLifecycleOwner.lifecycleScope.launch {
                delay(4000)
                binding.followersRefresher.isRefreshing = false
            }
        }

        userAdapter.loadStateFlow.collectLatest { loadStates ->
            binding.followersRefresher.isRefreshing = loadStates.refresh is LoadState.Loading
        }


    }

    /*private fun setAdapter(user: User, currentUser: User?, query: String) = viewLifecycleOwner.lifecycleScope.launchWhenCreated {
        val userAdapter = UserAdapter()

        binding.followersRecycler.apply {
            adapter = userAdapter
            layoutManager = LinearLayoutManager(activity)
        }


        userAdapter.loadStateFlow.collectLatest { loadStates ->
            binding.followersRefresher.isRefreshing = loadStates.refresh is LoadState.Loading
        }

        binding.followersRefresher.setOnRefreshListener {
            userAdapter.refresh()
            viewLifecycleOwner.lifecycleScope.launchWhenCreated {
                delay(4000)
                binding.followersRefresher.isRefreshing = false
            }

        }

        viewModel.userFollowers(user, query).collectLatest {
            userAdapter.submitData(it)
        }
    }*/

    override fun onDestroyView() {
        super.onDestroyView()
       /* activity.mainBinding.primarySearchBar.removeTextChangedListener(textWatcher)
        activity.mainBinding.primarySearchBar.text.clear()*/
    }


    companion object {

        const val TAG = "FollowersFragment"
        const val ARG_USER = "ARG_USER"
        const val TITLE = "Followers"

        @JvmStatic
        fun newInstance(user: User) = FollowersFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_USER, user)
            }
        }
    }
}