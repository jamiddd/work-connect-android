package com.jamid.workconnect.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SimpleMedia(val id: String, val type: String, val mediaLocation: String, val createdAt: Long) :
    Parcelable { constructor(): this("", "", "", 0)}