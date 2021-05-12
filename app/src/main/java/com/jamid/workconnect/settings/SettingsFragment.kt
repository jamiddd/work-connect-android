package com.jamid.workconnect.settings

import android.os.Bundle
import android.view.View
import com.jamid.workconnect.R
import com.jamid.workconnect.SupportFragment
import com.jamid.workconnect.databinding.FragmentSettingsBinding
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class SettingsFragment : SupportFragment(R.layout.fragment_settings, TAG, false) {

	private lateinit var binding: FragmentSettingsBinding

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding = FragmentSettingsBinding.bind(view)

		setInsetView(binding.settingsScroll, mapOf(insetTop to 64, insetBottom to 64))

		OverScrollDecoratorHelper.setUpOverScroll(binding.settingsScroll)

	}

	companion object {

		const val TAG = "SettingsFragment"

		@JvmStatic
		fun newInstance() =
			SettingsFragment()
	}
}