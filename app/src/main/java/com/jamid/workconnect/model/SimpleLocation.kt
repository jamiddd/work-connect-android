package com.jamid.workconnect.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SimpleLocation(
    var latitude: Double,
    var longitude: Double,
    var place: String
): Parcelable {
    constructor(): this(0.0, 0.0, "")
}