package com.jamid.workconnect

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Typeface
import android.text.Spannable
import android.text.style.CharacterStyle
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.ScrollView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setMargins
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import me.everything.android.ui.overscroll.IOverScrollState
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

const val START_TO_START = ""
const val START_TO_END = ""
const val END_TO_END = ""
const val END_TO_START = ""
const val TOP_TO_TOP = ""
const val TOP_TO_BOTTOM = ""
const val BOTTOM_TO_BOTTOM = ""
const val BOTTOM_TO_TOP = ""

fun getWindowHeight() = Resources.getSystem().displayMetrics.heightPixels

fun getWindowWidth() = Resources.getSystem().displayMetrics.widthPixels

fun AppBarLayout.hide() {
    val animator = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, -convertDpToPx(this.measuredHeight, this.context).toFloat())
    animator.duration = 300
    animator.interpolator = AccelerateDecelerateInterpolator()
    animator.start()
}


fun CardView.hide() {
    val animator = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, convertDpToPx(150, this.context).toFloat())
    animator.duration = 300
    animator.interpolator = AccelerateDecelerateInterpolator()
    animator.start()
}

fun CardView.show() {
    val animator = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, 0f)
    animator.duration = 300
    animator.interpolator = AccelerateDecelerateInterpolator()
    animator.start()
}

fun Context.statusBarHeight() : Int {
    // status bar height
    val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
    return if (resourceId > 0) {
        resources.getDimensionPixelSize(resourceId)
    } else {
        0
    }
}

fun Context.navigationBarHeight(): Int {
    val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
    return if (resourceId > 0) {
        resources.getDimensionPixelSize(resourceId)
    } else {
        0
    }
}

fun convertDpToPx(dp: Int, context: Context) = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    dp.toFloat(),
    context.resources.displayMetrics
).toInt()

fun Fragment.convertDpToPx(dp: Int) = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    dp.toFloat(),
    this.requireContext().resources.displayMetrics
).toInt()

fun Fragment.convertDpToPx(dp: Float) = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    dp,
    this.requireContext().resources.displayMetrics
)

fun Context.convertDpToPx(dp: Int) = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    dp.toFloat(),
    resources.displayMetrics
).toInt()

fun Fragment.showKeyboard() {
    view?.let { activity?.showKeyboard(it) }
}

private fun Context.showKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
}

fun Fragment.hideKeyboard() {
    view?.let { activity?.hideKeyboard(it) }
}

/**
 * Extension function to hide the keyboard from the given context
 *
 * @param view
 */
fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun setSpanRelatively(spannable: Spannable, start: Int, end: Int, newSpan: Any) {

    val listOfSpans = mutableListOf<CharacterStyle>()

    if (newSpan::class == StyleSpan::class) {
        val span = newSpan as StyleSpan
        if (span.style == Typeface.BOLD) {
            val list = spannable.getSpans(start, end, StyleSpan::class.java)
            val newList = list.filter {
                it.style == Typeface.BOLD
            }

            listOfSpans.addAll(newList)
        } else if (span.style  == Typeface.ITALIC) {
            val list = spannable.getSpans(start, end, StyleSpan::class.java)
            val newList = list.filter {
                it.style == Typeface.ITALIC
            }

            listOfSpans.addAll(newList)
        }
    }

    if (newSpan::class == UnderlineSpan::class) {
        val span = newSpan as UnderlineSpan

        val list = spannable.getSpans(start, end, UnderlineSpan::class.java)

        listOfSpans.addAll(list)
    }

    if (newSpan::class == StrikethroughSpan::class) {

        val span = newSpan as StrikethroughSpan

        val list = spannable.getSpans(start, end, StrikethroughSpan::class.java)

        listOfSpans.addAll(list)
    }


    if (listOfSpans.isEmpty()) {
        spannable.setSpan(newSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return

    }

    for (span in listOfSpans) {
        val prevStart = spannable.getSpanStart(span)
        val prevEnd = spannable.getSpanEnd(span)
        relativeSpanAdjustments(spannable, span, prevStart, prevEnd, start, end, newSpan)
    }
}

private fun relativeSpanAdjustments(spannable: Spannable, prevSpan: Any, prevStart: Int, prevEnd: Int, newStart: Int, newEnd: Int, newSpan: Any) {

    // remove old span
    spannable.removeSpan(prevSpan)

    if (prevStart < newStart && newEnd == prevEnd) {
        // new =>       <-------------->
        // old =>  <------------------->
        spannable.setSpan(newSpan, prevStart, newStart, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

    } else if (prevStart < newStart && prevEnd < newEnd) {
        // new =>       <-------------->
        // old =>  <--------------->

        spannable.setSpan(newSpan, prevStart, newStart, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(newSpan, prevEnd, newEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    } else if (prevStart < newStart && prevEnd > newEnd) {
        // new =>       <-------------->
        // old =>    <-------------------->

        spannable.setSpan(newSpan, prevStart, newStart, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(newSpan, newEnd, newEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    } else if (prevStart == newStart && prevEnd == newEnd) {
        // new =>       <-------------->
        // old =>       <-------------->

    } else if (prevStart == newStart && prevEnd > newEnd) {
        // new =>       <-------------->
        // old =>       <----------------->

        spannable.setSpan(newSpan, newEnd, prevEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    } else if (prevStart > newStart && prevEnd < newEnd) {
        // new =>       <-------------->
        // old =>           <------>

        spannable.setSpan(newSpan, newStart, prevStart, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(newSpan, prevEnd, newEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    } else if (prevStart > newStart && prevEnd == newEnd) {
        // new =>       <-------------->
        // old =>           <---------->

        spannable.setSpan(newSpan, newStart, prevStart, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    } else if (prevStart > newStart && prevEnd > newEnd) {
        // new =>       <-------------->
        // old =>           <--------------->

        spannable.setSpan(newSpan, newStart, prevStart, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(newSpan, newEnd, prevEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}

fun ViewGroup.updateLayout(
        height: Int? = null,
        width: Int? = null,
        margin: Int? = null,
        marginLeft: Int? = null,
        marginTop: Int? = null,
        marginRight: Int? = null,
        marginBottom: Int? = null,
        padding: Int? = null,
        paddingLeft: Int? = null,
        paddingTop: Int? = null,
        paddingRight: Int? = null,
        paddingBottom: Int? = null,
        ignoreParams: Boolean? = true,
        ignoreMargin: Boolean? = true,
        ignorePadding: Boolean? = true,
        extras: Map<String, Int>? = null) {

    var ilp = ignoreParams
    var im = ignoreMargin
    var ip = ignorePadding

    if (width != null || height != null) {
        ilp = false
    }

    if (margin != null || marginLeft != null || marginTop != null || marginRight != null || marginBottom != null) {
        im = false
    }

    if (padding != null || paddingLeft != null || paddingTop != null || paddingRight != null || paddingBottom != null) {
        ip = false
    }

    if (ilp != null && !ilp) {
        val params = if (extras != null) {
            val p1 = this.layoutParams as ConstraintLayout.LayoutParams
            p1.height = height ?: ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
            p1.width = width ?: ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
            val defaultId = (this.parent as ConstraintLayout).id
            p1.apply {
                if (extras.containsKey(START_TO_START)) {
                    startToStart = extras[START_TO_START] ?: defaultId
                }
                if (extras.containsKey(END_TO_END)) {
                    endToEnd = extras[END_TO_END] ?: defaultId
                }
                if (extras.containsKey(TOP_TO_TOP)) {
                    topToTop = extras[TOP_TO_TOP] ?: defaultId
                }
                if (extras.containsKey(BOTTOM_TO_BOTTOM)) {
                    bottomToBottom = extras[BOTTOM_TO_BOTTOM] ?: defaultId
                }
                if (extras.containsKey(START_TO_END)) {
                    startToEnd = extras[START_TO_END]!!
                }
                if (extras.containsKey(END_TO_START)) {
                    endToStart = extras[END_TO_START]!!
                }
                if (extras.containsKey(TOP_TO_BOTTOM)) {
                    topToBottom = extras[TOP_TO_BOTTOM]!!
                }
                if (extras.containsKey(BOTTOM_TO_TOP)) {
                    bottomToTop = extras[BOTTOM_TO_TOP]!!
                }
            }
            p1
        } else {
            val p1 = this.layoutParams as ViewGroup.LayoutParams
            p1.height = height ?: ViewGroup.LayoutParams.WRAP_CONTENT
            p1.width = width ?: ViewGroup.LayoutParams.MATCH_PARENT
            p1
        }

        this.layoutParams = params
    }

    if (im != null && !im) {
        val marginParams = this.layoutParams as ViewGroup.MarginLayoutParams
        if (margin != null) {
            marginParams.setMargins(margin)
        } else {
            marginParams.setMargins(marginLeft ?: 0, marginTop ?: 0, marginRight ?: 0, marginBottom ?: 0)
        }
        this.requestLayout()
    }

    if (ip != null && !ip) {
        if (padding != null) {
            this.setPadding(padding)
        } else {
            this.setPadding(paddingLeft ?: 0, paddingTop ?: 0, paddingRight ?: 0, paddingBottom ?: 0)
        }
    }
}

fun View.updateLayout(
    height: Int? = null,
    width: Int? = null,
    margin: Int? = null,
    marginLeft: Int? = null,
    marginTop: Int? = null,
    marginRight: Int? = null,
    marginBottom: Int? = null,
    padding: Int? = null,
    paddingLeft: Int? = null,
    paddingTop: Int? = null,
    paddingRight: Int? = null,
    paddingBottom: Int? = null,
    ignoreParams: Boolean? = true,
    ignoreMargin: Boolean? = true,
    ignorePadding: Boolean? = true,
    extras: Map<String, Int>? = null) {

    var ilp = ignoreParams
    var im = ignoreMargin
    var ip = ignorePadding

    if (width != null || height != null) {
        ilp = false
    }

    if (margin != null || marginLeft != null || marginTop != null || marginRight != null || marginBottom != null) {
        im = false
    }

    if (padding != null || paddingLeft != null || paddingTop != null || paddingRight != null || paddingBottom != null) {
        ip = false
    }

    if (ilp != null && !ilp) {
        val params = if (extras != null) {
            val p1 = this.layoutParams as ConstraintLayout.LayoutParams
            p1.height = height ?: ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
            p1.width = width ?: ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
            val defaultId = (this.parent as ConstraintLayout).id
            p1.apply {
                startToStart = extras[START_TO_START] ?: defaultId
                endToEnd = extras[END_TO_END] ?: defaultId
                topToTop = extras[TOP_TO_TOP] ?: defaultId
                bottomToBottom = extras[BOTTOM_TO_BOTTOM] ?: defaultId
                if (extras.containsKey(START_TO_END)) {
                    startToEnd = extras[START_TO_END]!!
                }
                if (extras.containsKey(END_TO_START)) {
                    endToStart = extras[END_TO_START]!!
                }
                if (extras.containsKey(TOP_TO_BOTTOM)) {
                    topToBottom = extras[TOP_TO_BOTTOM]!!
                }
                if (extras.containsKey(BOTTOM_TO_TOP)) {
                    bottomToTop = extras[BOTTOM_TO_TOP]!!
                }
            }
            p1
        } else {
            val p1 = this.layoutParams as ViewGroup.LayoutParams
            p1.height = height ?: ViewGroup.LayoutParams.WRAP_CONTENT
            p1.width = width ?: ViewGroup.LayoutParams.MATCH_PARENT
            p1
        }

        this.layoutParams = params
    }

    if (im != null && !im) {
        val marginParams = this.layoutParams as ViewGroup.MarginLayoutParams
        if (margin != null) {
            marginParams.setMargins(margin)
        } else {
            marginParams.setMargins(marginLeft ?: 0, marginTop ?: 0, marginRight ?: 0, marginBottom ?: 0)
        }
        this.requestLayout()
    }

    if (ip != null && !ip) {
        if (padding != null) {
            this.setPadding(padding)
        } else {
            this.setPadding(paddingLeft ?: 0, paddingTop ?: 0, paddingRight ?: 0, paddingBottom ?: 0)
        }
    }
}

fun View.attachOverScrollWithProgressListener(progress: ProgressBar) {
    var isProgressing = false
    val iDecor = when (this) {
        is RecyclerView -> OverScrollDecoratorHelper.setUpOverScroll(this, OverScrollDecoratorHelper.ORIENTATION_VERTICAL)
        is ScrollView -> OverScrollDecoratorHelper.setUpOverScroll(this)
        else -> OverScrollDecoratorHelper.setUpStaticOverScroll(this, OverScrollDecoratorHelper.ORIENTATION_VERTICAL)
    }

    val finalOffset = convertDpToPx(40, this.context)

    iDecor.setOverScrollUpdateListener { _, state, offset ->
        val progressOffset: Float = offset * 1.2f
        val percentage = (100 * progressOffset)/ finalOffset

        if (progressOffset < 10f) {
            progress.visibility = View.GONE
        } else if (progressOffset < finalOffset && !isProgressing) {
            progress.visibility = View.VISIBLE
            progress.isIndeterminate = false
            progress.progress = percentage.toInt()
            progress.translationY = progressOffset

            if (state == IOverScrollState.STATE_DRAG_START_SIDE) {
                progress.visibility = View.VISIBLE
            }
        } else {
            isProgressing = true
            this.translationY = finalOffset * 2f
            progress.isIndeterminate = true
            this.overScrollMode = View.OVER_SCROLL_NEVER
            iDecor.detach()

//            refreshListener?.onRefreshStart()
        }
    }

    isProgressing = false
    progress.isIndeterminate = false
    progress.visibility = View.GONE
    progress.translationY = 0f

    if (this.translationY != 0f) {
        val animator = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, 0f)
        animator.start()
    }

}