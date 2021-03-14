package com.jamid.workconnect

import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jamid.workconnect.databinding.FragmentImageSelectBinding
import com.jamid.workconnect.interfaces.ImageSelectMenuListener

class ImageSelectFragment : Fragment(R.layout.fragment_image_select) {

    private lateinit var binding: FragmentImageSelectBinding
    private val viewModel: MainViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentImageSelectBinding.bind(view)
        val activity = requireActivity() as MainActivity
        val imageSelectMenuListener = activity as ImageSelectMenuListener

        binding.selectFromGalleryBtn.setOnClickListener {
            imageSelectMenuListener.onSelectImageFromGallery()
        }

        binding.removeImgBtn.setOnClickListener {
            imageSelectMenuListener.onImageRemove()
        }

        binding.dismissBtn.setOnClickListener {
            activity.bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            val params = binding.dismissBtn.layoutParams as ConstraintLayout.LayoutParams
            params.setMargins(0, 0, 0, bottom)
            binding.dismissBtn.layoutParams = params
        }


    }

    companion object {

        const val TAG = "ImageSelectMenu"

        @JvmStatic
        fun newInstance() = ImageSelectFragment()
    }
}