package com.jamid.workconnect.search

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.jamid.workconnect.*
import com.jamid.workconnect.databinding.FragmentSearchBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchFragment : Fragment(R.layout.fragment_search) {

    private lateinit var binding: FragmentSearchBinding
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var activity: MainActivity

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity = requireActivity() as MainActivity
        binding = FragmentSearchBinding.bind(view)

        binding.searchViewPager.adapter = SearchPagesAdapter(requireActivity())
        binding.searchViewPager.offscreenPageLimit = 1

        activity.mainBinding.cancelSearchBtn.setOnClickListener {
            hideKeyboard()
        }

        lifecycleScope.launch {
            delay(150)
            activity.mainBinding.primaryTabs.visibility = View.VISIBLE
            activity.mainBinding.primarySearchLayout.transitionToEnd()
        }

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

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            if (bottom < 200) {
                findNavController().navigateUp()
                activity.mainBinding.primarySearchLayout.setTransitionDuration(150)
                activity.mainBinding.bottomNav.show(activity.mainBinding.bottomBlur)
                activity.mainBinding.primarySearchBar.text.clear()
                activity.mainBinding.primarySearchBar.clearFocus()
            }
        }

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

        @JvmStatic
        fun newInstance() = SearchFragment()
    }
}