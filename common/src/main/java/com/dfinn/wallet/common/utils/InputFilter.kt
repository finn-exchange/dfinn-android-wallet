package com.dfinn.wallet.common.utils

import android.text.InputFilter

class ByteSizeFilter(maxSizeInBytes: Int) : InputFilter.LengthFilter(maxSizeInBytes / Char.SIZE_BYTES)

private const val NAME_BYTE_LIMIT = 32

fun nameInputFilters() = arrayOf(ByteSizeFilter(NAME_BYTE_LIMIT))
