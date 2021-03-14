package com.jamid.workconnect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.jamid.workconnect.databinding.FragmentNotificationBinding

class NotificationFragment : BaseBottomSheetFragment() {

    private lateinit var binding: FragmentNotificationBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_notification, container, false)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.notificationPager.adapter = NotificationPager(requireActivity())

        TabLayoutMediator(binding.notificationTabs, binding.notificationPager) { t, p ->
            when (p) {
                0 -> t.text = "General"
                1 -> t.text = "Requests"
            }
        }.attach()

        binding.cancelNotificationsBtn.setOnClickListener {
            findNavController().navigateUp()
        }

    }

    companion object {

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