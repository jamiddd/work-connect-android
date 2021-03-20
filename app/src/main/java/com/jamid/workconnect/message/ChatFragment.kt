package com.jamid.workconnect.message

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.transition.TransitionManager
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.drawee.view.SimpleDraweeView
import com.firebase.ui.common.ChangeEventType.*
import com.github.mmin18.widget.RealtimeBlurView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.transition.platform.MaterialArcMotion
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.*
import com.jamid.workconnect.adapter.SimpleMessageAdapter
import com.jamid.workconnect.databinding.FragmentChatBinding
import com.jamid.workconnect.interfaces.ChatMenuClickListener
import com.jamid.workconnect.model.ChatChannel
import com.jamid.workconnect.model.ChatChannelContributor
import com.jamid.workconnect.model.Result
import com.jamid.workconnect.model.SimpleMessage
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class ChatFragment : Fragment(R.layout.fragment_chat), Animator.AnimatorListener, GestureDetector.OnGestureListener, ChatMenuClickListener {

    private lateinit var binding: FragmentChatBinding
    private lateinit var simpleMessageAdapter: SimpleMessageAdapter
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var chatChannel: ChatChannel
    private var isImageMode = false
    private var fullscreenImage: SimpleDraweeView? = null
    private var smallImage: SimpleDraweeView? = null
    private var scrimView: View? = null
    private var appbar: AppBarLayout? = null
    private var keyboardHeight = 0
    private var tempUploadingImage: View? = null
    private var currentBufferedMessageId: String = ""
    private var currentBufferedDocReference: DocumentReference? = null
    private lateinit var mDetector: GestureDetectorCompat
    private lateinit var currentContributor: ChatChannelContributor
    private var onBackPressedCallback: OnBackPressedCallback? = null
    private lateinit var activity: MainActivity
    private lateinit var listenerRegistration: ListenerRegistration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.chat_fragment_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.project_info_item -> {
                activity.projectDetailFragment(chatChannel, currentContributor)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentChatBinding.bind(view)
        chatChannel = arguments?.getParcelable(ARG_CHAT_CHANNEL) ?: return

        activity = requireActivity() as MainActivity
        val uid = Firebase.auth.currentUser?.uid.toString()

        val bnv = activity.findViewById<BottomNavigationView>(R.id.bottomNav)
        val blur = activity.findViewById<RealtimeBlurView>(R.id.bottom_blur)
        appbar = activity.findViewById(R.id.primaryAppBar)

        bnv.hide(blur)

        viewModel.channelContributors(chatChannel).observe(viewLifecycleOwner) {
            if (it != null && it.isNotEmpty()) {
                simpleMessageAdapter = SimpleMessageAdapter(it, lifecycleScope, activity)
                for (user in it) {
                    if (user.contributor.id == viewModel.user.value?.id) {
                        currentContributor = user.contributor
                        break
                    }
                }

                binding.messagesRecycler.apply {
                    itemAnimator = null
                    adapter = simpleMessageAdapter
                    layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, true)
                }

                OverScrollDecoratorHelper.setUpOverScroll(binding.messagesRecycler, OverScrollDecoratorHelper.ORIENTATION_VERTICAL)

                listenerRegistration = Firebase.firestore.collection(CHAT_CHANNELS).document(chatChannel.chatChannelId)
                    .collection(MESSAGES)
                    .orderBy(CREATED_AT, Query.Direction.DESCENDING)
                    .limit(it.size.toLong())
                    .addSnapshotListener { value, error ->
                        if (error != null) {
                            Toast.makeText(requireContext(), error.localizedMessage, Toast.LENGTH_SHORT).show()
                            return@addSnapshotListener
                        }

                        if (value != null && !value.isEmpty) {
                            val messages = value.toObjects(SimpleMessage::class.java)
                            viewModel.insertMessages(messages)
                        }
                    }


                viewModel.chatMessages(chatChannel.chatChannelId).observe(viewLifecycleOwner) { list ->
                    simpleMessageAdapter.submitList(list)
                }

            }
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { insets ->
            if (insets != null) {
                binding.bottomMessageLayout.setPadding(0, 0, 0, insets.second + convertDpToPx(8))
                binding.messagesRecycler.setPadding(0, insets.first + convertDpToPx(64), 0, insets.second + convertDpToPx(64))
            }
        }

        constantWork(activity)

    }

    private fun constantWork(activity: MainActivity) {
        activity.mainBinding.primaryTitle.text = chatChannel.postTitle
        activity.mainBinding.primaryToolbar.setNavigationOnClickListener {
            hideKeyboard()
            val fragment = activity.supportFragmentManager.findFragmentByTag(ImageViewFragment.TAG)
            if (fragment?.isVisible == true) {
                activity.supportFragmentManager.beginTransaction().remove(fragment).commit()
            } else {
                findNavController().navigateUp()
            }
        }

        onBackPressedCallback = activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            hideKeyboard()
            findNavController().navigateUp()
        }

        binding.sendMsgBtn.setOnClickListener {
            if (binding.messageText.text.isNullOrBlank()) return@setOnClickListener
            val messageRef = Firebase.firestore.collection(CHAT_CHANNELS).document(chatChannel.chatChannelId).collection(
                MESSAGES).document()
            val messageId = messageRef.id
            val msg = binding.messageText.text.toString()
            viewModel.sendMessage(messageRef, messageId, msg, chatChannel.chatChannelId, TEXT)
            binding.messageText.text.clear()
        }

        binding.addImgMsgBtn.setOnClickListener {
            val fragment = MessageMenuFragment.newInstance()
            activity.showBottomSheet(fragment, "MessageMenuFragment")
        }

        var layoutHeight = 0
        binding.root.doOnLayout {
            layoutHeight = binding.root.measuredHeight
        }

        binding.root.addOnLayoutChangeListener { view1, _, _, _, _, _, _, _, _ ->
            val diff = view1.measuredHeight - layoutHeight

            Log.d("ChatFragment", "Root Height = " + binding.chatFragmentRoot.measuredHeight.toString())

            if (diff < 0) {
                binding.messagesRecycler.smoothScrollToPosition(0)
            } else {
                keyboardHeight = diff
                Log.d("ChatFragment", "Keyboard Height = $keyboardHeight")
            }
            layoutHeight += diff

        }
        viewModel.imgMessageUploadResult.observe(viewLifecycleOwner) {
            val result = it ?: return@observe

            when (result) {
                is Result.Success -> {
                    viewModel.sendMessage(currentBufferedDocReference!!, currentBufferedMessageId, result.data, chatChannel.chatChannelId, "Image")
                    tempUploadingImage = null
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

        // onUpload
        /*Log.d("ChatFragmentDoc", it.toString())
        val file = File(it.path)

        if (file.exists()) {
            Toast.makeText(requireContext(), file.name + file.extension, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "File doesn't exists.", Toast.LENGTH_SHORT)
                .show()
        }*/
        /*val ref = Firebase.storage.reference.child("test")

        ref.putFile(it)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUri ->
                    val downloadUrl = downloadUri.toString()
                    Toast.makeText(requireContext(), downloadUrl, Toast.LENGTH_SHORT).show()
                }.addOnFailureListener {
                    Toast.makeText(
                        requireContext(),
                        "Something went wrong.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }*/

        viewModel.currentCroppedImageUri.observe(viewLifecycleOwner) {
            if (it != null) {
                currentBufferedDocReference = Firebase.firestore.collection(CHAT_CHANNELS).document(chatChannel.chatChannelId).collection(MESSAGES).document()
                currentBufferedMessageId = currentBufferedDocReference!!.id
                viewModel.uploadImgMessage(it, chatChannel.chatChannelId, currentBufferedMessageId)
            }
        }
    }
    /*private fun removeTempView() {
        binding.placeholderMsg.root.visibility = View.GONE
        val s: String? = null
        binding.placeholderMsg.imgMsgRight.setImageURI(s)
    }
    private fun addTemporaryView(image: Uri) {
        binding.messagesRecycler.smoothScrollToPosition(0)
        binding.placeholderMsg.root.visibility = View.VISIBLE
        binding.placeholderMsg.imgMsgRight.setImageURI(image.toString())
        binding.placeholderMsg.currentUserMessage.visibility = View.GONE
        binding.placeholderMsg.imgMsgRight.visibility = View.VISIBLE
        binding.placeholderMsg.imgMsgRight.setColorFilter(ContextCompat.getColor(requireContext(), R.color.semiTransparentDark))
        binding.placeholderMsg.imgMsgUploadProgress.visibility = View.VISIBLE
    }*/
    private fun selectImage() {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
        }
        startActivityForResult(intent, ADD_IMAGE_REQUEST)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                ADD_IMAGE_REQUEST -> {
                    val image = data?.data
                    val bundle = Bundle().apply {
                        putBoolean("freeMode", true)
                    }
                    viewModel.setCurrentImage(image)
                    findNavController().navigate(R.id.imageCropFragment, bundle)
                }
            }
        } else {
//            messageAdapter.notifyDataSetChanged()
//            Toast.makeText(requireContext(), "Didn't select any image. ${messageAdapter.itemCount}", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {

        private const val ADD_IMAGE_REQUEST = 23
        const val ARG_CHAT_CHANNEL = "chatChannel"
        const val ARG_CURRENT_CONTRIBUTOR = "currentContributor"
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
        listenerRegistration.remove()
        onBackPressedCallback?.remove()
        hideKeyboard()
    }

   /* override fun onImageClick(view: SimpleDraweeView, actualWidth: Int, actualHeight: Int, message: SimpleMessage) {
        isImageMode = true
        createImageView(view, actualWidth, actualHeight, message.content)
    }*/

    @SuppressLint("ClickableViewAccessibility")
    private fun createImageView(sv: SimpleDraweeView, actualWidth: Int, actualHeight: Int, img: String) {
        val parent = binding.chatFragmentRoot
        val simpleDraweeView = SimpleDraweeView(requireContext())
        val scrim = RealtimeBlurView(requireContext(), null)
        scrim.setBlurRadius(convertDpToPx(30f))

       /* val secondToolbar = MaterialToolbar(requireContext())
        secondToolbar.navigationIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_arrow_back_24)
        secondToolbar.alpha = 0f
        secondToolbar.elevation = convertDpToPx(20f)
        secondToolbar.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white
        ))*/

        scrim.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
        scrim.alpha = 0f
        scrim.isClickable  = true

        Log.d("ChatFragment", sv.measuredHeight.toString() + " " + sv.measuredWidth.toString())

        val screenWidth = getWindowWidth()
        val screenHeight = parent.measuredHeight

        binding.chatFragmentRoot.addView(scrim, ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT))

        if (actualWidth >= actualHeight) {
            // horizontal

            val normalizedHeight = (actualHeight * screenWidth) / actualWidth

            binding.chatFragmentRoot.addView(simpleDraweeView, ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, normalizedHeight))
        } else {
            // vertical

            val normalizedWidth = (actualWidth * screenHeight) / actualHeight;

            binding.chatFragmentRoot.addView(simpleDraweeView, ConstraintLayout.LayoutParams(normalizedWidth, ConstraintLayout.LayoutParams.MATCH_PARENT))
        }

