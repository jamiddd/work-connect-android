package com.jamid.workconnect.search

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jamid.workconnect.MainActivity
import com.jamid.workconnect.MainViewModel
import com.jamid.workconnect.R
import com.jamid.workconnect.adapter.paging2.SearchAdapter
import com.jamid.workconnect.convertDpToPx
import com.jamid.workconnect.databinding.FragmentSearchPeopleBinding
import com.jamid.workconnect.model.User
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class SearchPeopleFragment : Fragment(R.layout.fragment_search_people) {

    private lateinit var binding: FragmentSearchPeopleBinding
    private lateinit var searchAdapter: SearchAdapter<User>
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var activity: MainActivity

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as MainActivity
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentSearchPeopleBinding.bind(view)

        searchAdapter = SearchAdapter(User::class.java, activity)

        binding.userHorizRecycler.apply {
            adapter = searchAdapter
            addItemDecoration(DividerItemDecoration(activity, RecyclerView.VERTICAL))
            layoutManager = LinearLayoutManager(activity)
        }

        OverScrollDecoratorHelper.setUpOverScroll(binding.userHorizRecycler, OverScrollDecoratorHelper.ORIENTATION_VERTICAL)

        viewModel.currentQuery.observe(viewLifecycleOwner) {
            if (it != null) {
                viewModel.searchUsers(it).observe(viewLifecycleOwner) { users ->
                    if (users.isNotEmpty()) {
                        searchAdapter.submitList(users)
                    }
                }
            }
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            binding.userHorizRecycler.setPadding(0, top + convertDpToPx(104), 0, bottom + convertDpToPx(8))
        }
    }

    companion object {

        @JvmStatic
        fun newInstance() = SearchPeopleFragment()
    }

}