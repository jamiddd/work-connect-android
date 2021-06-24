package com.jamid.workconnect.search

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jamid.workconnect.*
import com.jamid.workconnect.adapter.GenericAdapter
import com.jamid.workconnect.adapter.paging3.SearchAdapter
import com.jamid.workconnect.databinding.FragmentSearchPeopleBinding
import com.jamid.workconnect.model.RecentSearch
import com.jamid.workconnect.model.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SearchPeopleFragment : Fragment(R.layout.fragment_search_people) {

    private lateinit var binding: FragmentSearchPeopleBinding
    private lateinit var searchAdapter: SearchAdapter<User>
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var recentSearchAdapter: GenericAdapter<RecentSearch>
    private lateinit var activity: MainActivity
    private var job: Job? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as MainActivity
    }

    private fun search(query: String) {
        job?.cancel()
        job = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.searchUsers(query).collectLatest {
                searchAdapter.submitData(it)
            }
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentSearchPeopleBinding.bind(view)

//        initSearchAdapter()
        initRecentSearch()

        viewModel.currentQuery.observe(viewLifecycleOwner) {
            if (!it.isNullOrBlank()) {
                initSearchAdapter()
                binding.searchPeopleRoot.visibility = View.VISIBLE
                binding.noUsersFound.visibility = View.GONE

                binding.searchResultsPeople.text = "Search Result"

                search(it)
            } else {
                if (recentSearchAdapter.itemCount > 0) {
                    binding.noUsersFound.visibility = View.GONE
                } else {
                    binding.noUsersFound.visibility = View.VISIBLE
                    binding.searchPeopleRoot.visibility = View.GONE
                }
            }
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            binding.userHorizRecycler.setPadding(0, 0, 0, bottom + convertDpToPx(8))
        }
    }

    private fun initSearchAdapter() {
        if (!::searchAdapter.isInitialized) {
            searchAdapter = SearchAdapter(User::class.java, activity)

            binding.userHorizRecycler.apply {
                adapter = searchAdapter
                addItemDecoration(DividerItemDecoration(activity, RecyclerView.VERTICAL))
                layoutManager = LinearLayoutManager(activity)
            }
        }
    }

    private fun initRecentSearch() {
        recentSearchAdapter = GenericAdapter(RecentSearch::class.java)

        binding.userHorizRecycler.apply {
            adapter = recentSearchAdapter
            layoutManager = LinearLayoutManager(activity)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getRecentSearchesByType(USER).collectLatest {
                binding.noUsersFound.visibility = View.GONE
                if (it.isNotEmpty()) {
                    recentSearchAdapter.submitList(it)
                    binding.searchPeopleRoot.visibility = View.VISIBLE
                    binding.searchResultsPeople.text = "Recent Search"
                } else {
                    binding.searchPeopleRoot.visibility = View.GONE
                }
            }
        }
    }

    companion object {

        @JvmStatic
        fun newInstance() = SearchPeopleFragment()
    }

}