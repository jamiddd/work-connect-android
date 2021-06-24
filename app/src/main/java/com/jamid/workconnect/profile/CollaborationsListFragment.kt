package com.jamid.workconnect.profile

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.workconnect.InsetControlFragment
import com.jamid.workconnect.R
import com.jamid.workconnect.adapter.paging3.PostAdapter
import com.jamid.workconnect.databinding.FragmentCollaborationsListBinding
import com.jamid.workconnect.model.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(androidx.paging.ExperimentalPagingApi::class)
class CollaborationsListFragment : InsetControlFragment(R.layout.fragment_collaborations_list) {

    private lateinit var postAdapter: PostAdapter
    private lateinit var binding: FragmentCollaborationsListBinding
    private var job: Job? = null

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
        /*setInsetView(binding.collaborationsRecycler, mapOf(INSET_TOP to 8, INSET_BOTTOM to 48))*/

        val user = arguments?.getParcelable<User>(ARG_USER)

        if (user != null) {

            initAdapter()

            if (user.id != viewModel.user.value?.id) {
                binding.noCollaborationsText.text = "No collaborations"
            }

            setCollaborationsRefresher(user)

            getUserCollaborations(user)

        }
    }

    private fun setCollaborationsRefresher(user: User) {
        viewLifecycleOwner.lifecycleScope.launch {
            postAdapter.loadStateFlow.collectLatest { loadStates ->
                binding.collaborationsRefresher.isRefreshing = true
                if (loadStates.refresh is LoadState.NotLoading) {
                    delay(1000)
                    binding.collaborationsRefresher.isRefreshing = false
                    if (postAdapter.itemCount == 0) {
                        showEmptyNotificationsUI()
                    } else {
                        hideEmptyNotificationsUI()
                    }
                }
            }
        }

        binding.collaborationsRefresher.setOnRefreshListener {

            getUserCollaborations(user)

            viewLifecycleOwner.lifecycleScope.launch {
                delay(2000)
                binding.collaborationsRefresher.isRefreshing = false
            }
        }
    }

    private fun hideEmptyNotificationsUI() {
        binding.collaborationsRecycler.visibility = View.VISIBLE
        binding.noCollaborationsLayoutScroll.visibility = View.GONE
    }

    private fun showEmptyNotificationsUI() {
        binding.collaborationsRecycler.visibility = View.GONE
        binding.noCollaborationsLayoutScroll.visibility = View.VISIBLE
    }


    private fun initAdapter() {
        postAdapter = PostAdapter()

        binding.collaborationsRecycler.apply {
            adapter = postAdapter
            itemAnimator = null
            setRecycledViewPool(activity.recyclerViewPool)
            layoutManager = LinearLayoutManager(activity)
        }
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