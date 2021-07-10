package com.jamid.workconnect.explore

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.jamid.workconnect.R
import com.jamid.workconnect.SupportFragment
import com.jamid.workconnect.databinding.FragmentExploreBinding

class ExploreFragment : SupportFragment(R.layout.fragment_explore) {

    private lateinit var binding: FragmentExploreBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentExploreBinding.bind(view)

        binding.explorePager.adapter = ExploreFragmentPager(activity)
        binding.explorePager.isUserInputEnabled = false

        (binding.explorePager.getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER

    }

    inner class ExploreFragmentPager(fa: FragmentActivity): FragmentStateAdapter(fa) {

        override fun getItemCount(): Int {
            return 1
        }

        override fun createFragment(position: Int): Fragment {
            return ExploreHomeFragment.newInstance()
        }
    }

    companion object {

        const val TAG = "ExploreFragment"
        const val TITLE = ""

        @JvmStatic
        fun newInstance() = ExploreFragment()
    }
}