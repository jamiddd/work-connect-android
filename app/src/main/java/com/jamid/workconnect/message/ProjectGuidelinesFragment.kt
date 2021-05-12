package com.jamid.workconnect.message

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.jamid.workconnect.R
import com.jamid.workconnect.SupportFragment
import com.jamid.workconnect.convertDpToPx
import com.jamid.workconnect.databinding.FragmentProjectGuidelinesBinding
import com.jamid.workconnect.getWindowHeight
import com.jamid.workconnect.model.ChatChannel
import com.jamid.workconnect.model.Post
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ProjectGuidelinesFragment : SupportFragment(R.layout.fragment_project_guidelines, TAG, false) {

    private lateinit var binding: FragmentProjectGuidelinesBinding
    private var positionFromBottom = 0
    private var saved = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentProjectGuidelinesBinding.bind(view)
        val chatChannel = arguments?.getParcelable<ChatChannel>(ARG_CHAT_CHANNEL) ?: return
        val post = arguments?.getParcelable<Post>(ARG_POST) ?: return
        viewModel.extras[ARG_POST] = post
        binding.pgText.setText(post.guidelines)

        viewModel.guidelinesUpdateResult.observe(viewLifecycleOwner) {
            if (it != null) {
                activity.mainBinding.primaryProgressBar.visibility = View.GONE
                activity.onBackPressed()
                viewModel.clearGuidelinesUpdateResult()
            }
        }

        val currentUser = viewModel.user.value!!

        if (chatChannel.administrators.contains(currentUser.id)) {
            binding.pgText.isEnabled = true

            activity.mainBinding.primaryMenuBtn.isEnabled = true
            lifecycleScope.launch {
                delay(1000)
                activity.mainBinding.primaryMenuBtn.setOnClickListener {
                    activity.mainBinding.primaryProgressBar.visibility = View.VISIBLE
                    val text = binding.pgText.text
                    if (!text.isNullOrBlank()) {
                        saved = true
                        viewModel.updatePost(post, text.trimEnd().toString())
                    }
                }
            }


            val height = getWindowHeight()

            binding.editNotification.setBackgroundColor(ContextCompat.getColor(activity, R.color.blue_500))
            binding.editNotification.setTextColor(ContextCompat.getColor(activity, R.color.white))
            binding.editNotification.text =  "You are in edit mode. Only you and other admins can edit the guidelines."

            viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->

                val params = binding.editNotification.layoutParams as CoordinatorLayout.LayoutParams
                params.setMargins(0, top + convertDpToPx(56), 0, 0)
                binding.editNotification.layoutParams = params

                binding.pgContentScroller.setPadding(0, top + convertDpToPx(64) + binding.editNotification.measuredHeight, 0, bottom + convertDpToPx(8))

                val child = binding.pgText
                val r = Rect()
                child.getGlobalVisibleRect(r)

                val p = child.selectionStart
                val layout = child.layout
                if (layout != null) {
                    val line = layout.getLineForOffset(p)
                    val baseline = layout.getLineBaseline(line)
                    val ascent = layout.getLineAscent(line)

                    val cursorY = baseline + ascent + r.top

                    positionFromBottom = height - (bottom + convertDpToPx(48))

                    if (cursorY > positionFromBottom) {
                        // when the cursor is below the keyboard
                        val diff = cursorY - positionFromBottom
                        binding.pgContentScroller.scrollY = binding.pgContentScroller.scrollY + diff
                    } else {
                        // when the cursor is above the keyboard
                    }
                }
            }

        } else {
            activity.mainBinding.primaryMenuBtn.isEnabled = false
            binding.pgText.isEnabled = false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (activity.resources?.configuration?.isNightModeActive == true) {
                    binding.editNotification.setBackgroundColor(Color.parseColor("#4C4B4B"))
                    binding.editNotification.setTextColor(ContextCompat.getColor(activity, R.color.grey))
                } else {
                    binding.editNotification.setBackgroundColor(ContextCompat.getColor(activity, R.color.grey))
                    binding.editNotification.setTextColor(ContextCompat.getColor(activity, R.color.darkerGrey))
                }
            } else {
                if (activity.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
                    binding.editNotification.setBackgroundColor(Color.parseColor("#4C4B4B"))
                    binding.editNotification.setTextColor(ContextCompat.getColor(activity, R.color.grey))
                } else {
                    binding.editNotification.setBackgroundColor(ContextCompat.getColor(activity, R.color.grey))
                    binding.editNotification.setTextColor(ContextCompat.getColor(activity, R.color.darkerGrey))
                }
            }

            binding.editNotification.text =  "Only an admin can update the guidelines."

            activity.mainBinding.primaryMenuBtn.setOnClickListener {
                //
            }

            viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->

                val params = binding.editNotification.layoutParams as CoordinatorLayout.LayoutParams
                params.setMargins(0, top + convertDpToPx(56), 0, 0)
                binding.editNotification.layoutParams = params

                binding.pgContentScroller.setPadding(0, top + convertDpToPx(64) + binding.editNotification.measuredHeight, 0, bottom + convertDpToPx(8))

            }

        }
    }

    companion object {

        const val TITLE = "Guidelines"
        const val TAG = "ProjectGuidelines"
        const val ARG_CHAT_CHANNEL = "ARG_CHAT_CHANNEL"
        const val ARG_POST = "ARG_POST"

        @JvmStatic
        fun newInstance(chatChannel: ChatChannel, post: Post) =
            ProjectGuidelinesFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_CHAT_CHANNEL, chatChannel)
                    putParcelable(ARG_POST, post)
                }
            }
    }
}