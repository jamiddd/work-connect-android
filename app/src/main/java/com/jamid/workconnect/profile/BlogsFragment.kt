package com.jamid.workconnect.profile

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.workconnect.InsetControlFragment
import com.jamid.workconnect.PagingListFragment
import com.jamid.workconnect.R
import com.jamid.workconnect.adapter.paging3.PostAdapter
import com.jamid.workconnect.convertDpToPx
import com.jamid.workconnect.databinding.FragmentBlogsBinding
import com.jamid.workconnect.home.EditorFragment
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(androidx.paging.ExperimentalPagingApi::class)
class BlogsFragment : PagingListFragment(R.layout.fragment_blogs) {

    private lateinit var binding: FragmentBlogsBinding
    private lateinit var postAdapter: PostAdapter
    private lateinit var user: User
    private var errorView: View? = null

    private fun getUserBlogs(user: User) {
        job?.cancel()
        job = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.otherUserBlogsFlow(user).collectLatest {
                postAdapter.submitData(it)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentBlogsBinding.bind(view)
        user = arguments?.getParcelable(ARG_USER) ?: return
        binding.blogsRefresher.isRefreshing = true
        postAdapter = PostAdapter()

        binding.blogsRecycler.setListAdapter(pagingAdapter = postAdapter,
        clazz = Post::class.java,
        onComplete = {
            getUserBlogs(user)
        },
        onEmptySet = {
            binding.blogsRefresher.isRefreshing = false
            showEmptyNotificationsUI()
        }, onNonEmptySet = {
            binding.blogsRefresher.isRefreshing = false
            hideEmptyNotificationsUI()
        }, onNewDataArrivedOnTop = {
            binding.blogsRefresher.isRefreshing = false
        })

        binding.blogsRefresher.setSwipeRefresher {
            getUserBlogs(user)
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (_, bottom) ->
            if (activity.mainBinding.bottomCard.translationY != 0f) {
                binding.blogsRecycler.setPadding(0, convertDpToPx(8), 0, bottom)
            } else {
                binding.blogsRecycler.setPadding(0, convertDpToPx(8), 0, bottom + convertDpToPx(56))
            }
        }

    }

    private fun hideEmptyNotificationsUI() {
        binding.blogsRefresher.visibility = View.VISIBLE
        errorView?.let {
            binding.blogFragmentRoot.removeView(it)
        }
    }

    private fun showEmptyNotificationsUI() {
        binding.blogsRefresher.visibility = View.GONE
        if (errorView != null) {
            binding.blogFragmentRoot.removeView(errorView)
            errorView = null
        }
        val errorViewBinding = if (viewModel.user.value?.id == user.id) {
            setErrorLayout(binding.blogFragmentRoot, "No blogs yet.\nYour blogs will show up here.", errorActionLabel = "Create Blog") { b, p ->
                b.visibility = View.VISIBLE
                p.visibility = View.GONE
                findNavController().navigate(R.id.editorFragment, null, options)
            }
        } else {
            setErrorLayout(binding.blogFragmentRoot, "No blogs yet.", errorActionEnabled = false)
        }

        val errorViewParams = errorViewBinding.errorItemsContainer.layoutParams as FrameLayout.LayoutParams
        errorViewParams.gravity = Gravity.TOP
        errorViewBinding.errorItemsContainer.layoutParams = errorViewParams

        errorView = errorViewBinding.root

    }

    companion object {

        private const val ARG_USER = "ARG_USER"

        @JvmStatic
        fun newInstance(user: User?) = BlogsFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_USER, user)
            }
        }

    }


}