package com.jamid.workconnect.message

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.jamid.workconnect.MainViewModel
import com.jamid.workconnect.R
import com.jamid.workconnect.databinding.FragmentMessageBinding

class MessageFragment : Fragment(R.layout.fragment_message) {

    private lateinit var binding: FragmentMessageBinding
    private val viewModel: MainViewModel by activityViewModels()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMessageBinding.bind(view)
        binding.messagePager.adapter = MessagePager(requireActivity())

        (binding.messagePager.getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
    }

    class MessagePager(fa: FragmentActivity): FragmentStateAdapter(fa) {
        override fun getItemCount() = 1

        override fun createFragment(position: Int): Fragment {
            return ChatChannelFragment.newInstance()
        }
    }

    companion object {

        @JvmStatic
        fun newInstance() = MessageFragment()
    }
}