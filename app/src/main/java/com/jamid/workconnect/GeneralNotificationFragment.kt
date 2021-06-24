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
import com.jamid.workconnect.databinding.FragmentGeneralNotificationBinding
import com.jamid.workconnect.model.Result
import com.jamid.workconnect.model.SimpleNotification
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GeneralNotificationFragment : InsetControlFragment(R.layout.fragment_general_notification) {

    private lateinit var binding: FragmentGeneralNotificationBinding
    private lateinit var notificationAdapter: NotificationAdapter<SimpleNotification>
    private var job: Job? = null

    private fun notifications() {
        job?.cancel()
        job = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notificationsFlow().collectLatest {
                hideEmptyNotificationUI()
                notificationAdapter.submitData(it)
            }
        }
    }

    private fun hideEmptyNotificationUI() {
        binding.notificationsRefresher.visibility = View.VISIBLE
        binding.noNotificationsLayoutScroll.visibility = View.GONE
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentGeneralNotificationBinding.bind(view)

        setInsetView(binding.notificationsRecycler, mapOf(INSET_TOP to 0, INSET_BOTTOM to 56))

        binding.noNotificationsLayoutScroll.setPadding(0, 0, 0, viewModel.windowInsets.value!!.second + convertDpToPx(56))

        initAdapter()

        initRefresher()

        viewModel.miniUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                notifications()
            } else {
                Log.d(TAG, "Not showing notifications because mini user is null")
                showEmptyNotificationsUI()
            }
        }

        setListeners()

    }

    private fun initRefresher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (activity.resources?.configuration?.isNightModeActive == true) {
                binding.notificationsRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.darkestGrey))
                binding.notificationsRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.purple_200))
            } else {
                binding.notificationsRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.white))
                binding.notificationsRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.blue_500))
            }
        } else {
            if (activity.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
                binding.notificationsRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.darkerGrey))
                binding.notificationsRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.purple_200))
            } else {
                binding.notificationsRefresher.setColorSchemeColors(ContextCompat.getColor(activity, R.color.blue_500))
                binding.notificationsRefresher.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(activity, R.color.white))
            }
        }

        binding.notificationsRefresher.setOnRefreshListener {
            notifications()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            notificationAdapter.loadStateFlow.collectLatest { loadStates ->
                binding.notificationsRefresher.isRefreshing = loadStates.refresh is LoadState.Loading
                if (loadStates.refresh is LoadState.NotLoading) {
                    delay(1000)
                    if (notificationAdapter.itemCount == 0) {
                        showEmptyNotificationsUI()
                    }
                }
            }
        }
    }

    private fun hideProgressBar()  = viewLifecycleOwner.lifecycleScope.launchWhenCreated {
        binding.notificationsRefresher.isRefreshing = false
    }

    private fun showEmptyNotificationsUI() {
        binding.notificationsRefresher.visibility = View.GONE
        binding.noNotificationsLayoutScroll.visibility = View.VISIBLE
    }

    private fun initAdapter() {
        notificationAdapter = NotificationAdapter(SimpleNotification::class.java)

        binding.notificationsRecycler.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = notificationAdapter
        }
    }

    private fun setListeners() {
        viewModel.declineProjectResult.observe(viewLifecycleOwner) {
            val result = it ?: return@observe

            when (result) {
                is Result.Success -> {
                    Log.d(TAG, "Declined - " + result.data.toString())
                }
                is Result.Error -> {
                    viewModel.setCurrentError(result.exception)
                }
            }
        }
        viewModel.acceptProjectResult.observe(viewLifecycleOwner) {
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

        private const val TAG = "GeneralNotification"

        @JvmStatic
        fun newInstance() = GeneralNotificationFragment()
    }

}