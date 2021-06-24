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
import com.jamid.workconnect.FOLLOWINGS
import com.jamid.workconnect.R
import com.jamid.workconnect.SupportFragment
import com.jamid.workconnect.adapter.paging2.UserAdapter
import com.jamid.workconnect.databinding.FragmentFollowingsBinding
import com.jamid.workconnect.model.User
import com.jamid.workconnect.updateLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(androidx.paging.ExperimentalPagingApi::class)
class FollowingsFragment : SupportFragment(R.layout.fragment_followings, TAG, false) {

    private lateinit var binding: FragmentFollowingsBinding
    private var textWatcher: TextWatcher? = null

    private var job: Job? = null
    private lateinit var userAdapter: UserAdapter

    private fun getFollowings(user: User, query: String? = null) {
        job?.cancel()
        job = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.userFollowings(user, query).collectLatest {
                userAdapter.submitData(it)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentFollowingsBinding.bind(view)

        /*val insets = viewModel.windowInsets.value
        if (insets != null) {
            binding.followingsRecycler.setPadding(0, 0, 0, insets.second)
        }
*/
//        val currentUser = viewModel.user.value

        binding.followingsMotionLayout.transitionToEnd()

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, _) ->
            binding.followingsMotionLayout.updateLayout(marginTop = top)
        }

        val user = arguments?.getParcelable<User>(ARG_USER)
        if (user != null) {

            binding.cancelSearchBtn.setOnClickListener {
                findNavController().navigateUp()
            }

            binding.searchBarText.doAfterTextChanged {
                if (!it.isNullOrBlank()) {
                    getFollowings(user, it.toString())
                }
            }

            initAdapter(user)

        } else {
            Log.d(FOLLOWINGS, "Hello")
        }
    }

    private fun initAdapter(user: User, query: String? = null) = viewLifecycleOwner.lifecycleScope.launch {

        userAdapter = UserAdapter()

        getFollowings(user, query)

        binding.followingsRecycler.apply {
            adapter = userAdapter
            layoutManager = LinearLayoutManager(activity)
        }

        binding.followingsRefresher.setOnRefreshListener {
            userAdapter.refresh()
            viewLifecycleOwner.lifecycleScope.launch {
                delay(4000)
                binding.followingsRefresher.isRefreshing = false
            }

        }

        userAdapter.loadStateFlow.collectLatest { loadStates ->
            binding.followingsRefresher.isRefreshing = loadStates.refresh is LoadState.Loading
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        /*activity.mainBinding.primarySearchBar.removeTextChangedListener(textWatcher)
        activity.mainBinding.primarySearchBar.text.clear()*/
    }

    companion object {

        const val TAG = "FollowingsFragment"
        const val TITLE = "Followings"
        const val ARG_USER = "ARG_USER"

        @JvmStatic
        fun newInstance(user: User) = FollowingsFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_USER, user)
            }
        }
    }
}