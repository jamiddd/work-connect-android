package com.jamid.workconnect.auth

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.FIND_VIEWS_WITH_TEXT
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.*
import com.jamid.workconnect.adapter.paging2.GenericComparator
import com.jamid.workconnect.databinding.FragmentInterestBinding
import com.jamid.workconnect.interfaces.OnChipClickListener
import com.jamid.workconnect.model.InterestItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class InterestFragment : SupportFragment(R.layout.fragment_interest) {

    // TODO("While selecting any interest, also increase searchRank")

    private lateinit var binding: FragmentInterestBinding
    private val checkedCount = MutableLiveData<Int>().apply { value = 0 }
    private var initialized = false
    private lateinit var onChipClickListener: OnChipClickListener
    private var job: Job? = null
    private var searchAdapter: SearchAdapter? = null

    private fun search(query: String) {
        job?.cancel()
        job = viewLifecycleOwner.lifecycleScope.launch {
            val task = Firebase.firestore.collection("interests")
                .whereArrayContainsAny("indices", listOf(
                    query,
                    query.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                    query.replaceFirstChar { it.lowercase(Locale.ROOT) },
                    query.uppercase(Locale.ROOT), query.lowercase(Locale.ROOT)
                ))
                .orderBy("interest")
                .limit(5)
                .get()
            val result = task.await()
            val interests = result.toObjects(InterestItem::class.java)

            searchAdapter?.submitList(interests)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentInterestBinding.bind(view)
        onChipClickListener = activity

        val isSignInFragment = arguments?.getBoolean(ARG_IS_SIGN_IN) ?: false

        binding.interestToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        activity.mainBinding.apply {
            bottomNavBackground.visibility = View.GONE
            if (isSignInFragment) {
                bottomCard.hide()
            } else {
                binding.skipInterestBtn.visibility = View.INVISIBLE
                binding.signInLayout.visibility = View.GONE
                bottomCard.show()
            }
        }

        binding.interestsSearchResultRecycler.layoutManager = LinearLayoutManager(activity)

        binding.interestEditText.doAfterTextChanged { s ->
            if (s.isNullOrBlank()) {
                binding.searchResultCardView.visibility = View.GONE
            } else {
                binding.customInterestText.text = s
                binding.searchResultCardView.visibility = View.VISIBLE
                val query = s.toString()
                search(query)
            }
        }

        binding.customInterestText.setOnClickListener {
            val customInterest = binding.interestEditText.text.toString()
            addNewChip(customInterest, binding.interestsList, isChecked = true)
            binding.interestEditText.text?.clear()
        }

        binding.interestEditText.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                binding.interestAppBar.setExpanded(false, true)
            }
        }

        binding.interestEditText.setOnClickListener {
            binding.interestAppBar.setExpanded(false, true)
        }


//        binding.addTagBtn.isEnabled = false

        viewModel.user.observe(viewLifecycleOwner) {
            if (it != null) {
                initialized = true
                activity.hideBottomSheet()
            }
        }

        viewModel.primaryBottomSheetState.observe(viewLifecycleOwner) {
            if (initialized) {
                if (it == BottomSheetBehavior.STATE_HIDDEN) {

                    if (findNavController().findDestination(R.id.signInFragment) != null) {
                        findNavController().popBackStack(R.id.signInFragment, true)
                    } else {
                        findNavController().popBackStack(R.id.userDetailFragment, true)
                    }

                    /*if (viewModel.fragmentTagStack.contains(SignInFragment.TAG)) {
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
                    }*/
                }
            }
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, _) ->
            binding.interestToolbar.updateLayout(marginTop = top)
        }

        /*binding.addTagBtn.setOnClickListener {
            val tag = binding.customInterestText.text.toString()
            checkedCount.postValue(checkedCount.value!! + 1)
            addChip(tag, binding.interestsList.childCount, activity, false)
            binding.customInterestText.text.clear()
        }
*/
        /*binding.customInterestText.doAfterTextChanged {
            binding.addTagBtn.isEnabled = !it.isNullOrBlank()
        }*/
        /*binding.customInterestText.setOnEditorActionListener { v, actionId, event ->
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
        }*/

        // TODO("make a collection of tags with rankings")
        val someRandomInterests = mutableListOf(
            "Artificial Intelligence",
            "Science",
            "Android",
            "Vector",
            "Computer Science",
            "Machine Learning",
            "Google",
            "Projects",
            "Physics",
            "Chemistry",
            "iOS Development",
            "Github",
            "Culinary",
            "Fashion Technology",
            "Neural Network",
            "Deep Learning",
            "Cryptocurrency",
            "Blockchain",
            "Artificial Intelligence",
            "Science",
            "Android",
            "Vector",
            "Computer Science",
            "Machine Learning",
            "Google",
            "Projects",
            "Physics",
            "Chemistry",
            "iOS Development",
            "Github",
            "Culinary",
            "Fashion Technology",
            "Neural Network",
            "Deep Learning",
            "Cryptocurrency",
            "Blockchain",
            "Artificial Intelligence",
            "Science",
            "Android",
            "Vector",
            "Computer Science",
            "Machine Learning",
            "Google",
            "Projects",
            "Physics",
            "Chemistry",
            "iOS Development",
            "Github",
            "Culinary",
            "Fashion Technology",
            "Neural Network",
            "Deep Learning",
            "Cryptocurrency",
            "Blockchain"
        )

        val currentUser = viewModel.user.value
        if (currentUser != null) {
            val interests = currentUser.userPrivate.interests
            if (Build.VERSION.SDK_INT <= 23) {
                for (int in interests) {
                    if (someRandomInterests.contains(int)) {
                        someRandomInterests.remove(int)
                    }
                }
            } else {
                for (int in interests) {
                    someRandomInterests.removeIf {
                        int == it
                    }
                }
            }
        }

        val shownList = someRandomInterests./*distinct().*/shuffled()
        shownList.forEachIndexed { index, s ->
            addNewChip(s, binding.interestsList)
        }

        searchAdapter = SearchAdapter(shownList)
        binding.interestsSearchResultRecycler.apply {
            adapter = searchAdapter
            layoutManager = LinearLayoutManager(activity)
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (_, bottom) ->
            binding.signInCompleteBtn.updateLayout(marginLeft = convertDpToPx(32), marginRight = convertDpToPx(32), marginBottom = bottom)
            binding.interestScroller.setPadding(0, 0, 0, bottom + convertDpToPx(128))
        }

        /*binding.interestScroller.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (scrollY > binding.interestsFragmentHeader.measuredHeight - convertDpToPx(32)) {
                activity.setFragmentTitle("Add Interests")
            } else {
                activity.setFragmentTitle("")
            }
        }*/

        binding.signInCompleteBtn.setOnClickListener {
            /*if (viewModel.fragmentTagStack.contains(SignInFragment.TAG)) {
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
            }*/
            createUser()
        }

        binding.skipInterestBtn.setOnClickListener {
            createUser()
        }