//        val insets = viewModel.windowInsets.value!!
//        binding.chatFragmentRoot.addView(secondToolbar, ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, insets.first + convertDpToPx(56)))

        val scrimParams = scrim.layoutParams as ConstraintLayout.LayoutParams
        val params = simpleDraweeView.layoutParams as ConstraintLayout.LayoutParams
        scrimParams.startToStart = parent.id
        scrimParams.endToEnd = parent.id
        scrimParams.topToTop = parent.id
        scrimParams.bottomToBottom = parent.id
        params.startToStart = parent.id
        params.endToEnd = parent.id
        params.topToTop = parent.id
        params.bottomToBottom = parent.id

        scrim.layoutParams = scrimParams

        simpleDraweeView.layoutParams = params
        simpleDraweeView.hierarchy.actualImageScaleType = ScalingUtils.ScaleType.FIT_CENTER

        simpleDraweeView.setImageURI(img)

//        val appbarHeight = binding.chatAppbar.measuredHeight.toFloat()
//        val toolbarSlideUp = ObjectAnimator.ofFloat(binding.chatAppbar, View.TRANSLATION_Y, 0f, -(appbarHeight + 100f))
        val messageLayoutSlideDown = ObjectAnimator.ofFloat(binding.bottomMessageLayout, View.TRANSLATION_Y, 0f, binding.bottomMessageLayout.measuredHeight.toFloat())
        val scrimFadeIn = ObjectAnimator.ofFloat(scrim, View.ALPHA, 0f, 1f)
