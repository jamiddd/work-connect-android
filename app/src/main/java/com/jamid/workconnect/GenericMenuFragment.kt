package com.jamid.workconnect

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.workconnect.adapter.GenericAdapter
import com.jamid.workconnect.databinding.GenericMenuLayoutBinding
import com.jamid.workconnect.interfaces.GenericMenuClickListener
import com.jamid.workconnect.model.GenericMenuItem

class GenericMenuFragment: Fragment(R.layout.generic_menu_layout) {

    private lateinit var binding: GenericMenuLayoutBinding
    private lateinit var genericMenuAdapter: GenericAdapter<GenericMenuItem>
    private lateinit var activity: MainActivity
    var shouldHideTitle = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as MainActivity
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = GenericMenuLayoutBinding.bind(view)

        val genericMenuClickListener = activity as GenericMenuClickListener
        val currentItem = arguments?.getParcelable<Parcelable>(ARG_EXTRA_ITEM)
        genericMenuAdapter = if (currentItem != null) {
            GenericAdapter(GenericMenuItem::class.java, extras = mapOf("menu" to currentItem))
        } else {
            GenericAdapter(GenericMenuItem::class.java)
        }

        if (Build.VERSION.SDK_INT <= 27) {
            binding.genericMenuRoot.setBackgroundColor(Color.WHITE)
            binding.genericMenuHeading.setTextColor(Color.BLACK)
        }

        if (shouldHideTitle) {
            binding.genericMenuHeading.visibility = View.GONE
        }

        arguments?.apply {

            val tag = getString(ARG_TAG)

            binding.genericMenuItemRecycler.apply {
                adapter = genericMenuAdapter
                layoutManager = LinearLayoutManager(activity)
            }

            val title = getString(ARG_TITLE)
            binding.genericMenuHeading.text = title

            val items = getParcelableArrayList<GenericMenuItem>(ARG_ITEMS)
            items?.let {
                Log.d(TAG, it.toString())
                genericMenuAdapter.submitList(items)
            }

            binding.genericMenuDismiss.setOnClickListener {
                if (tag != null) {
                    genericMenuClickListener.onDismissClick(tag)
                }
            }

        }

        val bottomInset = activity.navigationBarHeight()
        binding.genericMenuContainer.setPadding(0, 0, 0, bottomInset)


    }

    companion object {

        const val TAG = "GenericMenuFragment"
        const val ARG_TITLE = "ARG_TITLE"
        const val ARG_ITEMS = "ARG_ITEMS"
        const val ARG_TAG = "ARG_TAG"
        const val ARG_EXTRA_ITEM = "ARG_EXTRA_ITEM"

        @JvmStatic
        fun newInstance(tag: String, title: String, items: ArrayList<GenericMenuItem>, item: Parcelable? = null) = GenericMenuFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_TAG, tag)
                putString(ARG_TITLE, title)
                putParcelableArrayList(ARG_ITEMS, items)
                putParcelable(ARG_EXTRA_ITEM, item)
            }
        }
    }

}