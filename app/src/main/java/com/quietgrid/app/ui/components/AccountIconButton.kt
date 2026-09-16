package com.quietgrid.app.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.quietgrid.app.R

@Composable
fun AccountIconButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    IconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier.pressScale(interactionSource),
    ) {
        Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.account_menu_content_description))
    }
}
