package com.jamid.workconnect.home

import android.annotation.SuppressLint
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
import com.jamid.workconnect.MainActivity
import com.jamid.workconnect.MainViewModel
import com.jamid.workconnect.databinding.FragmentLocationBinding
import com.jamid.workconnect.databinding.LocationListItemBinding
import com.jamid.workconnect.getWindowHeight

class LocationFragment : Fragment() {

    private var _binding: FragmentLocationBinding? = null
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var activity: MainActivity

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

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

        activity = requireActivity() as MainActivity

        viewModel.addressList.observe(viewLifecycleOwner, { list ->
            if (list != null) {
                binding.locationProgressBar.visibility = View.GONE
                binding.locationList.layoutManager = LinearLayoutManager(context)
                binding.locationList.adapter = LocationItemAdapter(list)
            }
        })

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

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            val windowHeight = getWindowHeight()
//            val rect = windowHeight - top

            val params = binding.root.layoutParams as ViewGroup.LayoutParams
            params.height = windowHeight
            params.width = ViewGroup.LayoutParams.MATCH_PARENT

            binding.root.layoutParams = params
        }
    }


    private inner class ViewHolder(binding: LocationListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val text: TextView = binding.text
    }

    private inner class LocationItemAdapter(private val places: List<String>) : RecyclerView.Adapter<ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

            return ViewHolder(
                LocationListItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.text.text = places[position]
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