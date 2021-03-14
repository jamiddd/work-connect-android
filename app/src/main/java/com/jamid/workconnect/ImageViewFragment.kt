package com.jamid.workconnect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.drawee.view.DraweeTransition
import com.jamid.workconnect.databinding.FragmentImageViewBinding
import com.jamid.workconnect.model.SimpleMessage

class ImageViewFragment : Fragment(){

    private lateinit var binding: FragmentImageViewBinding
    private lateinit var message: SimpleMessage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        message = arguments?.getParcelable("message") ?: return
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_image_view, container, false)
        // Inflate the layout for this fragment
        binding.fullscreenImage.transitionName = message.messageId
        sharedElementEnterTransition = DraweeTransition.createTransitionSet(ScalingUtils.ScaleType.CENTER_CROP, ScalingUtils.ScaleType.FIT_CENTER)
        sharedElementReturnTransition = DraweeTransition.createTransitionSet(ScalingUtils.ScaleType.FIT_CENTER, ScalingUtils.ScaleType.CENTER_CROP)
        startPostponedEnterTransition()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fullscreenImage.setImageURI(message.content)

        binding.fullscreenToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner) {
            findNavController().navigateUp()
        }
        
    }

    companion object {

        @JvmStatic
        fun newInstance() = ImageViewFragment()
    }

}