package com.jamid.workconnect.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.jamid.workconnect.CREATE_MENU
import com.jamid.workconnect.GenericMenuFragment
import com.jamid.workconnect.R
import com.jamid.workconnect.SupportFragment
import com.jamid.workconnect.databinding.FragmentHomeBinding
import com.jamid.workconnect.model.GenericMenuItem

@ExperimentalPagingApi
class HomeFragment : SupportFragment(R.layout.fragment_home, TAG, true) {

    private lateinit var binding: FragmentHomeBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentHomeBinding.bind(view)

        binding.homePager.adapter = HomeFragmentPager(activity)
        binding.homePager.isNestedScrollingEnabled = true
        (binding.homePager.getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER

        activity.mainBinding.primaryMenuBtn.setOnClickListener {
            val tag = CREATE_MENU
            val item1 = GenericMenuItem(tag, "Create new Blog", R.drawable.ic_baseline_note_24, 0)
            val item2 = GenericMenuItem(tag, "Create new Project", R.drawable.ic_baseline_architecture_24, 1)

            val fragment = GenericMenuFragment.newInstance(tag, "Create New ...", arrayListOf(item1, item2))
            activity.showBottomSheet(fragment, tag)
        }

    }

    inner class HomeFragmentPager(fa: FragmentActivity): FragmentStateAdapter(fa) {
        override fun getItemCount() = 1


        override fun createFragment(position: Int): Fragment {
            return PostsFragment.newInstance()
        }
    }

    companion object {
        const val TAG = "HomeFragment"
        const val TITLE = "Worknet"

        @JvmStatic
        fun newInstance() = HomeFragment()
    }

}