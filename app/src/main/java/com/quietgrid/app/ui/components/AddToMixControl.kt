package com.quietgrid.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.mix.Mix
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToMixControl(eligibleMixes: List<Mix>, onAddToMix: (Mix) -> Unit, modifier: Modifier = Modifier) {
    var confirmedMixName by remember { mutableStateOf<String?>(null) }
    var showPicker by remember { mutableStateOf(false) }

    LaunchedEffect(confirmedMixName) {
        if (confirmedMixName != null) {
            delay(1500)
            confirmedMixName = null
        }
    }

    fun add(mix: Mix) {
        onAddToMix(mix)
        confirmedMixName = mix.name
        showPicker = false
    }

    val confirmation = confirmedMixName
    if (confirmation == null && eligibleMixes.isEmpty()) return

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(1.dp).height(16.dp).background(MaterialTheme.colorScheme.outlineVariant))
        if (confirmation != null) {
            Text(
                stringResource(R.string.mix_added_confirmation, confirmation),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp),
            )
        } else {
            TextButton(onClick = { if (eligibleMixes.size == 1) add(eligibleMixes.single()) else showPicker = true }) {
                Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text(stringResource(R.string.mix_add_to_mix), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showPicker) {
        ModalBottomSheet(onDismissRequest = { showPicker = false }) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
                Text(stringResource(R.string.mix_add_to_mix_sheet_title), style = MaterialTheme.typography.titleMedium)
                eligibleMixes.forEach { mix ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { add(mix) }
                            .padding(vertical = 14.dp),
                    ) {
                        Text(mix.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