//        val secondToolbarFadeIn = ObjectAnimator.ofFloat(secondToolbar, View.ALPHA, 0f, 0.5f)
        val appbarSlide = AnimatorSet().apply {
//            addListener(this@ChatFragment)
            playTogether(listOf(/*toolbarSlideUp, */messageLayoutSlideDown, scrimFadeIn/*, secondToolbarFadeIn*/))
        }

        appbarSlide.start()

        val transform = MaterialContainerTransform().apply {
            startView = sv
            endView = simpleDraweeView

            addTarget(endView)

            pathMotion = MaterialArcMotion()

            scrimColor = Color.TRANSPARENT
        }

        /*secondToolbar.setNavigationOnClickListener {
            revertImageMode()
        }*/

        TransitionManager.beginDelayedTransition(parent, transform)

        mDetector = GestureDetectorCompat(requireContext(), this)

        simpleDraweeView.setOnTouchListener { v, event ->

            v.performClick()

            mDetector.onTouchEvent(event)

            /*mActivePointerId = event.getPointerId(0)

            val (x: Float, y: Float) = event.findPointerIndex(mActivePointerId).let { pointerIndex ->
                event.getX(pointerIndex) to event.getY(pointerIndex)
            }
            Log.d("ChatFragment", "$x, $y")
            val (xPos: Int, yPos: Int) = event.actionMasked.let { action ->

                Log.d("ChatFragment", "The action is ${actionToString(action)}")

                event.actionIndex.let { index ->
                    event.getX(index).toInt() to event.getY(index).toInt()
                }
            }

            if (event.pointerCount > 1) {
                Log.d("ChatFragment", "Multitouch event - ($xPos, $yPos)")
            } else {
                Log.d("ChatFragment", "Single touch event")
            }*/

            true
        }

        smallImage = sv
        fullscreenImage = simpleDraweeView
        scrimView = scrim
