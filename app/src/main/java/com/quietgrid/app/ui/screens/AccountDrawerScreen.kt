package com.quietgrid.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R

private val SETTINGS_ICON_COLOR = Color(0xFFA78BFA)
private val SUPPORT_MENU_ICON_COLOR = Color(0xFF60A5FA)
private val TRUST_MENU_ICON_COLOR = Color(0xFF34D399)
private val ABOUT_MENU_ICON_COLOR = Color(0xFFF472B6)

@Composable
fun AccountDrawerContent(
    onOpenSettings: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenTrust: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        SupportRow(Icons.Filled.Settings, SETTINGS_ICON_COLOR, stringResource(R.string.account_preferences_section), "") { onOpenSettings() }
        SupportRow(Icons.Filled.SupportAgent, SUPPORT_MENU_ICON_COLOR, stringResource(R.string.support_support_section), "") { onOpenSupport() }
        SupportRow(Icons.Filled.VerifiedUser, TRUST_MENU_ICON_COLOR, stringResource(R.string.support_trust_section), "") { onOpenTrust() }
        SupportRow(Icons.Filled.Info, ABOUT_MENU_ICON_COLOR, stringResource(R.string.support_about_section), "") { onOpenAbout() }

        HorizontalDivider(Modifier.padding(vertical = 16.dp))

        ShowYourSupportSection()
    }
}
