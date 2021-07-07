package com.jamid.workconnect.explore

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.jamid.workconnect.PagingListFragment
import com.jamid.workconnect.R
import com.jamid.workconnect.adapter.paging3.PostAdapter
import com.jamid.workconnect.convertDpToPx
import com.jamid.workconnect.databinding.FragmentTopBlogsBinding
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.updateLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TopBlogsFragment : PagingListFragment(R.layout.fragment_top_blogs) {

    private lateinit var binding: FragmentTopBlogsBinding
    private lateinit var topPostsAdapter: PostAdapter

    private fun getTopBlogs() {
        job?.cancel()
        job = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.topBlogsFlow().collectLatest {
                topPostsAdapter.submitData(it)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentTopBlogsBinding.bind(view)

        binding.topBlogsToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        topPostsAdapter = PostAdapter()

        binding.allTopBlogsRecycler.setListAdapter(
            pagingAdapter = topPostsAdapter,
            clazz = Post::class.java,
            onComplete = {
                getTopBlogs()
            })


        binding.allTopBlogsRefresher.setSwipeRefresher {
            getTopBlogs()
            stopRefreshProgress(it)
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            binding.topBlogsToolbar.updateLayout(marginTop = top)
            binding.allTopBlogsRecycler.setPadding(
                0,
                convertDpToPx(8),
                0,
                bottom + convertDpToPx(56)
            )
        }

    }

    companion object {

        const val TAG = "TopBlogsFragment"
        const val TITLE = "Top Blogs"

        @JvmStatic
        fun newInstance() =
            TopBlogsFragment()
    }
}