package com.jamid.workconnect.model

import com.jamid.workconnect.END_OF_CAT
import com.jamid.workconnect.TEXT
import java.util.*

class BlogItemConverter(item: String) {

	private val opString = item
	var type: String = TEXT

	// for extra purposes
	var decodedItemList: List<String> = emptyList()
	private val _decodedItemList: MutableList<String> = mutableListOf()
	var tempString = ""
	var content = ""
	var spanSize = 0

	var spans: List<SpanItem> = emptyList()
	private val _spans: MutableList<SpanItem> = mutableListOf()

	private val stack = Stack<Char>()

	init {

		val parts = opString.split(END_OF_CAT)
		val categories = parts[0]
		val cont = parts[1]

		for (ch in categories) {
			when (ch) {
				'[' -> {
					stack.push(ch)
				}
				']' -> {
					stack.pop()
					_decodedItemList.add(tempString)
					tempString = ""
				}
				else -> {
					tempString += ch
				}
			}
		}

		for (i in _decodedItemList.indices) {
			when (i) {
				0 -> {
					type = _decodedItemList[i]
				}
				1 -> {
					spanSize = _decodedItemList[i].toInt()
				}
				else -> {
					val spanDetail = _decodedItemList[i].split(',')
					val spanItem = SpanItem(spanDetail[0], spanDetail[1].toInt(), spanDetail[2].toInt())
					_spans.add(spanItem)
				}
			}
		}

		decodedItemList = _decodedItemList
		spans = _spans
		content = cont

	}

}