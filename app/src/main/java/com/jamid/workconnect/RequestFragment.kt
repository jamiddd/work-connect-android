package com.jamid.workconnect

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.workconnect.adapter.paging2.NotificationAdapter
import com.jamid.workconnect.databinding.FragmentRequestBinding
import com.jamid.workconnect.interfaces.OnRefreshListener
import com.jamid.workconnect.model.Result
import com.jamid.workconnect.model.SimpleRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class RequestFragment : InsetControlFragment(R.layout.fragment_request), OnRefreshListener {

    private lateinit var binding: FragmentRequestBinding
    private lateinit var activeRequestsAdapter: NotificationAdapter<SimpleRequest>
    private var job: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentRequestBinding.bind(view)
        activeRequestsAdapter = NotificationAdapter(activity, SimpleRequest::class.java)

        setInsetView(binding.requestsRecycler, mapOf(INSET_TOP to 104, INSET_BOTTOM to 56, PROGRESS_OFFSET to 40))
        setRefreshListener(this)

        OverScrollDecoratorHelper.setUpOverScroll(binding.noRequestsLayoutScroll)

        viewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {

                binding.requestsRecycler.apply {
                    layoutManager = LinearLayoutManager(activity)
                    adapter = activeRequestsAdapter
                }

                setOverScrollView(binding.requestsRecycler)

                viewModel.activeRequests(user.id).observe(viewLifecycleOwner) {
                    if (it.isNotEmpty()) {
                        job?.cancel()

                        setOverScrollView(binding.requestsRecycler)
                        activity.mainBinding.primaryProgressBar.visibility = View.GONE
                        binding.noRequestsLayoutScroll.visibility = View.GONE
                        binding.requestsRecycler.visibility = View.VISIBLE

                        activeRequestsAdapter.submitList(it)
                    } else {
                        viewModel.clearAndFetchNewRequests()

                        activity.mainBinding.primaryProgressBar.visibility = View.VISIBLE
                        job = lifecycleScope.launch {
                            delay(2000)
                            activity.mainBinding.primaryProgressBar.visibility = View.GONE

                            binding.requestsRecycler.visibility = View.GONE
                            binding.noRequestsLayoutScroll.visibility = View.VISIBLE
                        }

                    }
                }
            } else {
                binding.requestsRecycler.visibility = View.GONE
                binding.noRequestsLayoutScroll.visibility = View.VISIBLE
            }
        }

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

    override fun onRefreshStart() {
        viewModel.clearAndFetchNewRequests()
        lifecycleScope.launch {
            delay(2000)
            setOverScrollView(binding.requestsRecycler)
        }
    }

}