//        binding.interestsDoneBtn.updateLayout(marginBottom = viewModel.windowInsets.value!!.second)

    }

    private inner class SearchAdapter(val existingList: List<String>): ListAdapter<InterestItem, SearchAdapter.SearchViewHolder>(GenericComparator(InterestItem::class.java)) {

        private inner class SearchViewHolder(view: View): RecyclerView.ViewHolder(view) {

            private val itemText: TextView = view.findViewById(R.id.search_menu_text)

            fun bind(item: InterestItem) {
                itemText.text = item.interest

                itemText.setOnClickListener {
                    if (!existingList.contains(item.interest)) {
                        // make sure that the interest from the search is not an interest already in users interests
                        addNewChip(item.interest, binding.interestsList, true)
//                        onChipClickListener.onInterestSelect(item.interest)
                    } else {
                        val vs = arrayListOf<View>()
                        binding.interestsList.findViewsWithText(vs, item.interest, FIND_VIEWS_WITH_TEXT)
                        if (vs.isNotEmpty()) {
                            for (v in vs) {
                                (v as Chip).isChecked = true
                            }
//                            onChipClickListener.onInterestSelect(item.interest)
                        }
                    }
                    binding.interestEditText.text?.clear()
                }

            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
            return SearchViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.menu_item, parent, false))
        }

        override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

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

    private fun addNewChip(s: String, group: ChipGroup, isChecked: Boolean = false) {
        val chip = LayoutInflater.from(activity).inflate(R.layout.chip, null) as Chip

        chip.text = s
        chip.isChecked = isChecked

        chip.setOnClickListener {
            if (chip.isChecked) {
                onChipClickListener.onInterestSelect(chip.text.toString())
            } else {
                onChipClickListener.onInterestRemoved(chip.text.toString())
            }
        }
        group.addView(chip, 0)
    }

    companion object {

        const val TAG = "InterestFragment"
        const val TITLE = ""
        const val ARG_IS_SIGN_IN = "ARG_IS_SIGN_IN"

        @JvmStatic
        fun newInstance(isSignInFragment: Boolean = false) = InterestFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_IS_SIGN_IN, isSignInFragment)
            }
        }
    }
}