//        tempToolbar = secondToolbar
    }

    private fun revertImageMode() {
        if (appbar?.translationY != 0f) {
            showSecondToolbar()
        }
//        val appbarHeight = binding.chatAppbar.measuredHeight.toFloat()
//        val appbarSlideDown = ObjectAnimator.ofFloat(binding.chatAppbar, View.TRANSLATION_Y, -(appbarHeight + 100f), 0f)
        val messageLayoutSlideUp = ObjectAnimator.ofFloat(binding.bottomMessageLayout, View.TRANSLATION_Y, binding.bottomMessageLayout.measuredHeight.toFloat(), 0f)
        val scrimFadeOut = ObjectAnimator.ofFloat(scrimView, View.ALPHA, 1f, 0f)
//        val secondToolbarFadeOut = ObjectAnimator.ofFloat(tempToolbar, View.ALPHA, 0.5f, 0f)
        val appbarSlide = AnimatorSet().apply {
//            addListener(this@ChatFragment)
            playTogether(listOf(/*appbarSlideDown, */messageLayoutSlideUp, scrimFadeOut/*, secondToolbarFadeOut*/))
        }

        appbarSlide.start()

        val transform = MaterialContainerTransform().apply {
            startView = fullscreenImage
            endView = smallImage

            addTarget(endView)

            pathMotion = MaterialArcMotion()

            scrimColor = Color.TRANSPARENT
        }

        TransitionManager.beginDelayedTransition(binding.chatFragmentRoot, transform)
        binding.chatFragmentRoot.removeView(scrimView)
        binding.chatFragmentRoot.removeView(fullscreenImage)
//        binding.chatFragmentRoot.removeView(tempToolbar)
        scrimView = null
        fullscreenImage = null
//        tempToolbar = null

        isImageMode = false

    }

   /* override fun onTextClick(content: String) {
        //
    }*/

    override fun onAnimationStart(animation: Animator?) {
        Log.d("ChatFragment", "Animation has started")
    }

    override fun onAnimationEnd(animation: Animator?) {

    }

    override fun onAnimationCancel(animation: Animator?) {
        Log.d("ChatFragment", "Animation was canceled")
    }

    override fun onAnimationRepeat(animation: Animator?) {
        Log.d("ChatFragment", "Animation has repeated")
    }

    // Given an action int, returns a string description
    fun actionToString(action: Int): String {
        return when (action) {
            MotionEvent.ACTION_DOWN -> "Down"
            MotionEvent.ACTION_MOVE -> "Move"
            MotionEvent.ACTION_POINTER_DOWN -> "Pointer Down"
            MotionEvent.ACTION_UP -> "Up"
            MotionEvent.ACTION_POINTER_UP -> "Pointer Up"
            MotionEvent.ACTION_OUTSIDE -> "Outside"
            MotionEvent.ACTION_CANCEL -> "Cancel"
            else -> ""
        }
    }

    override fun onDown(e: MotionEvent?): Boolean {
        return true
    }

    override fun onShowPress(e: MotionEvent?) {

    }

    override fun onSingleTapUp(e: MotionEvent?): Boolean {
        if (appbar?.translationY == 0f) {
            hideSecondToolbar()
        } else {
            showSecondToolbar()
        }
        return true
    }

    private fun hideSecondToolbar() {
        if (isImageMode) {
            val tempToolbarSlideUp = ObjectAnimator.ofFloat(appbar, View.TRANSLATION_Y, -appbar?.measuredHeight?.toFloat()!!)
            tempToolbarSlideUp.start()
        }
    }

    private fun showSecondToolbar() {
        if (isImageMode) {
            val tempToolbarSlideDown = ObjectAnimator.ofFloat(appbar, View.TRANSLATION_Y, 0f)
            tempToolbarSlideDown.start()
        }
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent?,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        return true
    }

    override fun onLongPress(e: MotionEvent?) {

    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        revertImageMode()
        return true
    }

    override fun onImageSelect() {
        selectImage()
    }

    override fun onCameraSelect() {
        //
    }

    override fun onDocumentSelect() {
        //
    }

}