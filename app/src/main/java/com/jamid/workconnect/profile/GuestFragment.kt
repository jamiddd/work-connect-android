package com.jamid.workconnect.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.jamid.workconnect.MainViewModel
import com.jamid.workconnect.R
import com.jamid.workconnect.databinding.FragmentGuestBinding

class GuestFragment : Fragment() {

    private lateinit var binding: FragmentGuestBinding
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_guest, container, false)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.authButton.setOnClickListener {
            findNavController().navigate(R.id.signInFragment)
        }

        /*viewModel.user.observe(viewLifecycleOwner) {
            if (it != null) {
                findNavController().navigateUp()
            }
        }*/

    }

    companion object {

        @JvmStatic
        fun newInstance() = GuestFragment()
    }
}