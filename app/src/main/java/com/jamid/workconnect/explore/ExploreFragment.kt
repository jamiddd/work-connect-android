package com.jamid.workconnect.explore

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.jamid.workconnect.MainActivity
import com.jamid.workconnect.MainViewModel
import com.jamid.workconnect.R
import com.jamid.workconnect.databinding.FragmentExploreBinding
import com.jamid.workconnect.hide

class ExploreFragment : Fragment(R.layout.fragment_explore) {

    private lateinit var binding: FragmentExploreBinding
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var activity: MainActivity

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = requireActivity() as MainActivity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentExploreBinding.bind(view)

        binding.explorePager.adapter = ExploreFragmentPager(requireActivity())
        binding.explorePager.isUserInputEnabled = false


        val initialBottomInset = 132
        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->

            activity.mainBinding.primarySearchLayout.setTransitionDuration(150)
            if (bottom > initialBottomInset) {
                activity.mainBinding.bottomNav.hide(activity.mainBinding.bottomBlur)
                findNavController().navigate(R.id.searchFragment)
            }

            if (activity.mainBinding.cancelSearchBtn.translationX == 0f) {
                activity.mainBinding.primarySearchLayout.transitionToStart()
            }

        }

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

        @JvmStatic
        fun newInstance() = ExploreFragment()
    }
}