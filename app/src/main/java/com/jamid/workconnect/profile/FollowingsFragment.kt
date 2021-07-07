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
import com.jamid.workconnect.databinding.FragmentFollowingsBinding
import com.jamid.workconnect.model.User
import com.jamid.workconnect.updateLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(androidx.paging.ExperimentalPagingApi::class)
class FollowingsFragment : PagingListFragment(R.layout.fragment_followings) {

    private lateinit var binding: FragmentFollowingsBinding
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

        binding.followingsMotionLayout.transitionToEnd()

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            binding.followingsMotionLayout.updateLayout(marginTop = top)
            if (activity.mainBinding.bottomCard.translationY != 0f) {
                binding.followingsRecycler.setPadding(0, 0, 0, bottom)
            } else {
                binding.followingsRecycler.setPadding(0, 0, 0, bottom + convertDpToPx(56))
            }
        }

        val user = arguments?.getParcelable<User>(ARG_USER) ?: return

        binding.cancelSearchBtn.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.searchBarText.doAfterTextChanged {
            if (!it.isNullOrBlank()) {
                getFollowings(user, it.toString())
            } else {
                getFollowings(user)
            }
        }

        userAdapter = UserAdapter()

        binding.followingsRecycler.setListAdapter(
            pagingAdapter = userAdapter,
            clazz = User::class.java,
            onComplete = {
                getFollowings(user)
            })

        binding.followingsRefresher.setSwipeRefresher {
            val t = binding.searchBarText.text
            if (t.isNullOrBlank()) {
                getFollowings(user)
            } else {
                getFollowings(user, t.toString())
            }
            stopRefreshProgress(it)
        }
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