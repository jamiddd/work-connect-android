package com.jamid.workconnect

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import com.jamid.workconnect.adapter.paging2.NotificationAdapter
import com.jamid.workconnect.databinding.FragmentRequestBinding
import com.jamid.workconnect.model.Result
import com.jamid.workconnect.model.SimpleRequest
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RequestFragment : PagingListFragment(R.layout.fragment_request) {

    private lateinit var binding: FragmentRequestBinding
    private lateinit var activeRequestsAdapter: NotificationAdapter<SimpleRequest>
    private var errorView: View? = null

    private fun requests() {
        job?.cancel()
        job = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activeRequests().collectLatest {
                hideEmptyRequestsUI()
                activeRequestsAdapter.submitData(it)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentRequestBinding.bind(view)

        activeRequestsAdapter = NotificationAdapter(SimpleRequest::class.java)
        binding.requestsRefresher.isRefreshing = true
        binding.requestsRecycler.setListAdapter(
            pagingAdapter = activeRequestsAdapter,
            clazz = SimpleRequest::class.java,
            onComplete = {
                viewModel.user.observe(viewLifecycleOwner) {
                    if (it != null) {
                        requests()
                    } else {
                        showEmptyRequestsUI()
                    }
                }
            },
            onEmptySet = {
                binding.requestsRefresher.isRefreshing = false
                showEmptyRequestsUI()
            },
            onNonEmptySet = {
                binding.requestsRefresher.isRefreshing = false
                hideEmptyRequestsUI()
            },
            onNewDataArrivedOnTop = {

            })

        binding.requestsRefresher.setSwipeRefresher {
            requests()
        }

        setListeners()

        viewModel.windowInsets.observe(viewLifecycleOwner) { (_, bottom) ->
            binding.requestsRecycler.setPadding(0, 0, 0, bottom + convertDpToPx(56))
        }

    }

    private fun showEmptyRequestsUI() {
        binding.requestsRefresher.visibility = View.GONE
        if (errorView != null) {
            binding.requestFragmentRoot.removeView(errorView)
            errorView = null
        }

        val errorViewBinding = if (viewModel.user.value != null) {
            setErrorLayout(
                binding.requestFragmentRoot,
                "No active requests at the moment.\n Try collaborating in a project. Your requests will appear here.",
                R.drawable.ic_empty_alerts,
                margin = convertDpToPx(109)
            ) { b, p ->
                binding.requestsRefresher.isRefreshing = true
                requests()
            }
        } else {
            setErrorLayout(
                binding.requestFragmentRoot,
                "No active requests at the moment.\n Try collaborating in a project. Your requests will appear here.",
                R.drawable.ic_empty_alerts,
                errorActionEnabled = false,
                margin = convertDpToPx(109)
            )
        }

        errorView = errorViewBinding.root

        /*val appBar = activity.findViewById<AppBarLayout>(R.id.notificationAppBar)

        val co = intArrayOf(0, 0)
        errorViewBinding.errorActionBtn.getLocationOnScreen(co)

        val co1 = intArrayOf(0, 0)
        activity.mainBinding.bottomCard.getLocationOnScreen(co1)

        if (co[1] + convertDpToPx(48) > co1[1]) {
            appBar?.setExpanded(false, true)
        }

        Log.d(BUG_TAG, "${co[1]} -- ${co1[1]}")*/


    }

    private fun hideEmptyRequestsUI() {
        binding.requestsRefresher.visibility = View.VISIBLE
        errorView?.let {
            binding.requestFragmentRoot.removeView(it)
        }
    }

    private fun setListeners() {
        viewModel.undoProjectResult.observe(viewLifecycleOwner) {
            val result = it ?: return@observe

            when (result) {
                is Result.Success -> {
                    Log.d(TAG, "Accepted - " + result.data.toString())
                }
                is Result.Error -> {
                    viewModel.setCurrentError(result.exception)
                }
            }
        }
    }

    companion object {

        private const val TAG = "RequestFragment"

        @JvmStatic
        fun newInstance() = RequestFragment()
    }

}