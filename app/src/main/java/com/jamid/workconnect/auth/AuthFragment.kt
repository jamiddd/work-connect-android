package com.jamid.workconnect.auth

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jamid.workconnect.MainActivity
import com.jamid.workconnect.MainViewModel
import com.jamid.workconnect.R
import com.jamid.workconnect.databinding.FragmentAuthBinding


class AuthFragment : Fragment(R.layout.fragment_auth) {

    private lateinit var binding: FragmentAuthBinding
    private val viewModel: MainViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAuthBinding.bind(view)
        val activity = requireActivity() as MainActivity
        binding.secondaryBtn.setOnClickListener {
            activity.bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        val navHostFragment = childFragmentManager.findFragmentById(R.id.authFragmentContainer) as NavHostFragment
        val navController = navHostFragment.navController

        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            when (destination.id) {
                R.id.signInFragment -> {
                    binding.primaryBtn.visibility = View.INVISIBLE
                    binding.secondaryBtn.visibility = View.VISIBLE
                }
                R.id.userDetailFragment -> {
                    binding.primaryBtn.visibility = View.VISIBLE
                    binding.secondaryBtn.visibility = View.INVISIBLE
                    binding.primaryBtn.text = "Next"
                }
                R.id.interestFragment -> {
                    binding.primaryBtn.text = "Finish"
                    binding.primaryBtn.visibility = View.VISIBLE
                    binding.secondaryBtn.visibility = View.INVISIBLE
                }
            }
        }


    }


    companion object {

        const val TAG = "AuthFragment"

        @JvmStatic
        fun newInstance() = AuthFragment()
    }
}