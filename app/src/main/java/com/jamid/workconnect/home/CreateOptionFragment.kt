package com.jamid.workconnect.home

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
import com.jamid.workconnect.databinding.FragmentCreateOptionBinding
import com.jamid.workconnect.interfaces.CreateMenuListener

class CreateOptionFragment : Fragment(R.layout.fragment_create_option) {

    private lateinit var binding: FragmentCreateOptionBinding
    private val viewModel: MainViewModel by activityViewModels()
    private var onBackPressedCallback: OnBackPressedCallback? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity() as MainActivity
        binding = FragmentCreateOptionBinding.bind(view)

        onBackPressedCallback = activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            activity.bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        val listener = activity as CreateMenuListener

        binding.dismissCreateMenuBtn.setOnClickListener {
            activity.bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        binding.newBlogBtn.setOnClickListener {
            listener.onCreateBlog()
        }

        binding.newProjectBtn.setOnClickListener {
            listener.onCreateProject()
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            val params = binding.dismissCreateMenuBtn.layoutParams as ConstraintLayout.LayoutParams
            params.setMargins(0, 0, 0, bottom)
            binding.dismissCreateMenuBtn.layoutParams = params
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        onBackPressedCallback?.remove()
    }

    companion object {

        const val TAG = "CreateOptionMenu"

        @JvmStatic
        fun newInstance() = CreateOptionFragment()
    }
}