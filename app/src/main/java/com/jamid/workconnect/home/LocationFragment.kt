package com.jamid.workconnect.home

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jamid.workconnect.*
import com.jamid.workconnect.databinding.FragmentLocationBinding
import com.jamid.workconnect.databinding.GenericMenuItemBinding
import com.jamid.workconnect.databinding.LocationListItemBinding

class LocationFragment : Fragment() {

    private var _binding: FragmentLocationBinding? = null
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var activity: MainActivity

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as MainActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("VisibleForTests")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.addressList.observe(viewLifecycleOwner){ list ->
            if (list != null) {
                binding.locationProgressBar.visibility = View.GONE
                binding.locationList.layoutManager = LinearLayoutManager(context)
                binding.locationList.adapter = LocationItemAdapter(list)
            }
        }

        binding.locationProgressBar.visibility = View.VISIBLE

        binding.cancelAddLocation.setOnClickListener {
            activity.bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        binding.addCustomLocation.setOnClickListener {
            val customLocation = binding.customLocation.text
            if (customLocation.isNotBlank()) {
                viewModel.setCurrentPlace(customLocation.toString())
            }
            activity.bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        binding.root.updateLayout(getWindowHeight())
    }


    private inner class ViewHolder(binding: GenericMenuItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val text: TextView = binding.menuItem
    }

    private inner class LocationItemAdapter(private val places: List<String>) : RecyclerView.Adapter<ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

            return ViewHolder(
                GenericMenuItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.text.text = places[position]
            holder.text.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_location_on_24, 0)
            holder.text.setOnClickListener {
                viewModel.setCurrentPlace(places[position])
                activity.bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
        }

        override fun getItemCount(): Int {
            return places.size
        }
    }

    companion object {

        const val REQUEST_FINE_LOCATION = 12
        const val TAG = "LocationFragment"
        private const val ARG_ITEM_COUNT = "ARG_ITEM_COUNT"

        fun newInstance() = LocationFragment()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}