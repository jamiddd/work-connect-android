package com.jamid.workconnect.profile

import android.os.Bundle
import android.view.View
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jamid.workconnect.*
import com.jamid.workconnect.adapter.PostAdapter
import com.jamid.workconnect.databinding.FragmentCollaborationsListBinding
import com.jamid.workconnect.interfaces.PostItemClickListener
import com.jamid.workconnect.interfaces.PostsLoadStateListener
import com.jamid.workconnect.message.ProjectDetailContainer
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.User

class CollaborationsListFragment : BaseFeedFragment(R.layout.fragment_collaborations_list) {

    private lateinit var postAdapter: PostAdapter
    private lateinit var binding: FragmentCollaborationsListBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCollaborationsListBinding.bind(view)
        val user = arguments?.getParcelable<User>(ARG_USER)
        setRecyclerView(binding.collaborationsRecycler)
        setDeleteListeners()

        if (user != null) {
            val query = FirebaseFirestore.getInstance()
                .collection(POSTS)
                .orderBy(CREATED_AT, Query.Direction.DESCENDING)
                .whereArrayContains(CONTRIBUTORS, user.id)
                .whereEqualTo(TYPE, PROJECT)

            val config = PagedList.Config.Builder().setPageSize(10).setEnablePlaceholders(false).setPrefetchDistance(5).build()

            val options = FirestorePagingOptions.Builder<Post>()
                .setLifecycleOwner(viewLifecycleOwner)
                .setQuery(query, config, Post::class.java)
                .build()

            val parent = activity.currentBottomFragment
            postAdapter = if (parent is ProjectDetailContainer) {
                PostAdapter(viewModel, options, viewLifecycleOwner, parent as PostItemClickListener, parent as PostsLoadStateListener)
            } else {
                PostAdapter(viewModel, options, viewLifecycleOwner, activity, activity)
            }

            binding.collaborationsRecycler.apply {
                adapter = postAdapter
                layoutManager = LinearLayoutManager(activity)
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