package com.jamid.workconnect.interfaces

import android.os.Bundle
import com.jamid.workconnect.model.GenericMenuItem

interface GenericMenuClickListener {
    fun onDismissClick(menuTag: String)
    fun onMenuItemClick(item: GenericMenuItem, bundle: Bundle?)
}