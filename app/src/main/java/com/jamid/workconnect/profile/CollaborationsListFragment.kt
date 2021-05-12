package com.jamid.workconnect.profile

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.workconnect.InsetControlFragment
import com.jamid.workconnect.R
import com.jamid.workconnect.adapter.paging2.PostAdapter
import com.jamid.workconnect.databinding.FragmentCollaborationsListBinding
import com.jamid.workconnect.model.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class CollaborationsListFragment : InsetControlFragment(R.layout.fragment_collaborations_list) {

    private lateinit var postAdapter: PostAdapter
    private lateinit var binding: FragmentCollaborationsListBinding
    private var job: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCollaborationsListBinding.bind(view)
        /*setInsetView(binding.collaborationsRecycler, mapOf(INSET_TOP to 8, INSET_BOTTOM to 48))*/

        val user = arguments?.getParcelable<User>(ARG_USER)

        if (user != null) {

            postAdapter = PostAdapter()

            binding.collaborationsRecycler.apply {
                adapter = postAdapter
                itemAnimator = null
                layoutManager = LinearLayoutManager(activity)
            }

            OverScrollDecoratorHelper.setUpOverScroll(binding.collaborationsRecycler, OverScrollDecoratorHelper.ORIENTATION_VERTICAL)

            viewModel.userCollaborations(user.id).observe(viewLifecycleOwner) {
                if (it.isNotEmpty()) {
                    activity.mainBinding.primaryProgressBar.visibility = View.GONE
                    binding.collaborationsRecycler.visibility = View.VISIBLE
                    binding.noCollaborationsLayoutScroll.visibility = View.GONE
                    postAdapter.submitList(it)
                } else {
                    activity.mainBinding.primaryProgressBar.visibility = View.VISIBLE
                    job = lifecycleScope.launch {
                        delay(2000)
                        activity.mainBinding.primaryProgressBar.visibility = View.GONE
                        binding.collaborationsRecycler.visibility = View.GONE
                        OverScrollDecoratorHelper.setUpOverScroll(binding.noCollaborationsLayoutScroll)
                        binding.noCollaborationsLayoutScroll.visibility = View.VISIBLE
                    }
                }
            }

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