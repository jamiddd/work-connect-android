package com.jamid.workconnect.search

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jamid.workconnect.*
import com.jamid.workconnect.adapter.paging2.SearchAdapter
import com.jamid.workconnect.databinding.FragmentSearchProjectsBinding
import com.jamid.workconnect.model.Post
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class SearchProjectsFragment : Fragment(R.layout.fragment_search_projects) {

    private lateinit var binding: FragmentSearchProjectsBinding
    private lateinit var searchAdapter: SearchAdapter<Post>
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var activity: MainActivity

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as MainActivity
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentSearchProjectsBinding.bind(view)

        searchAdapter = SearchAdapter(Post::class.java, activity)

        binding.searchProjectsRecycler.apply {
            adapter = searchAdapter
            addItemDecoration(DividerItemDecoration(activity, RecyclerView.VERTICAL))
            layoutManager = LinearLayoutManager(activity)
        }

        OverScrollDecoratorHelper.setUpOverScroll(binding.searchProjectsRecycler, OverScrollDecoratorHelper.ORIENTATION_VERTICAL)

        viewModel.currentQuery.observe(viewLifecycleOwner) {
            if (it != null) {
                viewModel.searchPosts(it, PROJECT).observe(viewLifecycleOwner) { posts ->
                    if (posts != null) {
                        searchAdapter.submitList(posts)
                    }
                }
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