package com.jamid.workconnect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.paging.Config
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.adapter.NotificationAdapter
import com.jamid.workconnect.databinding.FragmentGeneralNotificationBinding
import com.jamid.workconnect.interfaces.GenericLoadingStateListener
import com.jamid.workconnect.interfaces.NotificationClickListener
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.SimpleNotification

class GeneralNotificationFragment : Fragment(), GenericLoadingStateListener, NotificationClickListener {

    private lateinit var binding: FragmentGeneralNotificationBinding
    private lateinit var notificationAdapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_general_notification, container, false)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val auth = Firebase.auth
        val db = Firebase.firestore

        if (auth.currentUser != null) {
            val query = db
                .collection("users")
                .document(auth.currentUser!!.uid)
                .collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)

            val config = Config(10, 5, false)

            val options = FirestorePagingOptions.Builder<SimpleNotification>()
                .setLifecycleOwner(viewLifecycleOwner)
                .setQuery(query, config, SimpleNotification::class.java)
                .build()

            notificationAdapter = NotificationAdapter(options, this as GenericLoadingStateListener, this as NotificationClickListener)

            binding.notificationsRecycler.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = notificationAdapter
            }

            binding.notificationsRefresher.setOnRefreshListener {
                notificationAdapter.refresh()
                binding.notificationsRefresher.isRefreshing = false
            }
        } else {
            binding.noNotificationsText.visibility = View.VISIBLE

            binding.notificationsRefresher.setOnRefreshListener {
                binding.notificationsRefresher.isRefreshing = false
            }
        }

    }

    companion object {

        @JvmStatic
        fun newInstance() = GeneralNotificationFragment()
    }

    override fun onInitial() {
        binding.notificationProgressBar.visibility = View.VISIBLE
    }

    override fun onLoadingMore() {

    }

    override fun onLoaded() {
        binding.notificationProgressBar.visibility = View.GONE
        if (notificationAdapter.itemCount == 0) {
            binding.noNotificationsText.visibility = View.VISIBLE
        } else {
            binding.noNotificationsText.visibility = View.GONE
        }
    }

    override fun onFinished() {
        binding.notificationProgressBar.visibility = View.GONE
        if (notificationAdapter.itemCount == 0) {
            binding.noNotificationsText.visibility = View.VISIBLE
        } else {
            binding.noNotificationsText.visibility = View.GONE
        }
    }

    override fun onError() {
        Toast.makeText(requireContext(), "Something went wrong", Toast.LENGTH_SHORT).show()
    }

    override fun onItemClick(post: Post) {
        val bundle = Bundle().apply {
            putParcelable("post", post)
        }
        findNavController().navigate(R.id.projectFragment, bundle)
    }
}