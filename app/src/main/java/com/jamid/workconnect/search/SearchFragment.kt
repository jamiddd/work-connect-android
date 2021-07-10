package com.jamid.workconnect.search

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.jamid.workconnect.*
import com.jamid.workconnect.databinding.FragmentSearchBinding

class SearchFragment : SupportFragment(R.layout.fragment_search) {

    private lateinit var binding: FragmentSearchBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSearchBinding.bind(view)

        binding.searchViewPager.adapter = SearchPagesAdapter(activity)
        binding.searchViewPager.offscreenPageLimit = 1

        binding.searchMotionLayout.transitionToEnd()

        binding.searchBarText.requestFocus()
        showKeyboard()

        binding.cancelSearchBtn.setOnClickListener {
            hideKeyboard()
            findNavController().navigateUp()
        }

        TabLayoutMediator(binding.searchTabLayout, binding.searchViewPager) { tabX, pos ->
            when (pos) {
                0 -> tabX.text = PROJECTS
                1 -> tabX.text = BLOGS
                2 -> tabX.text = PEOPLE
            }
        }.attach()

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, _) ->
            binding.searchAppBar.setPadding(0, top, 0, 0)
        }

        binding.searchBarText.doAfterTextChanged {
            if (it.isNullOrBlank()) {
                viewModel.setCurrentQuery("")
            } else {
                viewModel.setCurrentQuery(it.toString())
            }
        }

    }

    inner class SearchPagesAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

        override fun getItemCount() = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> SearchProjectsFragment.newInstance()
                1 -> SearchBlogsFragment.newInstance()
                2 -> SearchPeopleFragment.newInstance()
                else -> SearchProjectsFragment.newInstance()
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.clearSearch()
    }

    companion object {

        const val TAG = "SearchFragment"

        @JvmStatic
        fun newInstance() = SearchFragment()
    }
}