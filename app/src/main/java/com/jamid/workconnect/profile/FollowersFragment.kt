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
import com.jamid.workconnect.databinding.FragmentFollowersBinding
import com.jamid.workconnect.model.User
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class FollowersFragment : SupportFragment(R.layout.fragment_followers, TAG, false) {

    private lateinit var binding: FragmentFollowersBinding
    private var textWatcher: TextWatcher? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentFollowersBinding.bind(view)
        val currentUser = viewModel.user.value

        val insets = viewModel.windowInsets.value
        if (insets != null) {
            binding.followersRecycler.setPadding(0, convertDpToPx(120) + insets.first, 0, insets.second)
        }

//        setInsetView(binding.followersRecycler, mapOf(INSET_TOP to 120))

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

        binding.followersRecycler.apply {
            adapter = userAdapter
            layoutManager = LinearLayoutManager(activity)
        }

        OverScrollDecoratorHelper.setUpOverScroll(binding.followersRecycler, OverScrollDecoratorHelper.ORIENTATION_VERTICAL)


        if (user.id == currentUser?.id) {
            viewModel.userFollowers(user.id).observe(viewLifecycleOwner) {
                if (it.isNotEmpty()) {
                    userAdapter.submitList(it)
                }
            }
        } else {
            viewModel.userFollowers(user).observe(viewLifecycleOwner) {
                if (it.isNotEmpty()) {
                    userAdapter.submitList(it)
                }
            }
        }
    }

    private fun setAdapter(user: User, currentUser: User?, query: String) {
        val userAdapter = UserAdapter(activity)

        binding.followersRecycler.apply {
            adapter = userAdapter
            layoutManager = LinearLayoutManager(activity)
        }

        OverScrollDecoratorHelper.setUpOverScroll(binding.followersRecycler, OverScrollDecoratorHelper.ORIENTATION_VERTICAL)

        if (user.id == currentUser?.id) {
            viewModel.userFollowers(user.id, query).observe(viewLifecycleOwner) {
                if (it.isNotEmpty()) {
                    userAdapter.submitList(it)
                }
            }
        } else {
            viewModel.userFollowers(user, query).observe(viewLifecycleOwner) {
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

        const val TAG = "FollowersFragment"
        private const val ARG_USER = "ARG_USER"
        const val TITLE = "Followers"

        @JvmStatic
        fun newInstance(user: User) = FollowersFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_USER, user)
            }
        }
    }
}