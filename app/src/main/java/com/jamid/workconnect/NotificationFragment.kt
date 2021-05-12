package com.jamid.workconnect

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.jamid.workconnect.databinding.FragmentNotificationBinding
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class NotificationFragment : SupportFragment(R.layout.fragment_notification, TAG, true) {

    private lateinit var binding: FragmentNotificationBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentNotificationBinding.bind(view)
        binding.notificationPager.adapter = NotificationPager(activity)

        OverScrollDecoratorHelper.setUpOverScroll(binding.notificationPager.getChildAt(0) as RecyclerView, OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL)

        TabLayoutMediator(activity.mainBinding.primaryTabs, binding.notificationPager) { t, p ->
            when (p) {
                0 -> t.text = "General"
                1 -> t.text = "Requests"
            }
        }.attach()

    }

    companion object {
        const val TITLE = "Notifications"
        const val TAG = "NotificationFragment"

        @JvmStatic
        fun newInstance() = NotificationFragment()
    }

    inner class NotificationPager(fa: FragmentActivity): FragmentStateAdapter(fa) {
        override fun getItemCount() = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> GeneralNotificationFragment.newInstance()
                1 -> RequestFragment.newInstance()
                else -> GeneralNotificationFragment.newInstance()
            }
        }
    }
}