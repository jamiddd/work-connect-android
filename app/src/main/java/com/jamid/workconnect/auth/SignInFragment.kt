package com.jamid.workconnect.auth

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jamid.workconnect.*
import com.jamid.workconnect.databinding.FragmentSignInBinding
import com.jamid.workconnect.model.Result
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class SignInFragment : Fragment(R.layout.fragment_sign_in) {

    private lateinit var binding: FragmentSignInBinding
    private val viewModel: MainViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSignInBinding.bind(view)
        val activity = requireActivity() as MainActivity


        viewModel.signInResult.observe(viewLifecycleOwner) {
            val result = it ?: return@observe

            binding.signInProgress.visibility = View.GONE
            binding.signInRegisterBtn.visibility = View.VISIBLE

            when (result) {
                is Result.Success -> {
                    val user = result.data
                    viewModel.setFirebaseUser(user)
                    activity.bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                }
                is Result.Error -> {
                    Log.e(TAG, "Couldn't sign in due to some error - ${result.exception}")
                }
            }
        }

        viewModel.registerResult.observe(viewLifecycleOwner) {
            val result = it ?: return@observe

            binding.signInProgress.visibility = View.GONE
            binding.signInRegisterBtn.visibility = View.VISIBLE

            when (result) {
                is Result.Success -> {
                    val user = result.data
                    viewModel.setFirebaseUser(user)
                    val navOptions = navOptions {
                        anim {
                            enter = R.anim.slide_in_right
                            exit = R.anim.slide_out_left
                            popEnter = R.anim.slide_in_left
                            popExit = R.anim.slide_out_right
                        }
                    }
                    findNavController().navigate(R.id.userDetailFragment, null, navOptions)
                }
                is Result.Error -> {
                    Toast.makeText(
                        requireContext(),
                        "Couldn't sign in due to some error.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        viewModel.emailExists.observe(viewLifecycleOwner) {
            if (it) {
                binding.signInRegisterBtn.text = "Sign In"
            } else {
                binding.signInRegisterBtn.text = "Register"
            }
            binding.emailProgress.visibility = View.GONE
        }

        viewModel.signInFormResult.observe(viewLifecycleOwner) {
            val result = it ?: return@observe

            binding.signInRegisterBtn.isEnabled = result.isValid

            when {
                result.emailError != null -> {
                    binding.emailText.isErrorEnabled = true
                    binding.emailText.error = result.emailError
                }
                result.passwordError != null -> {
                    binding.passwordText.isErrorEnabled = true
                    binding.passwordText.error = result.passwordError
                }
            }
        }


        binding.emailText.editText?.doAfterTextChanged {
            if (!it.isNullOrBlank()) {
                binding.emailText.error = null
                binding.emailText.isErrorEnabled = false
                val email = binding.emailText.editText?.text.toString()
                val password = binding.passwordText.editText?.text.toString()
                viewModel.validateSignInForm(email, password)
                viewModel.checkIfEmailExists(email)
                viewModel.checkIfUsernameExists(email.split('@')[0])

                lifecycleScope.launch {
                    delay(1000)
                    binding.emailProgress.visibility = View.VISIBLE
                    delay(5000)
                    binding.emailProgress.visibility = View.GONE
                }
            } else {
                binding.emailProgress.visibility = View.GONE
            }
        }

        binding.passwordText.editText?.doAfterTextChanged {
            if (!it.isNullOrBlank()) {
                binding.passwordText.error = null
                binding.passwordText.isErrorEnabled = false
                val email = binding.emailText.editText?.text.toString()
                val password = binding.passwordText.editText?.text.toString()
                viewModel.validateSignInForm(email, password)
            }
        }

        binding.signInRegisterBtn.setOnClickListener {
            val emailExists = viewModel.emailExists.value ?: return@setOnClickListener

            binding.signInProgress.visibility = View.VISIBLE
            binding.signInRegisterBtn.visibility = View.INVISIBLE

            val email = binding.emailText.editText?.text.toString()
            val password = binding.passwordText.editText?.text.toString()

            if (emailExists) {
                viewModel.signIn(email, password)
            } else {
                viewModel.register(email, password)
            }
        }


        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            val windowHeight = getWindowHeight()
//            val rect = windowHeight - top

            val params = binding.root.layoutParams as ViewGroup.LayoutParams
            params.height = windowHeight
            params.width = ViewGroup.LayoutParams.MATCH_PARENT

            binding.root.layoutParams = params

        }

        OverScrollDecoratorHelper.setUpOverScroll(binding.root as ScrollView)


        activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            activity.bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.setSignInResult(null)
        viewModel.setRegisterResult(null)
    }

    companion object {
        const val TAG = "SignInFragment"

        @JvmStatic
        fun newInstance() = SignInFragment()
    }
}