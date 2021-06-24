package com.jamid.workconnect

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.workconnect.adapter.paging2.NotificationAdapter
import com.jamid.workconnect.databinding.FragmentRequestBinding
import com.jamid.workconnect.model.Result
import com.jamid.workconnect.model.SimpleRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RequestFragment : InsetControlFragment(R.layout.fragment_request) {

    private lateinit var binding: FragmentRequestBinding
    private lateinit var activeRequestsAdapter: NotificationAdapter<SimpleRequest>
    private var job: Job? = null

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

        binding.noRequestsLayoutScroll.setPadding(0, 0, 0, viewModel.windowInsets.value!!.second + convertDpToPx(56))

        initAdapter()

        initRefresher()

        viewModel.user.observe(viewLifecycleOwner) {
            if (it != null) {
                requests()
            } else {
                showEmptyRequestsUI()
            }
        }

        setListeners()

    }

    private fun initRefresher() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (activity.resources?.configuration?.isNightModeActive == true) {
                binding.requestsRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.darkestGrey))
                binding.requestsRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.purple_200))
            } else {
                binding.requestsRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.white))
                binding.requestsRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.blue_500))
            }
        } else {
            if (activity.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
                binding.requestsRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.darkerGrey))
                binding.requestsRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.purple_200))
            } else {
                binding.requestsRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.blue_500))
                binding.requestsRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.white))
            }
        }

        binding.requestsRefresher.setOnRefreshListener {
            requests()
        }

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            activeRequestsAdapter.loadStateFlow.collectLatest { loadStates ->
                binding.requestsRefresher.isRefreshing = true

                if (loadStates.refresh is LoadState.NotLoading) {
                    delay(1000)
                    binding.requestsRefresher.isRefreshing = false
                    if (activeRequestsAdapter.itemCount == 0) {
                        showEmptyRequestsUI()
                    } else {
                        hideEmptyRequestsUI()
                    }
                }
            }
        }
    }

    private fun hideProgressBar()  = viewLifecycleOwner.lifecycleScope.launchWhenCreated {
        binding.requestsRefresher.isRefreshing = false
    }

    private fun initAdapter() {

        activeRequestsAdapter = NotificationAdapter(SimpleRequest::class.java)

        binding.requestsRecycler.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = activeRequestsAdapter
        }

    }

    private fun showEmptyRequestsUI() {
        binding.requestsRefresher.visibility = View.GONE
        binding.noRequestsLayoutScroll.visibility = View.VISIBLE
    }

    private fun hideEmptyRequestsUI() {
        binding.requestsRefresher.visibility = View.VISIBLE
        binding.noRequestsLayoutScroll.visibility = View.GONE
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