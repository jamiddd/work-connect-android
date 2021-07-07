package com.jamid.workconnect.profile

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.workconnect.R
import com.jamid.workconnect.SupportFragment
import com.jamid.workconnect.adapter.GenericAdapter
import com.jamid.workconnect.convertDpToPx
import com.jamid.workconnect.databinding.FragmentSavedPostsBinding
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.Result
import com.jamid.workconnect.updateLayout
import kotlinx.coroutines.launch

class SavedPostsFragment : SupportFragment(R.layout.fragment_saved_posts, TAG, false) {

    private lateinit var binding: FragmentSavedPostsBinding
    private lateinit var savedPostsAdapter: GenericAdapter<Post>
    private var initialPosition = 0
    private val posts = mutableListOf<Post>()
    private var hasReachedEnd = false


    private fun getPosts(initial: Int, _final: Int) = viewLifecycleOwner.lifecycleScope.launch {
        if (!hasReachedEnd) {
            when (val savedPostsResult = viewModel.fetchSavedPosts(initial, _final)) {
                is Result.Error -> {
                    binding.loadMoreSavedPostsBtn.visibility = View.VISIBLE
                    binding.savedPostsProgress.visibility = View.GONE

                    Toast.makeText(activity, "Problem => " + savedPostsResult.exception.localizedMessage, Toast.LENGTH_SHORT).show()
                }
                is Result.Success -> {
                    binding.loadMoreSavedPostsBtn.visibility = View.VISIBLE
                    binding.savedPostsProgress.visibility = View.GONE
                    val savedPostsSnapshot = savedPostsResult.data
                    val savedPosts = viewModel.filterPosts(savedPostsSnapshot.toObjects(Post::class.java))
                    if (savedPosts.size < 10) {
                        hasReachedEnd = true
                        binding.loadMoreSavedPostsBtn.visibility = View.GONE
                    }
                    posts.addAll(savedPosts)
                    savedPostsAdapter.submitList(posts)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentSavedPostsBinding.bind(view)

        binding.savedPostsToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        initAdapter()

        setListeners()

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            if (activity.mainBinding.bottomCard.translationY != 0f) {
                binding.savedPostsNestedScroll.setPadding(0, convertDpToPx(8), 0, bottom)
            } else {
                binding.savedPostsNestedScroll.setPadding(0, convertDpToPx(8), 0, bottom + convertDpToPx(56))
            }
            binding.savedPostsToolbar.updateLayout(marginTop = top)
        }


        getPosts(initialPosition, initialPosition + 10)

        /*binding.saveProjectsRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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
        })*/
        /*viewModel.savedPosts().observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                job?.cancel()
                Log.d(BUG_TAG, "Got saved posts ... ${it.size}")
//                activity.mainBinding.primaryProgressBar.visibility = View.GONE
                binding.noSavedPostsLayoutScroll.visibility = View.GONE
                binding.saveProjectsRecycler.visibility = View.VISIBLE
                savedPostsAdapter.submitList(it)
            } else {
                Log.d(BUG_TAG, "Getting saved posts ... ")
                viewModel.getSavedPosts(initialPosition, finalPosition)
//                activity.mainBinding.primaryProgressBar.visibility = View.VISIBLE
                job = lifecycleScope.launch {
                    delay(2000)
//                    activity.mainBinding.primaryProgressBar.visibility = View.GONE
                    binding.noSavedPostsLayoutScroll.visibility = View.VISIBLE
                    binding.saveProjectsRecycler.visibility = View.GONE
                }
            }
        }*/

    }

    private fun setListeners() {
        binding.loadMoreSavedPostsBtn.setOnClickListener {
            binding.loadMoreSavedPostsBtn.visibility = View.GONE
            binding.savedPostsProgress.visibility = View.VISIBLE
            initialPosition += 10
            getPosts(initialPosition, initialPosition + 10)
        }
    }

    private fun initAdapter() {
        savedPostsAdapter = GenericAdapter(Post::class.java)

        binding.saveProjectsRecycler.apply {
            itemAnimator = null
            layoutManager = LinearLayoutManager(activity)
            adapter = savedPostsAdapter
        }
    }

    companion object {
        const val TITLE = "Saved Posts"
        const val TAG = "SavedPostsFrag"

        @JvmStatic
        fun newInstance() = SavedPostsFragment()
    }

}