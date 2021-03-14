package com.jamid.workconnect

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.jamid.workconnect.model.User
import com.jamid.workconnect.profile.BlogsFragment
import com.jamid.workconnect.profile.CollaborationsListFragment
import com.jamid.workconnect.profile.ProjectListFragment

class ProfileFragmentPager(val user: User?, fa: FragmentActivity): FragmentStateAdapter(fa) {

    override fun getItemCount() = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ProjectListFragment.newInstance(user)
            1 -> CollaborationsListFragment.newInstance(user)
            2 -> BlogsFragment.newInstance(user)
            else -> ProjectListFragment.newInstance(user)
        }
    }

}