package com.jamid.workconnect.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.navigation.NavController
import androidx.navigation.NavHost

class CustomNavHostFragment @JvmOverloads constructor(
    context: Context, attributeSet: AttributeSet? = null, defStyleAttr: Int = 0,
    override val navController: NavController
) : FrameLayout(context, attributeSet, defStyleAttr), NavHost {
    /*private val mNavController = NavController(context)

    init {
        Navigation.setViewNavController(this, mNavController)
        val customViewNavigator = CustomNavigator(this)
        mNavController.navigatorProvider.addNavigator(customViewNavigator)
        context.withStyledAttributes(attributeSet, R.styleable.CustomNavHostFragment, 0, 0) {
            val graphId = getResourceId(R.styleable.CustomNavHostFragment_navGraph, 0)
            mNavController.setGraph(graphId)
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        return Bundle().apply {
            putParcelable(KEY_VIEW_STATE, super.onSaveInstanceState())
            putParcelable(KET_NAV_CONTROLLER_STATE, mNavController.saveState())
        }
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle && state.containsKey(KEY_VIEW_STATE)) {
            super.onRestoreInstanceState(state.getParcelable("viewState"))
            mNavController.restoreState(state.getParcelable(KET_NAV_CONTROLLER_STATE))
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    companion object {
        const val KEY_VIEW_STATE = "viewState"
        const val KET_NAV_CONTROLLER_STATE = "navControllerState"
    }*/


}