package com.dfinn.wallet.common.list.headers

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.dfinn.wallet.common.R
import com.dfinn.wallet.common.list.GroupedListHolder
import com.dfinn.wallet.common.utils.inflateChild
import kotlinx.android.synthetic.main.item_text_header.view.textHeader

class TextHeader(val content: String) {

    companion object {

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<TextHeader>() {

            override fun areItemsTheSame(oldItem: TextHeader, newItem: TextHeader): Boolean {
                return oldItem.content == newItem.content
            }

            override fun areContentsTheSame(oldItem: TextHeader, newItem: TextHeader): Boolean {
                return true
            }
        }
    }
}

class TextHeaderHolder(parentView: ViewGroup) : GroupedListHolder(parentView.inflateChild(R.layout.item_text_header)) {

    fun bind(item: TextHeader) {
        containerView.textHeader.text = item.content
    }
}
