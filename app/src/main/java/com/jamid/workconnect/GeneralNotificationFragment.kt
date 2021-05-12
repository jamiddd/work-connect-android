package com.jamid.workconnect

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.workconnect.adapter.paging2.NotificationAdapter
import com.jamid.workconnect.databinding.FragmentGeneralNotificationBinding
import com.jamid.workconnect.interfaces.OnRefreshListener
import com.jamid.workconnect.model.Result
import com.jamid.workconnect.model.SimpleNotification
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class GeneralNotificationFragment : InsetControlFragment(R.layout.fragment_general_notification), OnRefreshListener {

    private lateinit var binding: FragmentGeneralNotificationBinding
    private lateinit var notificationAdapter: NotificationAdapter<SimpleNotification>
    private var job: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentGeneralNotificationBinding.bind(view)

        setInsetView(binding.notificationsRecycler, mapOf(INSET_TOP to 104, INSET_BOTTOM to 56, PROGRESS_OFFSET to 30))
        setRefreshListener(this)

        OverScrollDecoratorHelper.setUpOverScroll(binding.noNotificationsLayoutScroll)

        viewModel.miniUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                notificationAdapter = NotificationAdapter(activity, SimpleNotification::class.java)

                binding.notificationsRecycler.apply {
                    layoutManager = LinearLayoutManager(activity)
                    adapter = notificationAdapter
                }

                /*binding.refreshNotificationBtn.visibility = View.VISIBLE
                binding.refreshNotificationBtn.setOnClickListener {
                    binding.refreshNotificationBtn.isEnabled = !binding.refreshNotificationBtn.isEnabled
                    viewModel.getNotifications()
                }*/

                setOverScrollView(binding.notificationsRecycler)

                viewModel.notifications(user.id).observe(viewLifecycleOwner) {
                    if (it.isNotEmpty()) {
                        job?.cancel()
                        setOverScrollView(binding.notificationsRecycler)

//                        binding.refreshNotificationBtn.isEnabled = true

                        activity.mainBinding.primaryProgressBar.visibility = View.GONE
                        binding.notificationsRecycler.visibility = View.VISIBLE
                        binding.noNotificationsLayoutScroll.visibility = View.GONE

                        notificationAdapter.submitList(it)
                    } else {
                        viewModel.getNotifications()
                        activity.mainBinding.primaryProgressBar.visibility = View.VISIBLE

                        job = lifecycleScope.launch {
                            delay(2000)
                            activity.mainBinding.primaryProgressBar.visibility = View.GONE
                            binding.notificationsRecycler.visibility = View.GONE
                            binding.noNotificationsLayoutScroll.visibility = View.VISIBLE
                        }
                    }
                }
            } else {
                binding.notificationsRecycler.visibility = View.GONE
                binding.noNotificationsLayoutScroll.visibility = View.VISIBLE
            }
        }

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

    override fun onRefreshStart() {
        viewModel.getNotifications()
        lifecycleScope.launch {
            delay(2000)
            setOverScrollView(binding.notificationsRecycler)
        }
    }

}