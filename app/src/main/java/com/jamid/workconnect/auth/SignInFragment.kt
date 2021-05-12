package com.jamid.workconnect.auth

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.jamid.workconnect.*
import com.jamid.workconnect.databinding.FragmentSignInBinding
import com.jamid.workconnect.model.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class SignInFragment : SupportFragment(R.layout.fragment_sign_in, TAG, false) {

    private lateinit var binding: FragmentSignInBinding
    private var job: Job? = null
    private var job1: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSignInBinding.bind(view)
        setInsetView(binding.signInScroller, mapOf(insetTop to 56))

        viewModel.signInResult.observe(viewLifecycleOwner) {
            val result = it ?: return@observe

            binding.signInProgress.visibility = View.GONE
            binding.signInRegisterBtn.visibility = View.VISIBLE

            when (result) {
                is Result.Success -> {
                    activity.onBackPressed()
                }
                is Result.Error -> {
                    viewModel.setCurrentError(result.exception)
                }
            }
        }

        viewModel.registerResult.observe(viewLifecycleOwner) {
            val result = it ?: return@observe

            binding.signInProgress.visibility = View.GONE
            binding.signInRegisterBtn.visibility = View.VISIBLE

            when (result) {
                is Result.Success -> {
                    activity.toFragment(UserDetailFragment.newInstance(), UserDetailFragment.TAG)
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
                    binding.emailProgress.visibility = View.GONE
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
                job?.cancel()
                binding.emailText.error = null
                binding.emailText.isErrorEnabled = false
                val email = binding.emailText.editText?.text.toString()
                val password = binding.passwordText.editText?.text.toString()
                job = lifecycleScope.launch {
                    delay(2000)
                    binding.emailProgress.visibility = View.VISIBLE
                    viewModel.validateSignInForm(email, password)
                    viewModel.checkIfEmailExists(email)
                    viewModel.checkIfUsernameExists(email.split('@')[0])
                    delay(5000)
                    binding.emailProgress.visibility = View.GONE
                }
            } else {
                job?.cancel()
                binding.emailProgress.visibility = View.GONE
            }
        }

        binding.passwordText.editText?.doAfterTextChanged {
            if (!it.isNullOrBlank()) {
                job1?.cancel()
                binding.passwordText.error = null
                binding.passwordText.isErrorEnabled = false
                val email = binding.emailText.editText?.text.toString()
                val password = binding.passwordText.editText?.text.toString()
                job1 = lifecycleScope.launch {
                    delay(1500)
                    viewModel.validateSignInForm(email, password)
                }
            }
        }

        binding.passwordText.editText?.setOnEditorActionListener { v, actionId, event ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    binding.signInRegisterBtn.requestFocus()
                    hideKeyboard()
                    if (binding.signInRegisterBtn.isEnabled) {
                        signInRegister()
                    }
                }
            }
            true
        }

        binding.signInRegisterBtn.setOnClickListener {
            signInRegister()
        }


        OverScrollDecoratorHelper.setUpOverScroll(binding.signInScroller)

    }

    private fun signInRegister() {
        val emailExists = viewModel.emailExists.value ?: return

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

    override fun onDestroy() {
        super.onDestroy()
        viewModel.clearSignInChanges()
    }

    companion object {
        const val TAG = "SignInFragment"
        const val TITLE = "Sign in Or Register"

        @JvmStatic
        fun newInstance() = SignInFragment()
    }
}