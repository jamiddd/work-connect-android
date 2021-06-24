package com.jamid.workconnect.model

data class InterestItem(val id: String, val interest: String, val searchRank: Int, val indices: List<String>) {
    constructor(): this("", "", 0, emptyList())
}