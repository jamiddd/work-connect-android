package com.jamid.workconnect.profile

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jamid.workconnect.BUG_TAG
import com.jamid.workconnect.R
import com.jamid.workconnect.SupportFragment
import com.jamid.workconnect.adapter.paging2.PostAdapter
import com.jamid.workconnect.databinding.FragmentSavedPostsBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class SavedPostsFragment : SupportFragment(R.layout.fragment_saved_posts, TAG, false) {

    private lateinit var binding: FragmentSavedPostsBinding
    private lateinit var savedPostsAdapter: PostAdapter
    private var initialPosition = 0
    private var finalPosition = 20
    private var job: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentSavedPostsBinding.bind(view)

        setInsetView(binding.saveProjectsRecycler, mapOf(insetTop to 64))

        savedPostsAdapter = PostAdapter()

        binding.saveProjectsRecycler.apply {
            itemAnimator = null
            layoutManager = LinearLayoutManager(activity)
            adapter = savedPostsAdapter
        }

        OverScrollDecoratorHelper.setUpOverScroll(binding.noSavedPostsLayoutScroll)

        OverScrollDecoratorHelper.setUpOverScroll(binding.saveProjectsRecycler, OverScrollDecoratorHelper.ORIENTATION_VERTICAL)

        binding.saveProjectsRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val layoutManager = binding.saveProjectsRecycler.layoutManager as LinearLayoutManager
                val lastPosition = layoutManager.findFirstCompletelyVisibleItemPosition()
                if (lastPosition > finalPosition - 5) {
                    viewModel.getSavedPosts(initialPosition, finalPosition)
                    initialPosition += 20
                    finalPosition += 20
                }
            }
        })

        viewModel.savedPosts().observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                job?.cancel()
                Log.d(BUG_TAG, "Got saved posts ... ${it.size}")
                activity.mainBinding.primaryProgressBar.visibility = View.GONE
                binding.noSavedPostsLayoutScroll.visibility = View.GONE
                binding.saveProjectsRecycler.visibility = View.VISIBLE
                savedPostsAdapter.submitList(it)
            } else {
                Log.d(BUG_TAG, "Getting saved posts ... ")
                viewModel.getSavedPosts(initialPosition, finalPosition)
                activity.mainBinding.primaryProgressBar.visibility = View.VISIBLE
                job = lifecycleScope.launch {
                    delay(2000)
                    activity.mainBinding.primaryProgressBar.visibility = View.GONE
                    binding.noSavedPostsLayoutScroll.visibility = View.VISIBLE
                    binding.saveProjectsRecycler.visibility = View.GONE
                }
            }
        }

    }

    companion object {
        const val TITLE = "Saved Posts"
        const val TAG = "SavedPostsFrag"

        @JvmStatic
        fun newInstance() = SavedPostsFragment()
    }

}