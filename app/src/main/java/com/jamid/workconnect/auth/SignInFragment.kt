package com.jamid.workconnect.auth

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.jamid.workconnect.*
import com.jamid.workconnect.databinding.FragmentSignInBinding
import com.jamid.workconnect.model.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class SignInFragment : SupportFragment(R.layout.fragment_sign_in, TAG, false) {

    private lateinit var binding: FragmentSignInBinding
    private var job: Job? = null
    private var job1: Job? = null
    private var mGoogleSignInClient: GoogleSignInClient? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSignInBinding.bind(view)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        setListeners()

        binding.signInToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        mGoogleSignInClient = GoogleSignIn.getClient(activity, gso)

        binding.signInWithGoogleBtn.setOnClickListener {
            signInWithGoogle()
        }

        binding.signInRegisterBtn.setOnClickListener {
            signInRegister()
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, _) ->
            binding.signInToolbar.updateLayout(marginTop = top)
        }

    }

    private fun fakeSignIn() {

        hideKeyboard()

//        activity.toFragment(UserDetailFragment.newInstance(), UserDetailFragment.TAG)
    }

    private fun setListeners() {

        viewModel.signInResult.observe(viewLifecycleOwner) {
            val result = it ?: return@observe

            binding.signInProgress.visibility = View.GONE
            binding.signInRegisterBtn.visibility = View.VISIBLE

            activity.hideBottomSheet()

            when (result) {
                is Result.Success -> {
                    viewModel.fetchCurrentUser(result.data)
                    findNavController().navigateUp()
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

            activity.hideBottomSheet()

            when (result) {
                is Result.Success -> {
                    findNavController().navigate(R.id.userDetailFragment)
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
            if (it != null) {
                if (it) {
                    binding.signInRegisterBtn.text = "Sign In"
                } else {
                    binding.signInRegisterBtn.text = "Register"
                }
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

        binding.passwordText.editText?.doAfterTextChanged {
            if (!it.isNullOrBlank()) {
                job1?.cancel()
                binding.passwordText.error = null
                binding.passwordText.isErrorEnabled = false
                val email = binding.emailText.editText?.text.toString()
                val password = binding.passwordText.editText?.text.toString()
                job1 = viewLifecycleOwner.lifecycleScope.launch {
                    delay(1500)
                    viewModel.validateSignInForm(email, password)
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
                job = viewLifecycleOwner.lifecycleScope.launch {
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
    }

    private fun signInRegister() {

        hideKeyboard()

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

    private fun signInWithGoogle() {

        hideKeyboard()

        val signInIntent = mGoogleSignInClient?.signInIntent
        activity.requestGoogleSingInLauncher.launch(signInIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.clearSignInChanges()
    }

    companion object {
        const val TAG = "SignInFragment"
        const val TITLE = "Sign in Or Register"
        const val RC_SIGN_IN = 12

        @JvmStatic
        fun newInstance() = SignInFragment()
    }
}