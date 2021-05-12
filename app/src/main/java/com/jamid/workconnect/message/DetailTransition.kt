package com.jamid.workconnect.message

import android.view.animation.AccelerateDecelerateInterpolator
import androidx.transition.ChangeBounds
import androidx.transition.ChangeImageTransform
import androidx.transition.ChangeTransform
import androidx.transition.TransitionSet


class DetailTransition(duration: Long, delay: Long) : TransitionSet() {

	init {
		ordering = ORDERING_TOGETHER
		addTransition(ChangeBounds()).addTransition(ChangeTransform())
			.addTransition(ChangeImageTransform()).setDuration(duration).setStartDelay(delay)
			.setInterpolator(AccelerateDecelerateInterpolator())
	}

}