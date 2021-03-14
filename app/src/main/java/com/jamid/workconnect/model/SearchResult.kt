package com.jamid.workconnect.model

data class SearchResult(val id: String, val substring: List<String>, val rank: Long,  val title: String, val img: String? = null, val type: String? = null) {
    constructor(): this("", emptyList(), 0, "", null, null)
}