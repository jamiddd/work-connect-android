package com.jamid.workconnect

import android.content.Context
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.jamid.workconnect.interfaces.OnRefreshListener

abstract class InsetControlFragment(@LayoutRes layout: Int) : Fragment(layout) {

	val INSET_TOP = "INSET_TOP"
	val INSET_BOTTOM = "INSET_BOTTOM"
	val PROGRESS_OFFSET = "PROGRESS_OFFSET"
	lateinit var activity: MainActivity
	var insetBasedParent: ViewGroup? = null
	val viewModel: MainViewModel by activityViewModels()
	var isProgressing = false
	var finalOffset = 0f
	var speed = 0.7f
	private var refreshListener: OnRefreshListener? = null

	override fun onAttach(context: Context) {
		super.onAttach(context)
		activity = context as MainActivity
	}

	fun setRefreshListener(ref: OnRefreshListener) {
		refreshListener = ref
	}

	fun setInsetView(parent: ViewGroup, extras: Map<String, Int>? = null) {
		insetBasedParent = parent

		val statusBarHeight = activity.statusBarHeight()
		val navigationBarHeight = activity.navigationBarHeight()
		val baseOffset = convertDpToPx(40f)

		if (extras != null) {
			val insetTop = convertDpToPx(extras[INSET_TOP] ?: 0)
			val insetBottom = convertDpToPx(extras[INSET_BOTTOM] ?: 0) + navigationBarHeight
			finalOffset = convertDpToPx(extras[PROGRESS_OFFSET] ?: 0).toFloat()
			speed = (speed / baseOffset) * finalOffset

			insetBasedParent?.setPadding(0, insetTop, 0, insetBottom)

		}

	}

	/*fun setOverScrollView(recyclerView: RecyclerView): ProgressBar {
		val progressBar = activity.mainBinding.primaryListProgress
		val iDecor = OverScrollDecoratorHelper.setUpOverScroll(recyclerView, OverScrollDecoratorHelper.ORIENTATION_VERTICAL)
		iDecor.setOverScrollUpdateListener { _, state, offset ->

			val progressOffset: Float = offset * speed
			val percentage = (100 * progressOffset)/finalOffset

			Log.d(BUG_TAG, progressOffset.toString())

			if (progressOffset < 10f) {
				progressBar.visibility = View.GONE
			} else if (progressOffset < finalOffset && !isProgressing) {
				progressBar.visibility = View.VISIBLE
				progressBar.isIndeterminate = false
				progressBar.progress = percentage.toInt()
				progressBar.translationY = progressOffset

				if (state == IOverScrollState.STATE_DRAG_START_SIDE) {
					progressBar.visibility = View.VISIBLE
				}
			} else {
				isProgressing = true
				recyclerView.translationY = finalOffset * 2f
				progressBar.isIndeterminate = true
				recyclerView.overScrollMode = View.OVER_SCROLL_NEVER
				iDecor.detach()

				refreshListener?.onRefreshStart()
			}
		}

		isProgressing = false
		progressBar.isIndeterminate = false
		progressBar.visibility = View.GONE
		progressBar.translationY = 0f

		if (recyclerView.translationY != 0f) {
			val animator = ObjectAnimator.ofFloat(recyclerView, View.TRANSLATION_Y, 0f)
			animator.start()
		}

		return progressBar
	}*/
}