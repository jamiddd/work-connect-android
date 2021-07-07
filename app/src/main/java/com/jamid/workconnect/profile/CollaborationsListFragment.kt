package com.jamid.workconnect.profile

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.workconnect.*
import com.jamid.workconnect.adapter.paging3.PostAdapter
import com.jamid.workconnect.databinding.FragmentCollaborationsListBinding
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(androidx.paging.ExperimentalPagingApi::class)
class CollaborationsListFragment : PagingListFragment(R.layout.fragment_collaborations_list) {

    private lateinit var postAdapter: PostAdapter
    private lateinit var binding: FragmentCollaborationsListBinding
    private var errorView: View? = null
    private lateinit var user: User

    private fun getUserCollaborations(user: User) {
        job?.cancel()
        job = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.otherUserCollaborationsFlow(user).collectLatest {
                postAdapter.submitData(it)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCollaborationsListBinding.bind(view)
        user = arguments?.getParcelable(ARG_USER) ?: return

        binding.collaborationsRefresher.isRefreshing = true

        postAdapter = PostAdapter()
        binding.collaborationsRecycler.setListAdapter(pagingAdapter = postAdapter, clazz = Post::class.java,
        onComplete = {
            getUserCollaborations(user)
        },
        onEmptySet = {
            binding.collaborationsRefresher.isRefreshing = false
            showEmptyNotificationsUI()
        }, onNonEmptySet = {
            binding.collaborationsRefresher.isRefreshing = false
            hideEmptyNotificationsUI()
        }, onNewDataArrivedOnTop = {
            binding.collaborationsRefresher.isRefreshing = false
        })

        binding.collaborationsRefresher.setSwipeRefresher {
            getUserCollaborations(user)
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (_, bottom) ->
            if (activity.mainBinding.bottomCard.translationY != 0f) {
                binding.collaborationsRecycler.setPadding(0, convertDpToPx(8), 0, bottom)
            } else {
                binding.collaborationsRecycler.setPadding(0, convertDpToPx(8), 0, bottom + convertDpToPx(56))
            }
        }

    }

    private fun hideEmptyNotificationsUI() {
        binding.collaborationsRefresher.visibility = View.VISIBLE
        errorView?.let {
            binding.collaborationsRoot.removeView(it)
        }
    }

    private fun showEmptyNotificationsUI() {
        binding.collaborationsRefresher.visibility = View.GONE
        if (errorView != null) {
            binding.collaborationsRoot.removeView(errorView)
            errorView = null
        }

        val errorViewBinding = if (viewModel.user.value?.id == user.id) {
            setErrorLayout(binding.collaborationsRoot, "No collaborations yet. Explore projects and start collaborating. Your collaborations will show up here.", errorActionEnabled = false)
        } else {
            setErrorLayout(binding.collaborationsRoot, "No collaborations yet.", errorActionEnabled = false)
        }

        val errorViewParams = errorViewBinding.errorItemsContainer.layoutParams as FrameLayout.LayoutParams
        errorViewParams.gravity = Gravity.TOP
        errorViewBinding.errorItemsContainer.layoutParams = errorViewParams

        errorView = errorViewBinding.root

    }

    companion object {

        private const val ARG_USER = "ARG_USER"

        @JvmStatic
        fun newInstance(user: User?) = CollaborationsListFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_USER, user)
            }
        }
    }

}