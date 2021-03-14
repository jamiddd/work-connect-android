package com.jamid.workconnect

import android.animation.AnimatorSet
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
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

fun getWindowHeight() = Resources.getSystem().displayMetrics.heightPixels

fun getWindowWidth() = Resources.getSystem().displayMetrics.widthPixels

fun AppBarLayout.hide() {
    val animator = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, -convertDpToPx(this.measuredHeight, this.context).toFloat())
    animator.duration = 300
    animator.interpolator = AccelerateDecelerateInterpolator()
    animator.start()
}

fun AppBarLayout.show() {
    val animator = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, 0f)
    val toolbar = this.findViewById<MaterialToolbar>(R.id.primaryToolbar)
    toolbar.alpha = 1f
    animator.duration = 300
    animator.interpolator = AccelerateDecelerateInterpolator()
    animator.start()
}

fun BottomNavigationView.hide(view: View) {
    val animator = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, convertDpToPx(150, this.context).toFloat())
    val animator1 = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, convertDpToPx(150, this.context).toFloat())

    animator.duration = 300
    animator1.duration = 300
    animator.interpolator = AccelerateDecelerateInterpolator()
    animator1.interpolator = AccelerateDecelerateInterpolator()

    AnimatorSet().apply {
        playTogether(animator, animator1)
        start()
    }
}

fun BottomNavigationView.show(view: View) {
    val animator = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, convertDpToPx(0, this.context).toFloat())
    val animator1 = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, convertDpToPx(0, this.context).toFloat())

    animator.duration = 300
    animator1.duration = 300
    animator.interpolator = AccelerateDecelerateInterpolator()
    animator1.interpolator = AccelerateDecelerateInterpolator()

    AnimatorSet().apply {
        playTogether(animator, animator1)
        start()
    }
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