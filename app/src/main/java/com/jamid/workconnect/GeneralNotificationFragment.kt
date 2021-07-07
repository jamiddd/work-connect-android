package com.jamid.workconnect

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.google.android.material.appbar.AppBarLayout
import com.jamid.workconnect.adapter.paging2.NotificationAdapter
import com.jamid.workconnect.databinding.FragmentGeneralNotificationBinding
import com.jamid.workconnect.model.Result
import com.jamid.workconnect.model.SimpleNotification
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GeneralNotificationFragment : PagingListFragment(R.layout.fragment_general_notification) {

    private lateinit var binding: FragmentGeneralNotificationBinding
    private lateinit var notificationAdapter: NotificationAdapter<SimpleNotification>
    private var errorView: View? = null

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
        errorView?.let {
            binding.generalNotificationRoot.removeView(it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentGeneralNotificationBinding.bind(view)
        binding.notificationsRefresher.isRefreshing = true
        notificationAdapter = NotificationAdapter(SimpleNotification::class.java)
        binding.notificationsRecycler.setListAdapter(pagingAdapter = notificationAdapter,
            clazz = SimpleNotification::class.java,
            onComplete = {
                viewModel.user.observe(viewLifecycleOwner) { user ->
                    if (user != null) {
                        notifications()
                    } else {
                        showEmptyNotificationsUI()
                    }
                }
            },
            onEmptySet = {
                binding.notificationsRefresher.isRefreshing = false
                showEmptyNotificationsUI()
            }, onNonEmptySet = {
                binding.notificationsRefresher.isRefreshing = false
                hideEmptyNotificationUI()
            })

        binding.notificationsRefresher.setSwipeRefresher {
            notifications()
        }

        setListeners()

        viewModel.windowInsets.observe(viewLifecycleOwner) { (_, bottom) ->
            binding.notificationsRecycler.setPadding(0, 0, 0, convertDpToPx(56) + bottom)
        }

    }

    private fun showEmptyNotificationsUI() {
        binding.notificationsRefresher.visibility = View.GONE
        if (errorView != null) {
            binding.generalNotificationRoot.removeView(errorView)
            errorView = null
        }

        val errorViewBinding = if (viewModel.user.value != null) {
            setErrorLayout(binding.generalNotificationRoot, "No notifications at the moment.", errorImg = R.drawable.ic_empty_notifications, margin = convertDpToPx(109)) { b, p ->
                binding.notificationsRefresher.isRefreshing = true
                notifications()
            }
        } else {
            setErrorLayout(binding.generalNotificationRoot, "No notifications at the moment.", errorImg = R.drawable.ic_empty_notifications, margin = convertDpToPx(109), errorActionEnabled = false)
        }

        errorView = errorViewBinding.root

        /*val co = intArrayOf(0, 0)
        errorViewBinding.errorActionBtn.getLocationOnScreen(co)

        val windowHeight = getWindowHeight()
        val bottomHeight = activity.mainBinding.bottomCard.measuredHeight
        val bottomCardPositionFromTop = windowHeight - bottomHeight

        if (co[1] + errorViewBinding.errorActionBtn.measuredHeight > bottomCardPositionFromTop) {
            activity.findViewById<AppBarLayout>(R.id.notificationAppBar)?.setExpanded(false, true)
        }*/

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