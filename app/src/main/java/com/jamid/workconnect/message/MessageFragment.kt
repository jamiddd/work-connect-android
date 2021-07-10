package com.jamid.workconnect.message

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.jamid.workconnect.R
import com.jamid.workconnect.SupportFragment
import com.jamid.workconnect.databinding.FragmentMessageBinding

class MessageFragment : SupportFragment(R.layout.fragment_message) {

    private lateinit var binding: FragmentMessageBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMessageBinding.bind(view)

        binding.messagePager.adapter = MessagePager(activity)

        (binding.messagePager.getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER

    }

    class MessagePager(fa: FragmentActivity): FragmentStateAdapter(fa) {
        override fun getItemCount() = 1

        override fun createFragment(position: Int): Fragment {
            return ChatChannelFragment.newInstance()
        }
    }

    companion object {

        const val TITLE = "Messages"

        const val TAG = "MessageFragment"

        @JvmStatic
        fun newInstance() = MessageFragment()
    }
}