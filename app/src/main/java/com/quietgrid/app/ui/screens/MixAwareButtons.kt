package com.quietgrid.app.ui.screens

import com.quietgrid.app.R

fun mixAwarePrimaryLabel(isMixActive: Boolean, defaultLabelRes: Int): Int =
    if (isMixActive) R.string.mix_next_puzzle else defaultLabelRes
