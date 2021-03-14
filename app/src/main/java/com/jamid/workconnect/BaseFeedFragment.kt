package com.jamid.workconnect

import android.content.Context
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.adapter.PostAdapter
import com.jamid.workconnect.model.Result

abstract class BaseFeedFragment(@LayoutRes layout: Int) : Fragment(layout) {

    val auth = Firebase.auth
    val viewModel: MainViewModel by activityViewModels()
    var mProgressBar: ProgressBar? = null
    var mRecyclerView: RecyclerView? = null
    var mTextView: TextView? = null
    lateinit var activity: MainActivity

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = requireActivity() as MainActivity
    }

    fun setProgressBar(progressBar: ProgressBar?) {
        mProgressBar = progressBar
    }

    fun setDeleteListeners() {
        viewModel.deletePostResult.observe(viewLifecycleOwner) {
            val result = it ?: return@observe

            when (result) {
                is Result.Success -> {
                    Toast.makeText(
                        requireContext(),
                        "Deleted post successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                    (mRecyclerView?.adapter as PostAdapter).refresh()
                }
                is Result.Error -> {
                    viewModel.setCurrentError(result.exception)
                }
            }
        }
    }

    fun setRecyclerView(recyclerView: RecyclerView) {
        mRecyclerView = recyclerView
    }

    fun setEmptyFeedText(textView: TextView?) {
        mTextView = textView
    }


}