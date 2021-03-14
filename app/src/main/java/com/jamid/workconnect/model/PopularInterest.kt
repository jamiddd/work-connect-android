package com.jamid.workconnect.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PopularInterest(val interest: String): Parcelable {
    constructor(): this("")
}