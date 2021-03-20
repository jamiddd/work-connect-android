package com.jamid.workconnect.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.*
import com.jamid.workconnect.databinding.FragmentFollowersBinding
import com.jamid.workconnect.databinding.UserHorizontalLayoutBinding
import com.jamid.workconnect.model.User
import com.jamid.workconnect.model.UserMinimal

class FollowersFragment : Fragment(R.layout.fragment_followers) {

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var binding: FragmentFollowersBinding
    private val db = Firebase.firestore

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentFollowersBinding.bind(view)
        val activity = requireActivity() as MainActivity

        val followersAdapter = FollowersAdapter()
        val user = arguments?.getParcelable<User>("user")
        if (user != null) {
            val followers = user.followers

            binding.followersRecycler.apply {
                adapter = followersAdapter
                layoutManager = LinearLayoutManager(requireContext())
            }

            followersAdapter.submitList(followers)

        }

        binding.followersToolbar.setNavigationOnClickListener {
            hideKeyboard()
            activity.bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            activity.bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        val height = getWindowHeight()
        val params = binding.root.layoutParams as ViewGroup.LayoutParams
        params.height = height
        params.width = ViewGroup.LayoutParams.MATCH_PARENT
        binding.root.layoutParams = params

        /*viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->

        }*/

    }

    inner class FollowersAdapter() : ListAdapter<String, FollowersAdapter.FollowersViewHolder>(
        StringComparator()
    ) {

        inner class FollowersViewHolder(val binding: UserHorizontalLayoutBinding): RecyclerView.ViewHolder(binding.root) {
            fun bind(userId: String) {
                db.collection(USER_MINIMALS).document(userId).get()
                    .addOnSuccessListener {
                        val user = it.toObject(UserMinimal::class.java)!!

                        binding.userHorizName.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.transparent))
                        binding.userHorizName.text = user.name
                        binding.userHorizPhoto.setImageURI(user.photo)
                        binding.userHorizAbout.text = "@" + user.username
                        binding.userHorizAbout.visibility = View.VISIBLE

                        val bundle = Bundle().apply {
                            putString("userId", userId)
                        }

                        binding.root.setOnClickListener {
                            findNavController().navigate(R.id.userFragment, bundle)
                        }

//                        binding.button.visibility = View.GONE
                    }.addOnFailureListener {
                        Toast.makeText(
                            requireContext(),
                            "Something went wrong :(",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowersViewHolder {
            val binding = DataBindingUtil.inflate<UserHorizontalLayoutBinding>(LayoutInflater.from(parent.context), R.layout.user_horizontal_layout, parent, false)
            return FollowersViewHolder(binding)
        }

        override fun onBindViewHolder(holder: FollowersViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }

    companion object {

        @JvmStatic
        fun newInstance(user: User) = FollowersFragment().apply {
            arguments = Bundle().apply {
                putParcelable("user", user)
            }
        }
    }
}