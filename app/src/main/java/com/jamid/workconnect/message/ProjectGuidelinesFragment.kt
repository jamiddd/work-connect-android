package com.jamid.workconnect.message

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jamid.workconnect.*
import com.jamid.workconnect.databinding.FragmentProjectGuidelinesBinding
import com.jamid.workconnect.model.ChatChannel
import com.jamid.workconnect.model.Post

class ProjectGuidelinesFragment : SupportFragment(R.layout.fragment_project_guidelines) {

    private lateinit var binding: FragmentProjectGuidelinesBinding
    private var positionFromBottom = 0
    private var saved = false
    private var dialog: AlertDialog? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentProjectGuidelinesBinding.bind(view)
        val chatChannel = arguments?.getParcelable<ChatChannel>(ARG_CHAT_CHANNEL) ?: return
        val post = arguments?.getParcelable<Post>(ARG_POST) ?: return

        binding.pgText.setText(post.guidelines)

        viewModel.guidelinesUpdateResult.observe(viewLifecycleOwner) {
            if (it != null) {
                dialog?.dismiss()
                findNavController().navigateUp()
                viewModel.clearGuidelinesUpdateResult()
            }
        }

        binding.projectGuidelinesFragmentToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        val currentUser = viewModel.user.value!!

        if (chatChannel.administrators.contains(currentUser.id)) {
            binding.pgText.isEnabled = true

            binding.saveGuidelinesButton.isEnabled = true

            binding.saveGuidelinesButton.setOnClickListener {
                val text = binding.pgText.text
                if (!text.isNullOrBlank()) {
                    saved = true

                    val linearLayout = LinearLayout(activity)
                    linearLayout.gravity = Gravity.CENTER_HORIZONTAL
                    linearLayout.orientation = LinearLayout.VERTICAL
                    val p = convertDpToPx(16)
                    linearLayout.setPadding(p)
                    val textView = TextView(activity)
                    textView.text = "Updating project guidelines ..."
                    textView.gravity = Gravity.CENTER_HORIZONTAL
                    val progressBar = ProgressBar(activity)
                    linearLayout.addView(progressBar)
                    linearLayout.addView(textView)

                    dialog = MaterialAlertDialogBuilder(activity)
                        .setView(linearLayout)
                        .show()

                    viewModel.updatePost(post, text.trimEnd().toString())
                }
            }


            val height = getWindowHeight()

            binding.editNotification.setBackgroundColor(ContextCompat.getColor(activity, R.color.blue_500))
            binding.editNotification.setTextColor(ContextCompat.getColor(activity, R.color.white))
            binding.editNotification.text =  "You are in edit mode. Only you and other admins can edit the guidelines."

            viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
                binding.projectGuidelinesFragmentToolbar.updateLayout(marginTop = top)

                binding.pgContentScroller.setPadding(0, 0, 0, bottom + convertDpToPx(8))

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
            binding.saveGuidelinesButton.isEnabled = false
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

            viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
                binding.projectGuidelinesFragmentToolbar.updateLayout(marginTop = top)
                binding.pgContentScroller.setPadding(0, 0, 0, bottom + convertDpToPx(8))
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