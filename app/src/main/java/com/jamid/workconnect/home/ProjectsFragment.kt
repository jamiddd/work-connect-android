package com.jamid.workconnect.home

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jamid.workconnect.*
import com.jamid.workconnect.adapter.PostAdapter
import com.jamid.workconnect.databinding.FragmentProjectsBinding
import com.jamid.workconnect.model.Post
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class ProjectsFragment : BaseFeedFragment(R.layout.fragment_projects) {

    private lateinit var binding: FragmentProjectsBinding
    var postAdapter: PostAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentProjectsBinding.bind(view)

        setRecyclerView(binding.projectsFragmentList)
        setDeleteListeners()

        val db = FirebaseFirestore.getInstance()

        val query = db.collection(POSTS)
            .orderBy(CREATED_AT, Query.Direction.DESCENDING)

        val config = PagedList.Config.Builder()
            .setPageSize(PAGE_SIZE)
            .setEnablePlaceholders(false)
            .setPrefetchDistance(PREFETCH_DIST)
            .build()

        val options = FirestorePagingOptions.Builder<Post>()
            .setLifecycleOwner(viewLifecycleOwner)
            .setQuery(query, config, Post::class.java)
            .build()

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            binding.projectsFragmentList
                .setPadding(0, top + convertDpToPx(112), 0, bottom + convertDpToPx(64))
        }

        postAdapter = PostAdapter(viewModel, options, viewLifecycleOwner, activity, activity)

        binding.projectsFragmentList.apply {
            adapter = postAdapter
            layoutManager = LinearLayoutManager(activity)
        }

        OverScrollDecoratorHelper.setUpOverScroll(binding.projectsFragmentList, OverScrollDecoratorHelper.ORIENTATION_VERTICAL)

        var scrollPosition = 0

        binding.projectsFragmentList.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                scrollPosition += dy
                if (dy < 0) {
                    if (activity.mainBinding.primaryAppBar.translationY != 0f) {
                        activity.mainBinding.primaryAppBar.translateDown()
                    }
                } else {
                    if (scrollPosition > convertDpToPx(80)) {
                        if (activity.mainBinding.primaryAppBar.translationY == 0f) {
                            activity.mainBinding.primaryAppBar.translateUp()
                        }
                    }
                }
            }
        })

        viewModel.currentHomeTag.observe(viewLifecycleOwner) {
            if (it != null) {
                val newQuery = db.collection(POSTS)
                    .whereArrayContains(TAGS, it)
                    .orderBy(CREATED_AT, Query.Direction.DESCENDING)

                val newOptions = FirestorePagingOptions.Builder<Post>()
                    .setQuery(newQuery, config, Post::class.java)
                    .setLifecycleOwner(viewLifecycleOwner)
                    .build()

                postAdapter?.updateOptions(newOptions)
            } else {
                val query1 = db.collection(POSTS)
                    .orderBy(CREATED_AT, Query.Direction.DESCENDING)

                val options1 = FirestorePagingOptions.Builder<Post>()
                    .setLifecycleOwner(viewLifecycleOwner)
                    .setQuery(query1, config, Post::class.java)
                    .build()

                postAdapter?.updateOptions(options1)
            }
        }
    }

    private fun AppBarLayout.translateUp() {
        val animator = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, -convertDpToPx(56f))
        val toolbar = this.findViewById<MaterialToolbar>(R.id.primaryToolbar)
        val animator1 = ObjectAnimator.ofFloat(toolbar, View.ALPHA, 0f)
        val animatorSet = AnimatorSet()
        animatorSet.apply {
            duration = 150
            interpolator = LinearInterpolator()
            playTogether(animator, animator1)
            start()
        }
    }

    private fun AppBarLayout.translateDown() {
        val animator = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, 0f)
        val toolbar = this.findViewById<MaterialToolbar>(R.id.primaryToolbar)
        val animator1 = ObjectAnimator.ofFloat(toolbar, View.ALPHA, 1f)
        val animatorSet = AnimatorSet()
        animatorSet.apply {
            duration = 150
            interpolator = LinearInterpolator()
            playTogether(animator, animator1)
            start()
        }
    }

    companion object {

        private const val PAGE_SIZE = 10
        private const val PREFETCH_DIST = 5

        @JvmStatic
        fun newInstance() = ProjectsFragment()
    }

}