package com.jamid.workconnect.explore

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.filter
import com.jamid.workconnect.PagingListFragment
import com.jamid.workconnect.R
import com.jamid.workconnect.adapter.paging3.UserAdapter
import com.jamid.workconnect.convertDpToPx
import com.jamid.workconnect.databinding.FragmentRandomUsersBinding
import com.jamid.workconnect.model.User
import com.jamid.workconnect.updateLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RandomUsersFragment : PagingListFragment(R.layout.fragment_random_users) {

    private lateinit var binding: FragmentRandomUsersBinding
    private lateinit var userAdapter: UserAdapter

    private fun getRandomUsers() {
        job?.cancel()
        job = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getRandomUsers().collectLatest {
                userAdapter.submitData(it.filter { user ->  user.id != viewModel.user.value?.id })
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentRandomUsersBinding.bind(view)

        binding.randomUsersToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        userAdapter = UserAdapter()
        binding.randomUsersRecycler.setListAdapter(
            pagingAdapter = userAdapter,
            clazz = User::class.java,
            onComplete = {
                getRandomUsers()
            })

        binding.randomUsersRefresher.setSwipeRefresher {
            getRandomUsers()
            stopRefreshProgress(it)
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            binding.randomUsersToolbar.updateLayout(marginTop = top)
            binding.randomUsersRecycler.setPadding(
                0,
                convertDpToPx(8),
                0,
                bottom + convertDpToPx(56)
            )
        }

    }


    companion object {

        const val TAG = "RandomUsersFragment"
        const val TITLE = "Users to follow"

        @JvmStatic
        fun newInstance() =
            RandomUsersFragment()
    }
}