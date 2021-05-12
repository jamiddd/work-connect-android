package com.jamid.workconnect.auth

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import com.jamid.workconnect.CREATING_USER
import com.jamid.workconnect.GenericDialogFragment
import com.jamid.workconnect.R
import com.jamid.workconnect.SupportFragment
import com.jamid.workconnect.databinding.FragmentInterestBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class InterestFragment : SupportFragment(R.layout.fragment_interest, TAG, false) {

    private lateinit var binding: FragmentInterestBinding
    private val checkedCount = MutableLiveData<Int>().apply { value = 0 }
    private var initialized = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentInterestBinding.bind(view)
        setInsetView(binding.interestScroller, mapOf(insetTop to 56))
        binding.addTagBtn.isEnabled = false

        viewModel.user.observe(viewLifecycleOwner) {
            if (it != null) {
                initialized = true
                activity.hideBottomSheet()
            }
        }

        viewModel.primaryBottomSheetState.observe(viewLifecycleOwner) {
            if (initialized) {
                if (it == BottomSheetBehavior.STATE_HIDDEN) {
                    if (viewModel.fragmentTagStack.contains(SignInFragment.TAG)) {
                        for (i in 0..2) {
                            viewModel.fragmentTagStack.pop()
                        }
                        viewModel.setCurrentFragmentTag(viewModel.fragmentTagStack.peek())
                        activity.supportFragmentManager.popBackStack(SignInFragment.TAG, 1)
                    } else {
                        for (i in 0..1) {
                            viewModel.fragmentTagStack.pop()
                        }
                        viewModel.setCurrentFragmentTag(viewModel.fragmentTagStack.peek())
                        activity.supportFragmentManager.popBackStack(UserDetailFragment.TAG, 1)
                    }
                }
            }
        }

        checkedCount.observe(viewLifecycleOwner) {
            binding.totalSelectedText.text = "$it Selected"
            activity.mainBinding.primaryMenuBtn.isEnabled =  it > 2
        }

        binding.addTagBtn.setOnClickListener {
            val tag = binding.customInterestText.text.toString()
            checkedCount.postValue(checkedCount.value!! + 1)
            addChip(tag, binding.interestsList.childCount, activity, false)
            binding.customInterestText.text.clear()
        }

        binding.customInterestText.doAfterTextChanged {
            binding.addTagBtn.isEnabled = !it.isNullOrBlank()
        }

        lifecycleScope.launch {
            delay(1000)

            activity.mainBinding.primaryMenuBtn.setOnClickListener {
                createUser()
            }
        }

        binding.customInterestText.setOnEditorActionListener { v, actionId, event ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    val t = binding.customInterestText.text
                    if (!t.isNullOrBlank()) {
                        addChip(t.toString(), binding.interestsList.childCount, activity, false)
                        checkedCount.postValue(checkedCount.value!! + 1)
                        binding.customInterestText.text.clear()
                    }
                }
            }
            true
        }


        // TODO("make a collection of tags with rankings")
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
        someRandomInterests.forEachIndexed { index, s ->
            addChip(s, index, activity, true)
        }

        binding.skipBtn.setOnClickListener {
            createUser()
        }

        OverScrollDecoratorHelper.setUpOverScroll(binding.interestScroller)

    }

    private fun createUser() {
        val interests = mutableListOf<String>()

        for (child in binding.interestsList.children) {
            val chip = child as Chip
            if (chip.isChecked) {
                interests.add(chip.text.toString())
            }
        }

        activity.showBottomSheet(
            GenericDialogFragment.newInstance(
                CREATING_USER,
                "Creating your account",
                "Creating your account. Please wait for a while ... ",
                isProgressing = true
            ), GenericDialogFragment.TAG)

        viewModel.uploadUser(interests)
    }

    private fun addChip(interest: String, index: Int, context: Activity, initial: Boolean = true) {
        val chip = LayoutInflater.from(context).inflate(R.layout.chip_filter, null) as Chip
        chip.text = interest
        chip.id = index
        chip.isCloseIconVisible = true
        chip.isCheckedIconVisible = true
        chip.closeIcon = ContextCompat.getDrawable(context, R.drawable.ic_baseline_close_24)
        chip.isCheckable = true

        chip.isChecked = !initial

        chip.setOnCloseIconClickListener {
            binding.interestsList.removeView(chip)
        }

        chip.setOnClickListener {
            checkedCount.postValue(binding.interestsList.checkedChipIds.size)
        }

        binding.interestsList.addView(chip)
    }

    companion object {

        const val TAG = "InterestFragment"
        const val TITLE = ""

        @JvmStatic
        fun newInstance() = InterestFragment()
    }
}