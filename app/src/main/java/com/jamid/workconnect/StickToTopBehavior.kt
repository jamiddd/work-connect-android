package com.jamid.workconnect

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.tabs.TabLayout

// pending
class StickToTopBehavior(context: Context, attributeSet: AttributeSet): CoordinatorLayout.Behavior<View>(context, attributeSet) {
    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: View,
        dependency: View
    ): Boolean {
        return dependency is TabLayout
    }

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: View,
        dependency: View
    ): Boolean {

        return super.onDependentViewChanged(parent, child, dependency)
    }
}