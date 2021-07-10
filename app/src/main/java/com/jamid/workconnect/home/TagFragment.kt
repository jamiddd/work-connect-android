package com.jamid.workconnect.home

import android.content.Context.MODE_PRIVATE
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jamid.workconnect.*
import com.jamid.workconnect.databinding.FragmentTagBinding


class TagFragment : Fragment(R.layout.fragment_tag) {

    private lateinit var binding: FragmentTagBinding
    private val viewModel: MainViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity() as MainActivity

        binding = FragmentTagBinding.bind(view)

        if (Build.VERSION.SDK_INT <= 27) {
            binding.tagLayoutRoot.setBackgroundColor(Color.WHITE)
        }

        binding.addTagBtn.isEnabled = false

        binding.tagEditText.doAfterTextChanged {
            binding.addTagBtn.isEnabled = !it.isNullOrBlank()
        }

        binding.addTagBtn.setOnClickListener {
            val tag = binding.tagEditText.text.toString().removeHashesInTheBeginning()
            viewModel.setTag(tag.trim())
            activity.bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }


        val sharedPref = activity.getSharedPreferences(WORK_CONNECT_SHARED_PREF, MODE_PRIVATE)
        if (sharedPref.getBoolean(HAS_SEEN_TIP, false)) {
            binding.addTagInfo.visibility = View.GONE
        } else {
            binding.addTagInfo.visibility = View.VISIBLE
            val editor = sharedPref.edit()
            editor.putBoolean(HAS_SEEN_TIP, true)
            editor.apply()
        }

        val minLength = convertDpToPx(8)

        val text = binding.addTagInfo.text
        val sp = SpannableString(text)
        val start = text.length - 7
        val end = text.length

        val clickableSpan = object: ClickableSpan() {
            override fun onClick(widget: View) {
                Toast.makeText(activity, "Clicked Something", Toast.LENGTH_SHORT).show()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
            }
        }

        sp.setSpan(ForegroundColorSpan(Color.BLUE), start, end, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        sp.setSpan(StyleSpan(Typeface.BOLD), start, end, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        sp.setSpan(clickableSpan, start, end, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.addTagInfo.text = sp

        binding.tagLayoutRoot.setOnClickListener {
            Log.d(TAG, "Clicked on the root")
        }

        binding.addTagInfo.setOnClickListener {
            binding.addTagInfo.visibility = View.GONE
        }


        viewModel.windowInsets.observe(viewLifecycleOwner) {(_, bottom) ->
            val params = binding.addTagBtn.layoutParams as ConstraintLayout.LayoutParams
            params.startToStart = binding.tagEditTextLayout.id
            params.endToEnd = binding.tagEditTextLayout.id
            params.bottomToBottom = binding.tagLayoutRoot.id
            params.topToBottom = binding.addTagInfo.id
            params.horizontalBias = 0.5f
            params.verticalChainStyle = ConstraintLayout.LayoutParams.CHAIN_PACKED
            params.verticalBias = 0.5f
            params.setMargins(minLength, minLength, minLength, bottom + minLength)
            binding.addTagBtn.layoutParams = params

            binding.tagEditText.requestFocus()
        }

    }

    companion object {

        const val TAG = "TagFragment"
        private const val HAS_SEEN_TIP = "HAS_SEEN_TIP"

        @JvmStatic
        fun newInstance() = TagFragment()
    }

    private fun String.removeHashesInTheBeginning(): String {
        var temp = this

        while (temp.first() == '#') {
            temp = temp.substring(1, temp.length)
        }

        return temp
    }
}
