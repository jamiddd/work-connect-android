package com.jamid.workconnect

import android.content.Context
import android.os.Bundle
import android.transition.TransitionInflater
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels

abstract class SupportFragment(@LayoutRes layout: Int, fragmentTag: String, isPrimaryFragment: Boolean) : Fragment() {

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

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val root = inflater.inflate(mLayoutRes, container, false)
		viewModel.setCurrentFragmentTag(childFragmentTag)
		viewModel.fragmentTagStack.push(childFragmentTag)
		viewModel.fragmentTransactionListener.observe(viewLifecycleOwner) {
			if (viewModel.currentFragmentTag.value == childFragmentTag) {
				activity.updateUI(childFragmentTag)
			} else {
				Log.d(FRAGMENT_MANAGER, "Not doing anything from the ${childFragmentTag}, since the top fragment is ${viewModel.currentFragmentTag.value}")
			}
		}
		return root
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (!mPrimaryFragment) {
			enterTransition = TransitionInflater.from(activity).inflateTransition(R.transition.slide_in_right)
			exitTransition = TransitionInflater.from(activity).inflateTransition(R.transition.slide_in_right)
		}
	}

}