package com.jamid.workconnect

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels

abstract class SupportFragment(@LayoutRes layout: Int, fragmentTag: String, isPrimaryFragment: Boolean) : Fragment(layout) {

	val viewModel: MainViewModel by activityViewModels()
	lateinit var activity: MainActivity
	private val childFragmentTag = fragmentTag
	private val mPrimaryFragment = isPrimaryFragment
	private val mLayoutRes = layout
	var insetBasedParent: ViewGroup? = null
	open val insetTop = "INSET_TOP"
	open val insetBottom = "INSET_BOTTOM"

	override fun onAttach(context: Context) {
		super.onAttach(context)
		activity = context as MainActivity
	}

	fun setInsetView(parent: ViewGroup, extras: Map<String, Int>? = null) {
		insetBasedParent = parent

		viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
			val topInset = extras?.get(insetTop) ?: 0
			val bottomInset = extras?.get(insetBottom) ?: 0
			parent.setPadding(0, top + convertDpToPx(topInset), 0, bottom + convertDpToPx(bottomInset))
		}
	}

}