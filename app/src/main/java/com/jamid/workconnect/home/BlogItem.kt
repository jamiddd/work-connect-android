package com.jamid.workconnect.home

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BlogItem(
    var content: String?,
    var type: String,
    var id: Long = System.currentTimeMillis(),
    var hint: String? = null,
    var spans: ArrayList<String> = arrayListOf(),
    var spanRangesStart: ArrayList<Int> = arrayListOf(),
    var spanRangesEnd: ArrayList<Int> = arrayListOf()
): Parcelable {
    constructor(): this("", "Paragraph")
}