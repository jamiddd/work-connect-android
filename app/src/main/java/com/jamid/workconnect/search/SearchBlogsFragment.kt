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
import com.jamid.workconnect.databinding.FragmentSearchBlogsBinding
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.RecentSearch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SearchBlogsFragment : Fragment(R.layout.fragment_search_blogs) {

    private lateinit var binding: FragmentSearchBlogsBinding
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
            viewModel.searchPosts(query, BLOG).collectLatest {
                searchAdapter.submitData(it)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentSearchBlogsBinding.bind(view)

//        initSearchAdapter()
        initRecentSearch()

        viewModel.currentQuery.observe(viewLifecycleOwner) {
            if (!it.isNullOrBlank()) {
                initSearchAdapter()
                binding.searchBlogsRoot.visibility = View.VISIBLE
                binding.noBlogsFound.visibility = View.GONE

                binding.searchResultsBlog.text = "Search Result"

                search(it)
            } else {
                if (recentSearchAdapter.itemCount > 0) {
                    binding.noBlogsFound.visibility = View.GONE
                } else {
                    binding.noBlogsFound.visibility = View.VISIBLE
                    binding.searchBlogsRoot.visibility = View.GONE
                }
            }
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            binding.searchBlogsRecycler.setPadding(0, 0, 0, bottom + convertDpToPx(8))
        }

    }

    private fun initSearchAdapter() {

        if (!::searchAdapter.isInitialized) {
            searchAdapter = SearchAdapter(Post::class.java, activity)

            binding.searchBlogsRecycler.apply {
                adapter = searchAdapter
                addItemDecoration(DividerItemDecoration(activity, RecyclerView.VERTICAL))
                layoutManager = LinearLayoutManager(activity)
            }
        }
    }

    private fun initRecentSearch() {
        recentSearchAdapter = GenericAdapter(RecentSearch::class.java)

        binding.searchBlogsRecycler.apply {
            adapter = recentSearchAdapter
            layoutManager = LinearLayoutManager(activity)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getRecentSearchesByType(BLOG).collectLatest {
                binding.noBlogsFound.visibility = View.GONE
                if (it.isNotEmpty()) {
                    recentSearchAdapter.submitList(it)
                    binding.searchBlogsRoot.visibility = View.VISIBLE
                    binding.searchResultsBlog.text = "Recent Search"
                } else {
                    binding.searchBlogsRoot.visibility = View.GONE
                }
            }
        }
    }

    companion object {

        @JvmStatic
        fun newInstance() = SearchBlogsFragment()
    }
}