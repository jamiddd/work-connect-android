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
import com.jamid.workconnect.databinding.FragmentSearchProjectsBinding
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.RecentSearch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SearchProjectsFragment : Fragment(R.layout.fragment_search_projects) {

    private lateinit var binding: FragmentSearchProjectsBinding
    private lateinit var searchAdapter: SearchAdapter<Post>
    private lateinit var recentSearchAdapter: GenericAdapter<RecentSearch>
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var activity: MainActivity
    private var job: Job? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as MainActivity
    }

    private fun search(query: String) {
        job?.cancel()
        job = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.searchPosts(query, PROJECT).collectLatest {
                searchAdapter.submitData(it)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentSearchProjectsBinding.bind(view)

        initRecentSearch()

        viewModel.currentQuery.observe(viewLifecycleOwner) {
            if (!it.isNullOrBlank()) {
                initSearchAdapter()
                binding.searchProjectsRoot.visibility = View.VISIBLE
                binding.noProjectsFound.visibility = View.GONE

                binding.searchResultsProjects.text = "Search Result"

                search(it)
            } else {
                if (recentSearchAdapter.itemCount > 0) {
                    binding.noProjectsFound.visibility = View.GONE
                } else {
                    binding.noProjectsFound.visibility = View.VISIBLE
                    binding.searchProjectsRoot.visibility = View.GONE
                }
            }
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            binding.searchProjectsRecycler.setPadding(0, 0, 0, bottom + convertDpToPx(8))
        }

    }

    private fun initSearchAdapter() {

        if (!::searchAdapter.isInitialized) {
            searchAdapter = SearchAdapter(Post::class.java, activity)

            binding.searchProjectsRecycler.apply {
                adapter = searchAdapter
                addItemDecoration(DividerItemDecoration(activity, RecyclerView.VERTICAL))
                layoutManager = LinearLayoutManager(activity)
            }
        }
    }

    private fun initRecentSearch() {
        recentSearchAdapter = GenericAdapter(RecentSearch::class.java)

        binding.searchProjectsRecycler.apply {
            adapter = recentSearchAdapter
            layoutManager = LinearLayoutManager(activity)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getRecentSearchesByType(PROJECT).collectLatest {
                binding.noProjectsFound.visibility = View.GONE
                if (it.isNotEmpty()) {
                    recentSearchAdapter.submitList(it)
                    binding.searchProjectsRoot.visibility = View.VISIBLE
                    binding.searchResultsProjects.text = "Recent Search"
                } else {
                    binding.searchProjectsRoot.visibility = View.GONE
                }
            }
        }
    }

    companion object {

        @JvmStatic
        fun newInstance() = SearchProjectsFragment()
    }

}