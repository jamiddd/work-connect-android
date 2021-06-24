package com.jamid.workconnect.interfaces

interface OnChipClickListener {
    fun onChipClick(interest: String)
    fun onInterestSelect(interest: String)
    fun onInterestRemoved(interest: String)
}