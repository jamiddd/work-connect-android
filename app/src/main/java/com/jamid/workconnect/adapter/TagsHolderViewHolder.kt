package com.jamid.workconnect.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.jamid.workconnect.R
import com.jamid.workconnect.interfaces.OnChipClickListener
import com.jamid.workconnect.model.TagsHolder

class TagsHolderViewHolder(parent: ViewGroup, @LayoutRes layout: Int): GenericViewHolder<TagsHolder>(parent, layout) {

    private val onChipClickListener = itemView.context as OnChipClickListener

    override fun bind(item: TagsHolder) {
        val holder = itemView.findViewById<ChipGroup>(R.id.tags_group)
        for (tag in item.tags) {
            addNewChip(tag, holder)
        }
    }

    private fun addNewChip(s: String, group: ChipGroup) {
        val chip = LayoutInflater.from(itemView.context).inflate(R.layout.chip, null) as Chip
        chip.text = s
        chip.isChipIconVisible = true
        chip.checkedIcon = ContextCompat.getDrawable(itemView.context, R.drawable.ic_baseline_done_24)

        chip.isChecked = false
        chip.setOnClickListener {
            chip.isChecked = false
            onChipClickListener.onChipClick(chip.text.toString())
//            onChipClickListener.onInterestSelect(chip.text.toString())
//            group.removeView(chip)
        }
        group.addView(chip)
    }

    companion object {

        @JvmStatic
        fun newInstance(parent: ViewGroup, @LayoutRes layout: Int): TagsHolderViewHolder {
            return TagsHolderViewHolder(parent, layout)
        }

    }
}