package com.jamid.workconnect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.SharedElementCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.transition.TransitionInflater
import androidx.viewpager2.widget.ViewPager2

class ImagePagerFragment : Fragment() {

    private lateinit var viewPager2: ViewPager2
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        viewPager2 = inflater.inflate(R.layout.fragment_image_pager, container, false) as ViewPager2
        val activity = requireActivity() as MainActivity



        viewPager2.adapter = ImagePagerAdapter(activity, listOf())
        viewPager2.currentItem = activity.currentImagePosition

        viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                activity.currentImagePosition = position
            }
        })

        prepareSharedElementTransition(activity)

        if (savedInstanceState == null) {
            postponeEnterTransition()
        }

        return viewPager2
    }

    companion object {

        private const val ARG_ITEM_COUNT = "ARG_ITEM_COUNT"

        @JvmStatic
        fun newInstance(itemCount: Int) =
            ImagePagerFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ITEM_COUNT, itemCount)
                }
            }
    }

    private fun prepareSharedElementTransition(activity: MainActivity) {
        val transition = TransitionInflater.from(activity)
            .inflateTransition(R.transition.image_shared_element_transition)

        sharedElementEnterTransition = transition

        setEnterSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(
                names: MutableList<String>?,
                sharedElements: MutableMap<String, View>?
            ) {
                val currentFragment = (viewPager2.adapter as ImagePagerAdapter).createFragment(activity.currentImagePosition)
                val view = currentFragment.view ?: return

                names?.get(0)?.let { sharedElements?.put(it, view.findViewById(R.id.fullscreenImage)) }
            }
        })
    }
}