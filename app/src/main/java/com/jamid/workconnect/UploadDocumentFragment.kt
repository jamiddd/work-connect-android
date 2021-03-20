package com.jamid.workconnect

import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.databinding.UploadDocumentFragmentBinding
import com.jamid.workconnect.model.ChatChannel
import com.jamid.workconnect.model.Result

class UploadDocumentFragment: Fragment(R.layout.upload_document_fragment){

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var binding: UploadDocumentFragmentBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = UploadDocumentFragmentBinding.bind(view)

        val activity = requireActivity() as MainActivity
        val chatChannel = arguments?.getParcelable<ChatChannel>(ARG_CHAT_CHANNEL)!!

        val contentResolver = activity.contentResolver

        var messageRef: DocumentReference? = null

        viewModel.currentDocUri.observe(viewLifecycleOwner) {
            if (it != null) {
                /*val inputStream = contentResolver.openInputStream(it)
                val type = contentResolver.getType(it)*/
                val cursor = contentResolver.query(it, null, null, null, null)

                cursor?.moveToFirst()
                val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor?.getColumnIndex(OpenableColumns.SIZE)

                val name = cursor?.getString(nameIndex ?: 0)
                binding.docName.text = name
                val sizeText = (cursor?.getLong(sizeIndex ?: 0) ?: 0 / 1024 ).toString() + " KB"
                binding.docSize.text = sizeText
                cursor?.close()

                binding.sendDocBtn.setOnClickListener { v ->
                    messageRef = Firebase.firestore.collection(CHAT_CHANNELS).document(chatChannel.chatChannelId).collection(
                        MESSAGES).document()
                    val messageId = messageRef!!.id
                    viewModel.uploadDoc(it, chatChannel.chatChannelId, messageId, name)
                }
            }
        }


        binding.cancelDocUpload.setOnClickListener {
            viewModel.setCurrentDoc(null)
            activity.bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        viewModel.docUploadResult.observe(viewLifecycleOwner) {
            val result = it ?: return@observe

            when (result) {
                is Result.Success -> {
                    val message = result.data
                    viewModel.sendMessage(message, messageRef!!)
                    activity.bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                }
                is Result.Error -> {
                    Toast.makeText(requireContext(), "Something went wrong!", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.setCurrentDoc(null)
    }

    companion object {
        const val TAG = "UploadDocument"
        private const val ARG_CHAT_CHANNEL = "ARG_CHAT_CHANNEL"
        fun newInstance(chatChannel: ChatChannel)
            = UploadDocumentFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_CHAT_CHANNEL, chatChannel)
                }
            }
    }
}