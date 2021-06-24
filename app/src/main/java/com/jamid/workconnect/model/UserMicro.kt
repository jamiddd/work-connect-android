package com.jamid.workconnect.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserMicro(val name: String, val username: String, val photo: String? = null): Parcelable {
    constructor(): this("", "")
}
