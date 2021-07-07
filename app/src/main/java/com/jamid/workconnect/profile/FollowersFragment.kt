package com.jamid.workconnect.profile

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.jamid.workconnect.PagingListFragment
import com.jamid.workconnect.R
import com.jamid.workconnect.adapter.paging2.UserAdapter
import com.jamid.workconnect.convertDpToPx
import com.jamid.workconnect.databinding.FragmentFollowersBinding
import com.jamid.workconnect.model.User
import com.jamid.workconnect.updateLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(androidx.paging.ExperimentalPagingApi::class)
class FollowersFragment : PagingListFragment(R.layout.fragment_followers) {

    private lateinit var binding: FragmentFollowersBinding
    private lateinit var userAdapter: UserAdapter

    private fun getFollowers(user: User, query: String? = null) {
        job?.cancel()
        job = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.userFollowers(user, query).collectLatest {
                userAdapter.submitData(it)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentFollowersBinding.bind(view)
        userAdapter = UserAdapter()
        binding.followersMotionLayout.transitionToEnd()

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            binding.followersMotionLayout.updateLayout(marginTop = top)
            if (activity.mainBinding.bottomCard.translationY != 0f) {
                binding.followersRecycler.setPadding(0, 0, 0, bottom)
            } else {
                binding.followersRecycler.setPadding(0, 0, 0, bottom + convertDpToPx(56))
            }
        }
        val user = arguments?.getParcelable<User>(ARG_USER) ?: return

        binding.cancelSearchBtn.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.searchBarText.doAfterTextChanged {
            if (!it.isNullOrBlank()) {
                getFollowers(user, it.toString())
            } else {
                getFollowers(user)
            }
        }

        binding.followersRecycler.setListAdapter(
            pagingAdapter = userAdapter,
            clazz = User::class.java,
            onComplete = {
                getFollowers(user)
            })

        binding.followersRefresher.setSwipeRefresher {
            val t = binding.searchBarText.text
            if (t.isNullOrBlank()) {
                getFollowers(user)
            } else {
                getFollowers(user, t.toString())
            }
            stopRefreshProgress(it)
        }
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