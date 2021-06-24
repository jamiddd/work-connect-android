package com.jamid.workconnect.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class GenericMenuItem(val menuTag: String, val item: String, val icon: Int, val id: Int): Parcelable
