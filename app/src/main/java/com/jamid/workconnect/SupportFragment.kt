package com.jamid.workconnect

import android.content.Context
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels

abstract class SupportFragment(@LayoutRes layout: Int) : Fragment(layout) {

	val viewModel: MainViewModel by activityViewModels()
	lateinit var activity: MainActivity

	override fun onAttach(context: Context) {
		super.onAttach(context)
		activity = context as MainActivity
	}

}