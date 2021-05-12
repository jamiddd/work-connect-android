package com.jamid.workconnect.search

import android.os.Bundle
import android.view.View
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.jamid.workconnect.*
import com.jamid.workconnect.databinding.FragmentSearchBinding
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class SearchFragment : SupportFragment(R.layout.fragment_search, TAG, false) {

    private lateinit var binding: FragmentSearchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = null
        exitTransition = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSearchBinding.bind(view)

        binding.searchViewPager.adapter = SearchPagesAdapter(activity)
        binding.searchViewPager.offscreenPageLimit = 1

        OverScrollDecoratorHelper.setUpOverScroll((binding.searchViewPager[0] as RecyclerView), OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL)

        viewModel.currentQuery.observe(viewLifecycleOwner) {
            if (it == null) {
                binding.emptySearchImg.visibility = View.VISIBLE
                binding.emptySearchText.visibility = View.VISIBLE
            } else {
                binding.emptySearchImg.visibility = View.GONE
                binding.emptySearchText.visibility = View.GONE
            }
        }

        TabLayoutMediator(activity.mainBinding.primaryTabs, binding.searchViewPager) { tabX, pos ->
            when (pos) {
                0 -> tabX.text = PROJECTS
                1 -> tabX.text = BLOGS
                2 -> tabX.text = PEOPLE
            }
        }.attach()

    }

    inner class SearchPagesAdapter(fa: FragmentActivity): FragmentStateAdapter(fa) {
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