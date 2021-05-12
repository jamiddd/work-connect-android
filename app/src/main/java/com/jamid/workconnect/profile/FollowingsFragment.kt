package com.jamid.workconnect.profile

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.workconnect.R
import com.jamid.workconnect.SupportFragment
import com.jamid.workconnect.adapter.paging2.UserAdapter
import com.jamid.workconnect.convertDpToPx
import com.jamid.workconnect.databinding.FragmentFollowingsBinding
import com.jamid.workconnect.model.User
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class FollowingsFragment : SupportFragment(R.layout.fragment_followings, TAG, false) {

    private lateinit var binding: FragmentFollowingsBinding
    private lateinit var userAdapter: UserAdapter
    private var textWatcher: TextWatcher? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentFollowingsBinding.bind(view)

        val insets = viewModel.windowInsets.value
        if (insets != null) {
            binding.followingsRecycler.setPadding(0, convertDpToPx(120) + insets.first, 0, insets.second)
        }

        userAdapter = UserAdapter(activity)
        val currentUser = viewModel.user.value

        val user = arguments?.getParcelable<User>(ARG_USER)
        if (user != null) {

            initAdapter(user, currentUser)

            textWatcher = object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {

                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                }

                override fun afterTextChanged(query: Editable?) {
                    if (!query.isNullOrBlank()) {
                        setAdapter(user, currentUser, query.toString())
                    } else {
                        initAdapter(user, currentUser)
                    }
                }

            }
            activity.mainBinding.primarySearchBar.addTextChangedListener(textWatcher)
        }
    }

    private fun initAdapter(user: User, currentUser: User?) {

        val userAdapter = UserAdapter(activity)

        binding.followingsRecycler.apply {
            adapter = userAdapter
            layoutManager = LinearLayoutManager(activity)
        }

        OverScrollDecoratorHelper.setUpOverScroll(binding.followingsRecycler, OverScrollDecoratorHelper.ORIENTATION_VERTICAL)

        if (user.id == currentUser?.id) {
            viewModel.userFollowings(user.id).observe(viewLifecycleOwner) {
                if (it.isNotEmpty()) {
                    userAdapter.submitList(it)
                }
            }
        } else {
            viewModel.userFollowings(user).observe(viewLifecycleOwner) {
                if (it.isNotEmpty()) {
                    userAdapter.submitList(it)
                }
            }
        }
    }

    private fun setAdapter(user: User, currentUser: User?, query: String) {
        val userAdapter = UserAdapter(activity)

        binding.followingsRecycler.apply {
            adapter = userAdapter
            layoutManager = LinearLayoutManager(activity)
        }

        OverScrollDecoratorHelper.setUpOverScroll(binding.followingsRecycler, OverScrollDecoratorHelper.ORIENTATION_VERTICAL)

        if (user.id == currentUser?.id) {
            viewModel.userFollowings(user.id, query).observe(viewLifecycleOwner) {
                if (it.isNotEmpty()) {
                    userAdapter.submitList(it)
                }
            }
        } else {
            viewModel.userFollowings(user, query).observe(viewLifecycleOwner) {
                if (it.isNotEmpty()) {
                    userAdapter.submitList(it)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity.mainBinding.primarySearchBar.removeTextChangedListener(textWatcher)
        activity.mainBinding.primarySearchBar.text.clear()
    }

    companion object {

        const val TAG = "FollowingsFragment"
        const val TITLE = "Followings"
        private const val ARG_USER = "ARG_USER"

        @JvmStatic
        fun newInstance(user: User) = FollowingsFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_USER, user)
            }
        }
    }
}