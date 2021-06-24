package com.jamid.workconnect.interfaces

interface GenericDialogListener {
    fun onDialogPositiveActionClicked(tag: String)
    fun onDialogNegativeActionClicked(tag: String)
}