package com.jamid.workconnect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.workconnect.adapter.ActiveRequestsAdapter
import com.jamid.workconnect.databinding.FragmentRequestBinding
import com.jamid.workconnect.interfaces.RequestItemClickListener
import com.jamid.workconnect.model.Post

class RequestFragment : Fragment(), RequestItemClickListener {

    private lateinit var binding: FragmentRequestBinding
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var activeRequestsAdapter: ActiveRequestsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_request, container, false)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activeRequestsAdapter = ActiveRequestsAdapter(true, this as RequestItemClickListener)

        binding.activeRequestsProgressBar.visibility = View.VISIBLE

        binding.requestsRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = activeRequestsAdapter
        }

        viewModel.user.observe(viewLifecycleOwner) {
            if (it != null) {
                val activeRequestsList = it.activeRequests
                activeRequestsAdapter.submitList(activeRequestsList)

                if (activeRequestsList.isEmpty()) {
                    binding.noRequestsText.visibility = View.VISIBLE
                }
                binding.activeRequestsProgressBar.visibility = View.GONE
                binding.activeRequestsProgressBar.visibility = View.GONE
            }
        }

        binding.activeRequestsSwipeRefresh.setOnRefreshListener {
            binding.activeRequestsSwipeRefresh.isRefreshing = false
        }
    }

    companion object {

        @JvmStatic
        fun newInstance() = RequestFragment()
    }

    override fun onRequestItemClick(post: Post) {
        val bundle = Bundle().apply {
            putParcelable("post", post)
        }
        findNavController().navigate(R.id.projectFragment, bundle)
    }

    override fun onPositiveButtonClick(post: Post) {

    }

    override fun onNegativeButtonClick(post: Post) {

    }

    override fun onDelete(postId: String, pos: Int) {
        activeRequestsAdapter.notifyItemRemoved(pos)
    }

}