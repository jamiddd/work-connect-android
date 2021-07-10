package com.jamid.workconnect.settings

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.R
import com.jamid.workconnect.SupportFragment
import com.jamid.workconnect.convertDpToPx
import com.jamid.workconnect.databinding.FragmentSettingsBinding
import com.jamid.workconnect.updateLayout

class SettingsFragment : SupportFragment(R.layout.fragment_settings) {

	private lateinit var binding: FragmentSettingsBinding

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding = FragmentSettingsBinding.bind(view)

		binding.settingsToolbar.setNavigationOnClickListener {
			findNavController().navigateUp()
		}

		binding.darkModeButton.setOnClickListener {
			binding.darkModeSwitch.toggle()
		}

		viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
			binding.settingsToolbar.updateLayout(marginTop = top)
			if (activity.mainBinding.bottomCard.translationY != 0f) {
				binding.settingsScrollContainer.setPadding(0, 0, 0, bottom)
			} else {
				binding.settingsScrollContainer.setPadding(0, convertDpToPx(8), 0, bottom + convertDpToPx(56))
			}
		}

		binding.logOutButton.setOnClickListener {
			MaterialAlertDialogBuilder(activity)
				.setTitle("Logging out ...")
				.setMessage("Are you sure you want to log out?")
				.setNegativeButton("Cancel") { d, b ->
					d.dismiss()
				}.setPositiveButton("Log out") { d, b ->
					Firebase.auth.signOut()
					viewModel.deleteWholeDatabase()
					findNavController().popBackStack(R.id.homeFragment, false)
				}.show()
		}

	}

	companion object {

		const val TAG = "SettingsFragment"

		@JvmStatic
		fun newInstance() =
			SettingsFragment()
	}
}