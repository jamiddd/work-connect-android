package com.jamid.workconnect

import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jamid.workconnect.databinding.UploadDocumentFragmentBinding
import com.jamid.workconnect.model.ChatChannel
import com.jamid.workconnect.model.MediaMetaData
import com.jamid.workconnect.model.SimpleMessage
import java.io.File
import java.io.FileOutputStream

class UploadDocumentFragment: Fragment(R.layout.upload_document_fragment){

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var binding: UploadDocumentFragmentBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = UploadDocumentFragmentBinding.bind(view)

        val activity = requireActivity() as MainActivity
        val chatChannel = arguments?.getParcelable<ChatChannel>(ARG_CHAT_CHANNEL)!!

        val contentResolver = activity.contentResolver
        var file: File?

        viewModel.currentDocUri.observe(viewLifecycleOwner) {
            if (it != null) {

                val cursor = contentResolver.query(it, null, null, null, null)

                cursor?.moveToFirst()
                val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor?.getColumnIndex(OpenableColumns.SIZE)

                var name = cursor?.getString(nameIndex ?: 0)

                val size = (cursor?.getLong(sizeIndex ?: 0) ?: 0)
                val sizeText = when {
                    size > (1024 * 1024) -> {
                        val sizeInMB = size.toFloat()/(1024 * 1024)
                        sizeInMB.toString().take(4) + " MB"
                    }
                    size/1024 > 100 -> {
                        val sizeInMB = size.toFloat()/(1024 * 1024)
                        sizeInMB.toString().take(4) + " MB"
                    }
                    else -> {
                        val sizeInKB = size.toFloat()/1024
                        sizeInKB.toString().take(3) + " KB"
                    }
                }

                binding.docSize.text = sizeText
                cursor?.close()

                val ins = contentResolver.openInputStream(it)!!
                val byteArray = ByteArray(ins.available())
                ins.read(byteArray)
                val externalDir = activity.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)

                file = File(externalDir, name!!)
                if (file != null) {
                    var counter = 0
                    val ext = file!!.extension

                    while (file!!.exists()) {
                        counter++
                        val actName = name!!.substring(0, name.length - (ext.length + 1))
                        name = "$actName($counter).$ext"
                        file = File(externalDir, name)
                    }

                    val metadata = MediaMetaData(size, name!!, ext)

                    binding.docName.text = name

                    binding.sendDocBtn.setOnClickListener { _ ->
                        if (file!!.createNewFile()) {
                            val outs = FileOutputStream(file)
                            outs.write(byteArray)
                            outs.flush()
                            outs.close()
                            val currentUser = viewModel.user.value!!

                            val message = SimpleMessage("", chatChannel.chatChannelId, DOCUMENT, it.toString(), currentUser.id, metaData = metadata, currentUser, isDownloaded = true)
                            viewModel.uploadMessageMedia(message, chatChannel)

                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Could not create file.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        activity.bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                    }
                }

                viewModel.setCurrentDoc(null)
            }
        }


        binding.cancelDocUpload.setOnClickListener {
            viewModel.setCurrentDoc(null)
            activity.bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

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