package com.jamid.workconnect

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.jamid.workconnect.databinding.FragmentNotificationBinding
import com.jamid.workconnect.profile.ProfileFragment

class NotificationFragment : SupportFragment(R.layout.fragment_notification, TAG, true) {

    private lateinit var binding: FragmentNotificationBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentNotificationBinding.bind(view)
        binding.notificationPager.adapter = NotificationPager(activity)


        val options = navOptions {
            anim {
                enter = R.anim.slide_in_right
                exit = R.anim.slide_out_left
                popEnter = R.anim.slide_in_left
                popExit = R.anim.slide_out_right
            }
        }

        viewModel.user.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.accountImg.setImageURI(it.photo)
                binding.accountImg.setOnClickListener { _ ->
                    findNavController().navigate(R.id.profileFragment, Bundle().apply { putParcelable(ProfileFragment.ARG_USER, it) }, options)
                }
            } else {
                binding.accountImg.setOnClickListener {
                    findNavController().navigate(R.id.signInFragment, null, options)
                }
            }
        }

        TabLayoutMediator(binding.notificationTabLayout, binding.notificationPager) { t, p ->
            when (p) {
                0 -> t.text = "General"
                1 -> t.text = "Requests"
            }
        }.attach()

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, _) ->
            binding.notificationFragmentToolbar.updateLayout(marginTop = top)
            binding.accountImg.updateLayout(marginTop = top + convertDpToPx(10), marginRight = convertDpToPx(16))
//            binding.notificationAppBar.setPadding(0, top, 0, 0)
        }

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