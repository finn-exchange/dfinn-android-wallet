package com.dfinn.wallet.common.view.bottomSheet.list.dynamic

import android.content.Context
import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.DiffUtil
import com.dfinn.wallet.common.R
import com.dfinn.wallet.common.utils.setVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.bottom_sheet_dynamic_list.*

typealias ClickHandler<T> = (T) -> Unit

class ReferentialEqualityDiffCallBack<T> : DiffUtil.ItemCallback<T>() {

    override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
        return oldItem === newItem
    }

    override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
        return true
    }
}

abstract class DynamicListBottomSheet<T>(
    context: Context,
    private val payload: Payload<T>,
    private val diffCallback: DiffUtil.ItemCallback<T>,
    private val onClicked: ClickHandler<T>,
    private val onCancel: (() -> Unit)? = null,
) : BottomSheetDialog(context, R.style.BottomSheetDialog), DynamicListSheetAdapter.Handler<T> {

    class Payload<T>(val data: List<T>, val selected: T? = null)

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.bottom_sheet_dynamic_list)
        super.onCreate(savedInstanceState)

        dynamicListSheetContent.setHasFixedSize(true)

        val adapter = DynamicListSheetAdapter(payload.selected, this, diffCallback, holderCreator())
        dynamicListSheetContent.adapter = adapter

        adapter.submitList(payload.data)

        setOnCancelListener { onCancel?.invoke() }
    }

    abstract fun holderCreator(): HolderCreator<T>

    override fun setTitle(title: CharSequence?) {
        dynamicListSheetTitle.text = title
    }

    override fun setTitle(titleId: Int) {
        dynamicListSheetTitle.setText(titleId)
    }

    fun setDividerVisible(visible: Boolean) {
        dynamicListSheetDivider.setVisible(visible)
    }

    override fun itemClicked(item: T) {
        onClicked(item)

        dismiss()
    }
}
