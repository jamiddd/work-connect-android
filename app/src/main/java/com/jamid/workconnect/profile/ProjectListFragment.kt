package com.jamid.workconnect.profile

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jamid.workconnect.*
import com.jamid.workconnect.adapter.PostAdapter
import com.jamid.workconnect.databinding.FragmentProjectListBinding
import com.jamid.workconnect.interfaces.PostItemClickListener
import com.jamid.workconnect.interfaces.PostsLoadStateListener
import com.jamid.workconnect.message.ProjectDetailContainer
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.User

class ProjectListFragment : BaseFeedFragment(R.layout.fragment_project_list) {

    private lateinit var postAdapter: PostAdapter
    private lateinit var binding: FragmentProjectListBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentProjectListBinding.bind(view)
        val user = arguments?.getParcelable<User>(ARG_USER)

        setDeleteListeners()
        setRecyclerView(binding.projectsRecycler)

        if (user != null) {

            val query = FirebaseFirestore.getInstance()
                .collection(POSTS)
                .orderBy(CREATED_AT, Query.Direction.DESCENDING)
                .whereEqualTo(UID, user.id)
                .whereEqualTo(TYPE, PROJECT)

            val config = PagedList.Config.Builder().setPageSize(10).setEnablePlaceholders(false).setPrefetchDistance(5).build()

            val options = FirestorePagingOptions.Builder<Post>()
                .setLifecycleOwner(viewLifecycleOwner)
                .setQuery(query, config, Post::class.java)
                .build()

            val parent = activity.currentBottomFragment
            postAdapter = if (parent is ProjectDetailContainer) {
                Log.d("ProjectList", "yes")
                PostAdapter(viewModel, options, viewLifecycleOwner, parent as PostItemClickListener, parent as PostsLoadStateListener)
            } else {
                Log.d("ProjectList", "No")
                PostAdapter(viewModel, options, viewLifecycleOwner, activity, activity)
            }

            binding.projectsRecycler.apply {
                adapter = postAdapter
                layoutManager = LinearLayoutManager(requireContext())
            }
        }
    }

    companion object {

        private const val ARG_USER = "ARG_USER"

        @JvmStatic
        fun newInstance(user: User?) = ProjectListFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_USER, user)
            }
        }
    }

}