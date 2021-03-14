package com.jamid.workconnect.message

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.jamid.workconnect.R

class MediaLinksFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_media_links, container, false)
    }

    companion object {

        @JvmStatic
        fun newInstance() = MediaLinksFragment()
    }
}