package com.jamid.workconnect.explore

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.workconnect.*
import com.jamid.workconnect.adapter.ExploreAdapter
import com.jamid.workconnect.databinding.FragmentExploreHomeBinding
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.UserMinimal
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class ExploreHomeFragment : Fragment(R.layout.fragment_explore_home) {

    private lateinit var binding: FragmentExploreHomeBinding
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var activity: MainActivity

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = requireActivity() as MainActivity
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentExploreHomeBinding.bind(view)

        val post1 = Post()
        post1.type = PROJECT

        val post2 = Post()
        post2.type = BLOG

        val exploreAdapter = ExploreAdapter(listOf(post1, post2, UserMinimal()), viewLifecycleOwner, viewModel, activity, activity)

        binding.exploreRecycler.apply {
            adapter = exploreAdapter
            layoutManager = LinearLayoutManager(activity)
        }

        OverScrollDecoratorHelper.setUpOverScroll(binding.exploreRecycler, OverScrollDecoratorHelper.ORIENTATION_VERTICAL)

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            binding.root.setPadding(0, top + convertDpToPx(64), 0, bottom + convertDpToPx(64))
        }

    }

    companion object {

        @JvmStatic
        fun newInstance() =
            ExploreHomeFragment()
    }
}