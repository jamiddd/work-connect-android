package com.jamid.workconnect

import android.os.Bundle
import android.view.*
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.drawee.view.DraweeTransition
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.jamid.workconnect.databinding.FragmentImageViewBinding
import com.jamid.workconnect.model.SimpleMessage

class ImageViewFragment : Fragment(R.layout.fragment_image_view){

    private lateinit var binding: FragmentImageViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        postponeEnterTransition()
        val root = inflater.inflate(R.layout.fragment_image_view, container, false)
        val message = arguments?.getParcelable<SimpleMessage>(ARG_MESSAGE)
        val imageView = root.findViewById<SimpleDraweeView>(R.id.fullscreenImage)
        imageView.transitionName = message?.messageId
        imageView.setImageURI(message?.content)
        /*val transition = TransitionInflater.from(activity)
            .inflateTransition(R.transition.image_shared_element_transition)*/
        sharedElementEnterTransition = DraweeTransition.createTransitionSet(ScalingUtils.ScaleType.CENTER_CROP,ScalingUtils.ScaleType.FIT_CENTER)

        startPostponedEnterTransition()

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentImageViewBinding.bind(view)

        val activity = requireActivity() as MainActivity


        activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            activity.supportFragmentManager.beginTransaction().remove(this@ImageViewFragment).commit()
        }
    }

    companion object {

        private const val ARG_MESSAGE = "ARG_MESSAGE"
        private const val ARG_IMAGE = "ARG_IMAGE"
        private const val ARG_CURRENT_POS = "ARG_CURRENT_POS"
        const val TAG = "ImageViewFragment"

        @JvmStatic
        fun newInstance(/*pos: Int, image: String*/message: SimpleMessage)
            = ImageViewFragment().apply {
                arguments = Bundle().apply {
                    /*putInt(ARG_CURRENT_POS, pos)
                    putString(ARG_IMAGE, image)*/
                    putParcelable(ARG_MESSAGE, message)
                }
            }
    }

}