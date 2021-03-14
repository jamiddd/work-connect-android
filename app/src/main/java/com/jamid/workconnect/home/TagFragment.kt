package com.jamid.workconnect.home

import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jamid.workconnect.MainActivity
import com.jamid.workconnect.MainViewModel
import com.jamid.workconnect.R
import com.jamid.workconnect.convertDpToPx
import com.jamid.workconnect.databinding.FragmentTagBinding


class TagFragment : Fragment(R.layout.fragment_tag) {

    private lateinit var binding: FragmentTagBinding
    private val viewModel: MainViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity() as MainActivity

        binding = FragmentTagBinding.bind(view)

        binding.addTagBtn.isEnabled = false

        binding.tagEditText.doAfterTextChanged {
            binding.addTagBtn.isEnabled = !it.isNullOrBlank()
        }

        binding.addTagBtn.setOnClickListener {
            val tag = binding.tagEditText.text
            viewModel.setTag(tag.trim().toString())
            activity.bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) {(top, bottom) ->
            val params = binding.addTagBtn.layoutParams as ConstraintLayout.LayoutParams
            params.startToStart = binding.tagEditText.id
            params.endToEnd = binding.tagEditText.id
            params.bottomToBottom = binding.tagLayoutRoot.id
            params.topToBottom = binding.tagEditText.id
            params.horizontalBias = 0.5f
            params.verticalChainStyle = ConstraintLayout.LayoutParams.CHAIN_PACKED
            params.verticalBias = 0.5f
            params.setMargins(convertDpToPx(8), convertDpToPx(8), convertDpToPx(8), bottom + convertDpToPx(8))
            binding.addTagBtn.layoutParams = params

            binding.tagEditText.requestFocus()
        }

    }

    companion object {

        const val TAG = "TagFragment"

        @JvmStatic
        fun newInstance() = TagFragment()
    }
}