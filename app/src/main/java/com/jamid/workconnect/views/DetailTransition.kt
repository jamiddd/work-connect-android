package com.jamid.workconnect.views

import android.view.animation.AnticipateOvershootInterpolator
import androidx.transition.ChangeBounds
import androidx.transition.ChangeImageTransform
import androidx.transition.ChangeTransform
import androidx.transition.TransitionSet


class DetailTransition(durationT: Long, delayT: Long) : TransitionSet() {

    init {
        ordering = ORDERING_TOGETHER
        addTransition(ChangeBounds()).addTransition(ChangeTransform())
            .addTransition(ChangeImageTransform()).setDuration(durationT).setStartDelay(delayT)
            .setInterpolator(AnticipateOvershootInterpolator())
    }

}