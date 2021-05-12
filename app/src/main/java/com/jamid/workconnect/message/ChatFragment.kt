package com.jamid.workconnect.message

import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import android.view.*
import androidx.core.view.doOnLayout
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.common.ChangeEventType.*
import com.jamid.workconnect.*
import com.jamid.workconnect.adapter.paging2.SimpleMessageAdapter
import com.jamid.workconnect.databinding.FragmentChatBinding
import com.jamid.workconnect.model.*
import kotlinx.coroutines.Job
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class ChatFragment : SupportFragment(R.layout.fragment_chat, TAG, false) {

    private lateinit var binding: FragmentChatBinding
    private lateinit var simpleMessageAdapter: SimpleMessageAdapter
    private lateinit var chatChannel: ChatChannel
    private var keyboardHeight = 0
    private var lastMessageCount = 0
    private var job: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentChatBinding.bind(view)
        chatChannel = arguments?.getParcelable(ARG_CHAT_CHANNEL) ?: return
        viewModel.extras[ProjectDetailFragment.ARG_CHAT_CHANNEL] = chatChannel
        activity.setFragmentTitle(chatChannel.postTitle)

        viewModel.channelContributorsLive(chatChannel.chatChannelId).observe(viewLifecycleOwner) {
            activity.mainBinding.primaryProgressBar.visibility = View.VISIBLE
            if (it.isNotEmpty() && it.size.toLong() == chatChannel.contributorsCount) {
                initChat(it)
                constantWork(it)
            } else {
                viewModel.getChannelContributors(chatChannel.chatChannelId)
            }
        }

        initLayoutChanges()
    }

    private fun initLayoutChanges() {
        viewModel.windowInsets.observe(viewLifecycleOwner) { insets ->
            if (insets != null) {
                binding.messagesRecycler.setPadding(0, insets.first + convertDpToPx(64), 0, insets.second + convertDpToPx(72))

                binding.messageText.updateLayout(marginTop = convertDpToPx(8), marginRight = convertDpToPx(4), marginBottom = insets.second + convertDpToPx(8))

                val parentId = binding.chatFragmentRoot.id
                binding.noChatLayoutScroll.updateLayout(
                marginBottom = insets.second, extras = mapOf(
                        START_TO_START to parentId,
                        END_TO_END to parentId,
                        TOP_TO_TOP to parentId,
                        BOTTOM_TO_BOTTOM to parentId
                    )
                )
            }
        }

        activity.mainBinding.primaryMenuBtn.setOnClickListener {
            val fragment = ProjectDetailFragment.newInstance(chatChannel)
            activity.toFragment(fragment, ProjectDetailFragment.TAG)
        }

    }

    private fun initChat(contributors: List<User>) {
        Log.d(BUG_TAG, "Starting chat adapter ... ")
        simpleMessageAdapter = SimpleMessageAdapter(viewModel, activity)

        binding.messagesRecycler.apply {
            itemAnimator = null
            adapter = simpleMessageAdapter
            layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, true)
        }

        OverScrollDecoratorHelper.setUpOverScroll(binding.messagesRecycler, OverScrollDecoratorHelper.ORIENTATION_VERTICAL)
        OverScrollDecoratorHelper.setUpOverScroll(binding.noChatLayoutScroll)

        viewModel.chatMessages(chatChannel.chatChannelId, contributors).observe(viewLifecycleOwner) { messages ->
            if (messages.isNotEmpty()) {
                job?.cancel()
                activity.mainBinding.primaryProgressBar.visibility = View.GONE
                binding.noChatLayoutScroll.visibility = View.GONE
                binding.messagesRecycler.visibility = View.VISIBLE
                simpleMessageAdapter.submitList(messages)
                if (messages.size != lastMessageCount) {
                    scrollToBottom()
                }
                lastMessageCount = messages.size
            } else {
                activity.mainBinding.primaryProgressBar.visibility = View.GONE
                binding.noChatLayoutScroll.visibility = View.VISIBLE
                binding.messagesRecycler.visibility = View.GONE
            }
        }

    }

    private fun scrollToBottom() {
        binding.messagesRecycler.smoothScrollToPosition(0)
    }

    private fun openChatMenu() {
        hideKeyboard()
        val tag = CHAT_MENU
        val item1 = GenericMenuItem(tag, "Select from gallery", R.drawable.ic_baseline_add_photo_alternate_24, 0)
        val item2 = GenericMenuItem(tag, "Take a photo", R.drawable.ic_baseline_camera_alt_24, 1)
        val item3 = GenericMenuItem(tag, "Document", R.drawable.ic_round_notes_24, 2)

        val fragment = GenericMenuFragment.newInstance(tag, "Send Message ...", arrayListOf(item1, item2, item3))
        activity.showBottomSheet(fragment, tag)
    }

    private fun constantWork(contributors: List<User>) {
        val externalDir = activity.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (externalDir != null) {
            viewModel.setChannelListener(externalDir, chatChannel, contributors)
        }

        binding.sendMsgBtn.setOnClickListener {
            if (binding.messageText.text.isNullOrBlank()) return@setOnClickListener
            val content = binding.messageText.text.toString()
            val currentUser = viewModel.user.value!!
            val message = SimpleMessage("", chatChannel.chatChannelId, TEXT, content, currentUser.id, sender = currentUser)
            viewModel.sendMessage(message, chatChannel)
            binding.messageText.text.clear()
        }

        binding.sendMsgBtn.isEnabled = false

        binding.messageText.doAfterTextChanged {
            binding.sendMsgBtn.isEnabled = !it.isNullOrBlank()
        }

        binding.addImgMsgBtn.setOnClickListener {
            openChatMenu()
        }

        var layoutHeight = 0
        binding.root.doOnLayout {
            layoutHeight = binding.root.measuredHeight
        }

        binding.root.addOnLayoutChangeListener { view1, _, _, _, _, _, _, _, _ ->
            val diff = view1.measuredHeight - layoutHeight

            if (diff < 0) {
                binding.messagesRecycler.smoothScrollToPosition(0)
            } else {
                keyboardHeight = diff
            }
            layoutHeight += diff

        }

        viewModel.mediaUploadResult.observe(viewLifecycleOwner) {
            val result = it ?: return@observe

            activity.hideBottomSheet()

            when (result) {
                is Result.Success -> {

                    /*val message = SimpleMessage(result.data.id, chatChannel.chatChannelId, result.data.type, result.data.mediaLocation, viewModel.user.value!!.id, User())
                    viewModel.sendMessage(message, chatChannel, result.data)*/

                }
                is Result.Error -> {
                    viewModel.setCurrentError(result.exception)
                }
            }

            viewModel.setImageMessageUploadResult(null)
        }

        viewModel.currentDocUri.observe(viewLifecycleOwner) {
            if (it != null) {
                val fragment = UploadDocumentFragment.newInstance(chatChannel)
                activity.showBottomSheet(fragment, UploadDocumentFragment.TAG)
            }
        }

        val contentResolver = activity.contentResolver

        viewModel.currentCroppedImageUri.observe(viewLifecycleOwner) {
            if (it != null) {
                val cursor = contentResolver.query(it, null, null, null, null)

                try {
                    cursor?.moveToFirst()
                    val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor?.getColumnIndex(OpenableColumns.SIZE)

                    val name = cursor?.getString(nameIndex ?: 0)
                    val size = (cursor?.getLong(sizeIndex ?: 0) ?: 0)

                    cursor?.close()
                    val currentUser = viewModel.user.value!!
                    val metaData = MediaMetaData(size, name!!, ".jpg")
                    val message = SimpleMessage("", chatChannel.chatChannelId, IMAGE, it.toString(), currentUser.id, metaData = metaData, currentUser)

//                val simpleMedia = SimpleMedia("", IMAGE, it.toString(), System.currentTimeMillis(), chatChannel.chatChannelId, viewModel.user.value!!.id, name, size)
                    viewModel.uploadMessageMedia(message, chatChannel)
                } catch (e: Exception) {
                    Log.e(TAG,e.localizedMessage + it.toString())
                } finally {
                    viewModel.setCurrentCroppedImageUri(null)
                }

            }
        }
    }

    companion object {

        const val ARG_CHAT_CHANNEL = "chatChannel"
        const val TAG = "ChatFragment"
        @JvmStatic
        fun newInstance(chatChannel: ChatChannel) = ChatFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_CHAT_CHANNEL, chatChannel)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideKeyboard()
    }

}