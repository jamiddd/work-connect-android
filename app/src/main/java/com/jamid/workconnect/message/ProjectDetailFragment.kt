package com.jamid.workconnect.message

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.jamid.workconnect.MainActivity
import com.jamid.workconnect.MainViewModel
import com.jamid.workconnect.R
import com.jamid.workconnect.adapter.VerticalContributorsAdapter
import com.jamid.workconnect.databinding.FragmentProjectDetailBinding
import com.jamid.workconnect.interfaces.UserItemClickListener
import com.jamid.workconnect.show

class ProjectDetailFragment : Fragment(R.layout.fragment_project_detail) {

    private lateinit var binding: FragmentProjectDetailBinding
    private lateinit var verticalContributorsAdapter: VerticalContributorsAdapter
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setHasOptionsMenu(true)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentProjectDetailBinding.bind(view)
        val activity = requireActivity() as MainActivity
        val toolbar = activity.findViewById<MaterialToolbar>(R.id.pdc_toolbar)
        val appbar = activity.findViewById<AppBarLayout>(R.id.pdc_appbar)
        toolbar.menu.clear()
        val viewModel: ProjectDetailViewModel by navGraphViewModels(R.id.project_detail_navigation)

        viewModel.currentPost.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.projectDetailContent.pdContent.text = it.content
            }
        }

        viewModel.currentChatChannel.observe(viewLifecycleOwner) { chatChannel ->
            if (chatChannel != null) {
                binding.projectDetailContent.projectDetailTitle.text = chatChannel.postTitle

                mainViewModel.channelContributors(chatChannel).observe(viewLifecycleOwner) { list ->
                    if (list != null) {
                        verticalContributorsAdapter = VerticalContributorsAdapter(parentFragment?.parentFragment as UserItemClickListener)
                        binding.projectDetailContent.projectDetailContributorsList.apply {
                            adapter = verticalContributorsAdapter
                            layoutManager = LinearLayoutManager(activity)
                        }

                        val contributors = list.map {
                            it.contributor
                        }

                        verticalContributorsAdapter.submitList(contributors)
                    }
                }

            }
        }

        binding.projectDetailContent.guidelinesBtn.setOnClickListener {
            findNavController().navigate(R.id.projectGuidelinesFragment)
        }

        binding.projectDetailContent.imageLinkDocBtn.setOnClickListener {
            findNavController().navigate(R.id.mediaFragment)
        }

//        activity.mainBinding.primaryToolbar.setNavigationOnClickListener {
//            activity.supportFragmentManager.beginTransaction()
//                .remove(activity.currentBottomFragment)
//                .commit()
//        }


        activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            activity.mainBinding.primaryAppBar.show()
            val frag = activity.currentBottomFragment
            frag?.let {
                activity.currentBottomFragment = null
                activity.supportFragmentManager.beginTransaction()
                    .remove(it)
                    .commit()
            }

        }

        mainViewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
//            binding.projectDetailScroller.setPadding(0, top + convertDpToPx(56), 0, bottom + convertDpToPx(8))
        }
    }

    companion object {
        const val TAG = "ProjectDetail"

        @JvmStatic
        fun newInstance() = ProjectDetailFragment()
    }
}