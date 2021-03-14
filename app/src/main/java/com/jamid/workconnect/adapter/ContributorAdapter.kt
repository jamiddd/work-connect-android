package com.jamid.workconnect.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.R
import com.jamid.workconnect.StringComparator
import com.jamid.workconnect.databinding.UserItemBinding
import com.jamid.workconnect.interfaces.UserItemClickListener
import com.jamid.workconnect.model.UserMinimal

class ContributorAdapter(val userItemClickListener: UserItemClickListener): ListAdapter<String, ContributorAdapter.ContributorViewHolder>(
    StringComparator()) {

    val db = Firebase.firestore

    inner class ContributorViewHolder(val binding: UserItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(userId: String) {
            db.collection("userMinimals").document(userId).get()
                .addOnFailureListener {
                    Log.e("ContributorAdapter", it.message.toString())
                }.addOnSuccessListener {  doc ->
                    if (doc != null && doc.exists()) {
                        val user = doc.toObject(UserMinimal::class.java)!!
                        binding.userImg.setImageURI(user.photo)
                        binding.userName.text = user.name

                        binding.root.setOnClickListener {
                            userItemClickListener.onUserPressed(user.id)
                        }
                    }
                }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContributorViewHolder {
        return ContributorViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.user_item, parent, false))
    }

    override fun onBindViewHolder(holder: ContributorViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}