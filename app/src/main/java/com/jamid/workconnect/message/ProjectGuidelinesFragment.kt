package com.jamid.workconnect.message

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.MainActivity
import com.jamid.workconnect.R
import com.jamid.workconnect.databinding.FragmentProjectGuidelinesBinding
import com.jamid.workconnect.hideKeyboard
import com.jamid.workconnect.model.ChatChannelContributor
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.Result
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class ProjectGuidelinesFragment : Fragment(R.layout.fragment_project_guidelines) {

    private lateinit var binding: FragmentProjectGuidelinesBinding
    private val auth = Firebase.auth
    val viewModel: ProjectDetailViewModel by navGraphViewModels(R.id.project_detail_navigation)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity() as MainActivity

        binding = FragmentProjectGuidelinesBinding.bind(view)

        val toolbar = activity.findViewById<MaterialToolbar>(R.id.pdc_toolbar)

        OverScrollDecoratorHelper.setUpStaticOverScroll(binding.pgContentScroller, OverScrollDecoratorHelper.ORIENTATION_VERTICAL)

        viewModel.guidelinesUpdateResult.observe(viewLifecycleOwner) {
            val result = it ?: return@observe

            when (result) {
                is Result.Success -> {
                    viewModel.postGuidelinesUpdate(result.data) {
                        activity.mainBinding.primaryProgressBar.visibility = View.GONE
                        findNavController().navigateUp()
                    }
                }
                is Result.Error -> {
                    Toast.makeText(activity, result.exception.localizedMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.currentContributor.observe(viewLifecycleOwner) {
            if (it != null) {
                if (auth.currentUser != null) {
                    if (auth.currentUser?.uid == it.id && it.admin) {
                        binding.editModeNotification.visibility = View.VISIBLE
                        toolbar.inflateMenu(R.menu.project_guidelines_menu)
                    } else {
                        binding.pgText.isEnabled = false
                    }
                }

            }
        }

        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.pg_save -> {
                    activity.mainBinding.primaryProgressBar.visibility = View.VISIBLE
                    val text = binding.pgText.text
                    if (text.isNotEmpty()) {
                        hideKeyboard()
                        val guidelines = text.toString()
                        viewModel.updateGuidelines(guidelines)
                    }
                }
            }
            true
        }

        activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            hideKeyboard()
            findNavController().navigateUp()
        }

        viewModel.currentPost.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.pgText.setText(it.guidelines)
            }
        }

        /*binding.pgToolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.pg_save -> {
                    val text = binding.pgText.text
                    if (!text.isNullOrBlank()) {
                        db.collection(POSTS).document(post.id).update("guidelines", text.toString())
                            .addOnSuccessListener {
                                Toast.makeText(
                                    requireContext(),
                                    "Guidelines updated successfully.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                dismiss()
                            }.addOnFailureListener {
                                Toast.makeText(
                                    requireContext(),
                                    "Something went wrong",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }
            }
            true
        }*/

        /*mainViewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            val windowHeight = getWindowHeight() + top + bottom

            val params = binding.root.layoutParams as ViewGroup.LayoutParams
            params.height = windowHeight
            params.width = ViewGroup.LayoutParams.MATCH_PARENT

            binding.root.layoutParams = params

        }*/

    }

    companion object {

        const val TAG = "ProjectGuidelines"
        private const val ARG_POST = "ARG_POST"
        private const val ARG_CURRENT_CONTRIBUTOR = "ARG_CURRENT_CONTRIBUTOR"

        @JvmStatic
        fun newInstance(post: Post, currentContributor: ChatChannelContributor) =
            ProjectGuidelinesFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_POST, post)
                    putParcelable(ARG_CURRENT_CONTRIBUTOR, currentContributor)
                }
            }
    }
}