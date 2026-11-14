package com.quietgrid.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.badges.BadgeCatalog
import com.quietgrid.app.ui.components.BadgeHex
import com.quietgrid.app.ui.components.badgeText

@Composable
internal fun NewBadgeLine(badgeId: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val def = BadgeCatalog.byId(badgeId) ?: return
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 44.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BadgeHex(def, earned = true, size = 30.dp)
        Text(
            stringResource(R.string.badge_new_line, stringResource(badgeText(def).nameRes)),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
