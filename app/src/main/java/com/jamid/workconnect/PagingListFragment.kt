package com.jamid.workconnect

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.navOptions
import androidx.paging.LoadState
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.jamid.workconnect.adapter.paging3.PostAdapter
import com.jamid.workconnect.adapter.paging3.PostsLoadStateAdapter
import com.jamid.workconnect.databinding.ErrorNoOutputLayoutBinding
import com.jamid.workconnect.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

const val LOADING_TIME_OUT: Long = 1000

abstract class PagingListFragment(@LayoutRes layout: Int) : Fragment(layout) {

    var job: Job? = null
    val viewModel: MainViewModel by activityViewModels()
    lateinit var activity: MainActivity

    val options = navOptions {
        anim {
            enter = R.anim.slide_in_right
            exit = R.anim.slide_out_left
            popEnter = R.anim.slide_in_left
            popExit = R.anim.slide_out_right
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as MainActivity
    }

    /*fun bind(binding: ViewDataBinding) {
        when (binding) {
            is FragmentProjectsBinding -> {
                binding.postsRefresher.bindSwipeRefresher()
            }
            is FragmentBlogsBinding -> {
                binding.blogsRefresher.bindSwipeRefresher()
            }
            is FragmentProjectListBinding -> {
                binding.projectsListRefresher.bindSwipeRefresher()
            }
            is FragmentCollaborationsListBinding -> {
                binding.collaborationsRefresher.bindSwipeRefresher()
            }
            is FragmentGeneralNotificationBinding -> {
                binding.notificationsRefresher.bindSwipeRefresher()
            }
            is FragmentRequestBinding -> {
                binding.requestsRefresher.bindSwipeRefresher()
            }
            is FragmentChatChannelBinding -> {
                binding.chatChannelRefresher.bindSwipeRefresher()
            }
        }
    }*/

    fun <T : Any, U : RecyclerView.ViewHolder> RecyclerView.setListAdapter(
        listAdapter: ListAdapter<T, U>? = null,
        pagingAdapter: PagingDataAdapter<T, U>? = null,
        clazz: Class<T>? = null,
        isHorizontal: Boolean = false,
        onComplete: () -> Unit,
        onEmptySet: (() -> Unit)? = null,
        onNonEmptySet: (() -> Unit)? = null,
        onNewDataArrivedOnTop: (() -> Unit)? = null
    ) {
        val context = this.context

        adapter = when (clazz) {
            Post::class.java -> {
                pagingAdapter?.withLoadStateFooter(footer = PostsLoadStateAdapter(pagingAdapter as PostAdapter))
            }
            SimpleNotification::class.java, SimpleRequest::class.java, User::class.java, SimpleComment::class.java -> {
                pagingAdapter
            }
            else -> listAdapter
        }

        layoutManager = if (!isHorizontal) {
            LinearLayoutManager(context)
        } else {
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }

        onComplete()

        pagingAdapter?.let {

            viewLifecycleOwner.lifecycleScope.launch {
                it.loadStateFlow.collectLatest { loadStates ->
                    if (loadStates.refresh is LoadState.NotLoading) {
                        delay(LOADING_TIME_OUT)
                        if (it.itemCount == 0) {
                            onEmptySet?.let { it() }
                        } else {
                            onNonEmptySet?.let { it() }
                        }
                    }
                }

                it.loadStateFlow.distinctUntilChangedBy { it.refresh }
                    .filter { it.refresh is LoadState.NotLoading }
                    .collect {
                        onNewDataArrivedOnTop?.let { it() }
                    }

            }
        }
    }

    fun stopRefreshProgress(refresher: SwipeRefreshLayout) = viewLifecycleOwner.lifecycleScope.launch {
        delay(LOADING_TIME_OUT)
        refresher.isRefreshing = false
    }

    fun SwipeRefreshLayout.setSwipeRefresher(onRefresh: ((v: SwipeRefreshLayout) -> Unit)? = null) {
        val context = this.context
        val purple = ContextCompat.getColor(context, R.color.purple_200)
        val white = ContextCompat.getColor(context, R.color.white)
        val blue = ContextCompat.getColor(context, R.color.blue_500)
        val darkerGrey = ContextCompat.getColor(context, R.color.darkestGrey)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (context.resources?.configuration?.isNightModeActive == true) {
                setProgressBackgroundColorSchemeColor(darkerGrey)
                setColorSchemeColors(purple)
            } else {
                setProgressBackgroundColorSchemeColor(white)
                setColorSchemeColors(blue)
            }
        } else {
            if (context.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
                setProgressBackgroundColorSchemeColor(darkerGrey)
                setColorSchemeColors(purple)
            } else {
                setColorSchemeColors(blue)
                setProgressBackgroundColorSchemeColor(white)
            }
        }

        onRefresh?.let {
            setOnRefreshListener {
                onRefresh(this)
            }
        }
    }

    fun setErrorLayout(
        parent: ViewGroup,
        errorMessage: String = "Something went wrong!",
        @DrawableRes errorImg: Int = R.drawable.ic_empty_blogs,
        dependantView: View? = null,
        errorActionEnabled: Boolean = true,
        errorActionLabel: String = "Retry",
        margin: Int = 0,
        onActionClick: ((v: View, p: ProgressBar) -> Unit)? = null
    ): ErrorNoOutputLayoutBinding {
        val errorViewBinding = DataBindingUtil.inflate<ErrorNoOutputLayoutBinding>(
            layoutInflater,
            R.layout.error_no_output_layout,
            parent,
            false
        )

        errorViewBinding?.apply {
            parent.addView(root)
            dependantView?.let {
                errorLayoutRoot.updateLayout(
                    marginTop = it.measuredHeight,
                    marginBottom = it.measuredHeight
                )
            }

            if (margin != 0) {
                errorLayoutRoot.updateLayout(
                    marginBottom = margin
                )
            }

            errorImage.setImageDrawable(ContextCompat.getDrawable(parent.context, errorImg))

            errorText.text = errorMessage
            if (errorActionEnabled) {
                errorActionBtn.visibility = View.VISIBLE
                errorActionBtn.text = errorActionLabel

                errorActionBtn.setOnClickListener {
                    errorActionBtn.visibility = View.GONE
                    errorRetryProgress.visibility = View.VISIBLE
                    onActionClick?.let { it(errorActionBtn, errorRetryProgress) }
                }
            } else {
                errorActionBtn.visibility = View.GONE
            }
        }

        return errorViewBinding
    }
}