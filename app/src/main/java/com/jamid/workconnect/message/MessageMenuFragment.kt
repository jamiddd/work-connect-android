package com.jamid.workconnect.message

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jamid.workconnect.MainActivity
import com.jamid.workconnect.MainViewModel
import com.jamid.workconnect.R
import com.jamid.workconnect.databinding.FragmentMessageMenuBinding

class MessageMenuFragment : Fragment(R.layout.fragment_message_menu) {

    private lateinit var binding: FragmentMessageMenuBinding
    private val viewModel: MainViewModel by activityViewModels()
    private var onBackPressedCallback: OnBackPressedCallback? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentMessageMenuBinding.bind(view)
        val activity = requireActivity() as MainActivity

        onBackPressedCallback = activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            activity.bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

//        val chatMenuClickListener = parentFragment as ChatMenuClickListener

        binding.cameraSelect.isClickable = true
        binding.cameraSelect.setOnClickListener {
//            chatMenuClickListener.onCameraSelect()
//            dismiss()
        }

        binding.gallerySelect.setOnClickListener {
//            chatMenuClickListener.onImageSelect()
        }

        binding.documentSelect.setOnClickListener {
//            chatMenuClickListener.onDocumentSelect()
//            dismiss()
        }

        binding.dismissOptions.setOnClickListener {
//            dismiss()
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            val params = binding.dismissOptions.layoutParams as ConstraintLayout.LayoutParams
            params.setMargins(0, 0, 0, bottom)
            binding.dismissOptions.layoutParams = params
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        onBackPressedCallback?.remove()
    }

    companion object {

        @JvmStatic
        fun newInstance() = MessageMenuFragment()
    }
}