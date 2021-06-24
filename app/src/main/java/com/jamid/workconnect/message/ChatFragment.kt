package com.jamid.workconnect.message

import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import android.view.*
import androidx.core.view.doOnLayout
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.common.ChangeEventType.*
import com.jamid.workconnect.*
import com.jamid.workconnect.adapter.paging2.SimpleMessageAdapter
import com.jamid.workconnect.databinding.FragmentChatBinding
import com.jamid.workconnect.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class ChatFragment : SupportFragment(R.layout.fragment_chat, TAG, false) {

    private lateinit var binding: FragmentChatBinding
    private lateinit var simpleMessageAdapter: SimpleMessageAdapter
    private lateinit var chatChannel: ChatChannel
    private var keyboardHeight = 0
    private var job: Job? = null
    private var initiateListeners = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentChatBinding.bind(view)

        chatChannel = arguments?.getParcelable(ARG_CHAT_CHANNEL) ?: return

        binding.sendMsgBtn.isEnabled = false
        binding.chatFragmentToolbar.title = chatChannel.postTitle

        binding.chatFragmentToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        if (!initiateListeners) {
            viewModel.setChannelContributorsListener(chatChannel)

            val externalDocumentsDir = activity.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!
            val externalImagesDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
            viewModel.setChannelMessagesListener(externalImagesDir, externalDocumentsDir, chatChannel)


            initiateListeners = true
        }

        initAdapter(binding.messagesRecycler, viewModel)

        getMessages()

        setListeners()
    }

    private fun initAdapter(recyclerView: RecyclerView, vm: ViewModel) {
        simpleMessageAdapter = SimpleMessageAdapter(vm as MainViewModel)
        val linearLayoutManager = LinearLayoutManager(recyclerView.context, LinearLayoutManager.VERTICAL, true)
        recyclerView.apply {
            itemAnimator = null
            adapter = simpleMessageAdapter
            layoutManager = linearLayoutManager
        }
    }

    private fun getMessages() {
        job?.cancel()
        val externalDocumentsDir = activity.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!
        val externalImagesDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!

        job = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.chatChannelMessages(chatChannel, externalImagesDir, externalDocumentsDir).collectLatest {
                simpleMessageAdapter.submitData(it)
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


    private fun setListeners() {

        viewModel.windowInsets.observe(viewLifecycleOwner) { insets ->
            if (insets != null) {
                binding.messagesRecycler.setPadding(0, convertDpToPx(8), 0, insets.second + convertDpToPx(64))

                binding.chatFragmentAppBar.setPadding(0, insets.first, 0, 0)

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

        val options = navOptions {
            anim {
                enter = R.anim.slide_in_right
                exit = R.anim.slide_out_left
                popEnter = R.anim.slide_in_left
                popExit = R.anim.slide_out_right
            }
            popUpTo(R.id.chatFragment) {
                saveState = true
            }
            restoreState = true
        }

        binding.projectInfoButton.setOnClickListener {
            val bundle = Bundle().apply {
                putParcelable(ProjectDetailFragment.ARG_CHAT_CHANNEL, chatChannel)
            }

            findNavController().navigate(R.id.projectDetailFragment, bundle, options)
        }

        simpleMessageAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                binding.chatFragmentProgress.visibility = View.GONE
                scrollToBottom()
            }
        })

        binding.sendMsgBtn.setOnClickListener {
            if (binding.messageText.text.isNullOrBlank()) return@setOnClickListener
            val content = binding.messageText.text.toString()
            val currentUser = viewModel.user.value!!
            val message = SimpleMessage("", chatChannel.chatChannelId, TEXT, content, currentUser.id, sender = currentUser)
            viewModel.sendMessage(message, chatChannel)
            binding.messageText.text.clear()
        }

        binding.messageText.doAfterTextChanged {
            binding.sendMsgBtn.isEnabled = !it.isNullOrBlank()
        }

        binding.addImgMsgBtn.setOnClickListener {
            openChatMenu()
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

                    viewModel.uploadMessageMedia(message, chatChannel)
                } catch (e: Exception) {
                    Log.e(TAG,e.localizedMessage!! + it.toString())
                } finally {
                    viewModel.setCurrentCroppedImageUri(null)
                }
            }
        }

        viewModel.currentDocUri.observe(viewLifecycleOwner) {
            if (it != null) {
                val fragment = UploadDocumentFragment.newInstance(chatChannel)
                activity.showBottomSheet(fragment, UploadDocumentFragment.TAG)
            }
        }

        binding.messagesRecycler.addOnLayoutChangeListener { view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (bottom < oldBottom) {
                binding.messagesRecycler.postDelayed({
                    binding.messagesRecycler.smoothScrollToPosition(
                        0
                    )
                }, 100)
            }
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

        viewLifecycleOwner.lifecycleScope.launch {
            binding.chatFragmentProgress.visibility = View.VISIBLE
            simpleMessageAdapter.loadStateFlow.collectLatest { loadStates ->
                if (loadStates.refresh is LoadState.NotLoading) {
                    delay(1000)

                    binding.chatFragmentProgress.visibility = View.GONE

                    if (simpleMessageAdapter.itemCount == 0) {
                        binding.noChatLayoutScroll.visibility = View.VISIBLE
                        binding.messagesRecycler.visibility = View.GONE
                    } else {
                        binding.noChatLayoutScroll.visibility = View.GONE
                        binding.messagesRecycler.visibility = View.VISIBLE
                    }
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

/*viewModel.mediaUploadResult.observe(viewLifecycleOwner) {
		   val result = it ?: return@observe

		   activity.hideBottomSheet()

		   when (result) {
			   is Result.Success -> {

				   val message = SimpleMessage(result.data.id, chatChannel.chatChannelId, result.data.type, result.data.mediaLocation, viewModel.user.value!!.id, User())
                    viewModel.sendMessage(message, chatChannel, result.data)
                }
                is Result.Error -> {
                    viewModel.setCurrentError(result.exception)
                }
            }

            viewModel.setImageMessageUploadResult(null)
        }*/
