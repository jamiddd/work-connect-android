package com.jamid.workconnect.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.*
import com.jamid.workconnect.databinding.FragmentFollowingsBinding
import com.jamid.workconnect.databinding.UserHorizontalLayoutBinding
import com.jamid.workconnect.model.User
import com.jamid.workconnect.model.UserMinimal

class FollowingsFragment : BaseBottomSheetFragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var binding: FragmentFollowingsBinding
    private val db = Firebase.firestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_followings, container, false)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val followingsAdapter = FollowingsAdapter()

        val user = arguments?.getParcelable<User>("user")
        if (user != null) {
            val followings = user.followings

            binding.followingsRecycler.apply {
                adapter = followingsAdapter
                layoutManager = LinearLayoutManager(requireContext())
            }

            followingsAdapter.submitList(followings)

        }

        binding.followingsToolbar.setNavigationOnClickListener {
            hideKeyboard()
            findNavController().navigateUp()
        }

    }

    inner class FollowingsAdapter() : ListAdapter<String, FollowingsAdapter.FollowingsViewHolder>(StringComparator()) {

        inner class FollowingsViewHolder(val binding: UserHorizontalLayoutBinding): RecyclerView.ViewHolder(binding.root) {
            fun bind(userId: String) {
                db.collection(USER_MINIMALS).document(userId).get()
                    .addOnSuccessListener {
                        val user = it.toObject(UserMinimal::class.java)!!

                        binding.userHorizName.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.transparent))
                        binding.userHorizName.text = user.name
                        binding.userHorizPhoto.setImageURI(user.photo)
                        binding.userHorizAbout.text = "@" + user.username
                        binding.userHorizAbout.visibility = View.VISIBLE
                        binding.button.visibility = View.GONE

                        val bundle = Bundle().apply {
                            putString("userId", userId)
                        }

                        binding.root.setOnClickListener {
                            findNavController().navigate(R.id.userFragment, bundle)
                        }

                    }.addOnFailureListener {
                        Toast.makeText(
                            requireContext(),
                            "Something went wrong :(",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowingsViewHolder {
            val binding = DataBindingUtil.inflate<UserHorizontalLayoutBinding>(LayoutInflater.from(parent.context), R.layout.user_horizontal_layout, parent, false)
            return FollowingsViewHolder(binding)
        }

        override fun onBindViewHolder(holder: FollowingsViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }

    companion object {

        @JvmStatic
        fun newInstance() = FollowingsFragment()
    }
}