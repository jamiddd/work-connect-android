package com.jamid.workconnect.auth

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ScrollView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jamid.workconnect.MainActivity
import com.jamid.workconnect.MainViewModel
import com.jamid.workconnect.R
import com.jamid.workconnect.databinding.FragmentInterestBinding
import com.jamid.workconnect.getWindowHeight
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class InterestFragment : Fragment(R.layout.fragment_interest) {

    private lateinit var binding: FragmentInterestBinding
    private val viewModel: MainViewModel by activityViewModels()
    private val checkChangeListener = MutableLiveData<List<Int>>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentInterestBinding.bind(view)
        var dialog: DialogInterface? = null
        var job: Job? = null

        val activity = requireActivity() as MainActivity
        val primaryBtn = activity.findViewById<Button>(R.id.primaryBtn)

        binding.addTagBtn.isEnabled = false

        viewModel.user.observe(viewLifecycleOwner) {
            if (it != null) {
                dialog?.dismiss()
                job?.cancel()
                activity.bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
        }

        checkChangeListener.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                binding.totalSelectedText.text = "${it.size} Selected"
                primaryBtn.isEnabled = it.size > 2
            } else {
                primaryBtn.isEnabled = false
                binding.totalSelectedText.text = "0 Selected"
            }
        }

        binding.addTagBtn.setOnClickListener {
            val tag = binding.customInterestText.text.toString()
            addChip(tag, requireActivity(), false)
            binding.customInterestText.text.clear()
        }

        binding.customInterestText.doAfterTextChanged {
            binding.addTagBtn.isEnabled = !it.isNullOrBlank()
        }

        primaryBtn.setOnClickListener {
            val interests = mutableListOf<String>()

            for (child in binding.interestsList.children) {
                val chip = child as Chip
                if (chip.isChecked) {
                    interests.add(chip.text.toString())
                }
            }
            /*
            for (ch in binding.interestsList.checkedChipIds) {
                val chip = binding.interestsList.getChildAt(ch) as Chip

            }*/
            dialog = MaterialAlertDialogBuilder(activity)
                .setView(R.layout.creating_user_progress_dialog)
                .setCancelable(false)
                .show()

            viewModel.createNewUser(interests)

            job = lifecycleScope.launch {
                delay(15000)
                dialog?.dismiss()

                Toast.makeText(activity, "Some unknown error occurred", Toast.LENGTH_SHORT).show()
            }
        }

        binding.customInterestText.setOnEditorActionListener { v, actionId, event ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    val t = binding.customInterestText.text
                    if (!t.isNullOrBlank()) {
                        addChip(t.toString(), activity, false)
                        binding.customInterestText.text.clear()
                    }
                }
            }
            true
        }


        val someRandomInterests = arrayOf(
            "Google",
            "Android",
            "Material Design",
            "Design",
            "React",
            "iOS",
            "Swift",
            "Web Development",
            "Objective-C"
        )

        for (interest in someRandomInterests) {
            addChip(interest, activity, true)
        }

        binding.skipBtn.setOnClickListener {
            dialog = MaterialAlertDialogBuilder(activity)
                .setView(R.layout.creating_user_progress_dialog)
                .setCancelable(false)
                .show()

            viewModel.createNewUser(emptyList())

            job = lifecycleScope.launch {
                delay(15000)
                dialog?.dismiss()

                Toast.makeText(activity, "Some unknown error occurred", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            val windowHeight = getWindowHeight()
//            val rect = windowHeight - top

            val params = binding.root.layoutParams as ViewGroup.LayoutParams
            params.height = windowHeight
            params.width = ViewGroup.LayoutParams.MATCH_PARENT

            binding.root.layoutParams = params

        }

        OverScrollDecoratorHelper.setUpOverScroll(binding.root as ScrollView)

    }

    private fun addChip(interest: String, context: Activity, initial: Boolean = true) {
        val chip = Chip(context)
        chip.text = interest
        chip.isCloseIconVisible = true
        chip.isCheckedIconVisible = true
        chip.closeIcon = ContextCompat.getDrawable(context, R.drawable.ic_baseline_close_24)
        chip.isCheckable = true

        if (initial) {
            chip.isChecked = false
            chip.toSecondary(context)
        } else {
            chip.isChecked = true
            chip.toPrimary(context)
        }

        chip.setOnCloseIconClickListener {
            binding.interestsList.removeView(chip)
        }

        chip.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                chip.toPrimary(context)
            } else {
                chip.toSecondary(context)
            }
            checkChangeListener.postValue(binding.interestsList.checkedChipIds)
        }

        binding.interestsList.addView(chip)
    }

    private fun Chip.toPrimary(context: Context) {
        closeIconTint = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))
        chipBackgroundColor =
            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.blue_500))
        setTextColor(ContextCompat.getColor(context, R.color.white))
    }

    private fun Chip.toSecondary(context: Context) {
        closeIconTint = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.black))
        chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#e3e3e3"))
        setTextColor(ContextCompat.getColor(context, R.color.black))
    }

    companion object {

        @JvmStatic
        fun newInstance() = InterestFragment()
    }
}