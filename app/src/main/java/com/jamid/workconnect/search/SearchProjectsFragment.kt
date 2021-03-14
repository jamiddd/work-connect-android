package com.jamid.workconnect.search

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.paging.PagedList
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.*
import com.jamid.workconnect.adapter.SearchAdapter
import com.jamid.workconnect.databinding.FragmentSearchProjectsBinding
import com.jamid.workconnect.model.SearchResult
import java.util.*

class SearchProjectsFragment : Fragment(R.layout.fragment_search_projects) {

    private lateinit var binding: FragmentSearchProjectsBinding
    private lateinit var searchAdapter: SearchAdapter
    private val viewModel: MainViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentSearchProjectsBinding.bind(view)
        val db = Firebase.firestore
        val activity = requireActivity() as MainActivity

        val config = PagedList.Config.Builder().setPageSize(10).setEnablePlaceholders(false).setPrefetchDistance(5).build()

        val initialQuery = db.collection(POSTS_SEARCH)
            .whereEqualTo(TYPE, PROJECT)
            .whereArrayContains(SUBSTRINGS, "Tag")
            .orderBy(RANK, Query.Direction.DESCENDING)

        val options = FirestorePagingOptions.Builder<SearchResult>()
            .setQuery(initialQuery, config, SearchResult::class.java)
            .setLifecycleOwner(viewLifecycleOwner)
            .build()

        searchAdapter = SearchAdapter(options, activity)

        binding.searchProjectsRecycler.apply {
            adapter = searchAdapter
            addItemDecoration(DividerItemDecoration(activity, RecyclerView.VERTICAL))
            layoutManager = LinearLayoutManager(requireContext())
        }

        viewModel.currentQuery.observe(viewLifecycleOwner) {
            if (it != null) {
                val query = db.collection(POSTS_SEARCH)
                    .whereEqualTo(TYPE, PROJECT)
                    .whereArrayContainsAny(SUBSTRINGS,
                        listOf(
                            it,
                            it.capitalize(Locale.ROOT), it.decapitalize(Locale.ROOT),
                            it.toUpperCase(Locale.ROOT), it.toLowerCase(Locale.ROOT)
                        )
                    )
                    .orderBy(RANK, Query.Direction.DESCENDING)

                val newOptions = FirestorePagingOptions.Builder<SearchResult>()
                    .setQuery(query, config, SearchResult::class.java)
                    .setLifecycleOwner(viewLifecycleOwner)
                    .build()

                searchAdapter.updateOptions(newOptions)
            }
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            binding.searchProjectsRecycler.setPadding(0, top + convertDpToPx(104), 0, bottom + convertDpToPx(8))
        }

    }

    companion object {

        @JvmStatic
        fun newInstance() = SearchProjectsFragment()
    }

}