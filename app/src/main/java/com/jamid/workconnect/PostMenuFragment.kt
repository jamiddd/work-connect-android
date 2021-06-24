package com.jamid.workconnect

import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.databinding.FragmentPostMenuBinding
import com.jamid.workconnect.interfaces.PostMenuClickListener
import com.jamid.workconnect.model.Post

class PostMenuFragment : Fragment(R.layout.fragment_post_menu) {

    private lateinit var binding: FragmentPostMenuBinding
    private val viewModel: MainViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentPostMenuBinding.bind(view)
        val activity = requireActivity() as MainActivity
        val post = arguments?.getParcelable<Post>(ARG_POST) ?: return
        val auth = Firebase.auth

        val clickListener = activity as PostMenuClickListener

        viewModel.user.observe(viewLifecycleOwner) {
            if (it != null) {
                when {
                    /*it.activeRequests.contains(post.uid) -> {
                        binding.postJoinText.visibility = View.GONE
                    }*/
                    post.contributors?.contains(it.id) == true -> {
                        binding.postJoinText.visibility = View.GONE
                    }
                    it.id == post.uid -> {
                        binding.postJoinText.visibility = View.GONE
                    }
                    else -> {
                        if (post.type == PROJECT) {
                            binding.postJoinText.visibility = View.VISIBLE
                        } else {
                            binding.postJoinText.visibility = View.GONE
                        }
                    }
                }
            }
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            val params = binding.dismissPostMenuText.layoutParams as ConstraintLayout.LayoutParams
            params.startToStart = binding.deleteProjectText.id
            params.endToEnd = binding.deleteProjectText.id
            params.bottomToBottom = binding.postMenuRoot.id
            params.topToBottom = binding.deleteProjectText.id
            params.horizontalBias = 0.5f
            params.verticalChainStyle = ConstraintLayout.LayoutParams.CHAIN_PACKED
            params.verticalBias = 0.5f
            params.setMargins(convertDpToPx(8), convertDpToPx(8), convertDpToPx(8), bottom + convertDpToPx(8))
            binding.dismissPostMenuText.layoutParams = params
        }


        binding.postJoinText.setOnClickListener {
            clickListener.onCollaborateClick(post)
        }

        binding.postMenuTitle.text = post.title

        if (auth.currentUser?.uid == post.uid) {
            binding.deleteProjectText.visibility = View.VISIBLE
        } else {
            binding.deleteProjectText.visibility = View.GONE
        }

        binding.dismissPostMenuText.setOnClickListener {
            activity.bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        binding.deleteProjectText.setOnClickListener {
            clickListener.onDeleteClick(post)
        }


    }

    companion object {

        const val TAG = "PostMenu"
        private const val ARG_POST = "ARG_POST"

        @JvmStatic
        fun newInstance(post: Post) = PostMenuFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_POST, post)
            }
        }
    }
}