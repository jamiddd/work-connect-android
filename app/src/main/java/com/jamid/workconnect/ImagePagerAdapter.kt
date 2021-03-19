package com.jamid.workconnect

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.jamid.workconnect.model.SimpleMessage

class ImagePagerAdapter(fa: FragmentActivity, private val images: List<String>): FragmentStateAdapter(fa) {

    override fun getItemCount() = images.size

    override fun createFragment(position: Int): Fragment {
        return ImageViewFragment.newInstance(/*position, images[position]*/SimpleMessage())
    }

}