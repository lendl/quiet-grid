package com.quietgrid.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.GameId

@Composable
fun HowToPlayScreen(gameId: GameId) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        when (gameId) {
            GameId.CHIMPTEST -> ChimpTestHowToPlay()
            GameId.TAKUZU -> TakuzuHowToPlay()
            GameId.NONOGRAM -> NonogramHowToPlay()
            GameId.MINESWEEPER -> MinesweeperHowToPlay()
            GameId.SUDOKU -> SudokuHowToPlay()
            GameId.WORDSEARCH -> WordSearchHowToPlay()
        }

        HorizontalDivider(Modifier.padding(vertical = 20.dp))
        InGameIconsAccordion()
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun BodyText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun RuleRow(number: Int, titleRes: Int, bodyRes: Int) {
    Row(Modifier.padding(top = if (number == 1) 0.dp else 10.dp)) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(number.toString(), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelMedium)
        }
        Column(Modifier.padding(start = 12.dp)) {
            Text(stringResource(titleRes), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(bodyRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Accordion(title: String, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clickable { expanded = !expanded },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Icon(
            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (expanded) {
        Column(Modifier.padding(top = 12.dp)) { content() }
    }
}

@Composable
private fun TechniqueItem(titleRes: Int, bodyRes: Int) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(stringResource(titleRes), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(bodyRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InGameIconsAccordion() {
    Accordion(stringResource(R.string.how_to_play_in_game_icons_title)) {
        InGameIconRow(Icons.Outlined.Lightbulb, R.string.how_to_play_icon_hint_label, R.string.how_to_play_icon_hint_description)
        InGameIconRow(Icons.Outlined.Flag, R.string.how_to_play_icon_forfeit_label, R.string.how_to_play_icon_forfeit_description)
    }
}

@Composable
private fun InGameIconRow(icon: ImageVector, labelRes: Int, descriptionRes: Int) {
    Row(Modifier.padding(vertical = 7.dp)) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.padding(start = 12.dp)) {
            Text(stringResource(labelRes), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(descriptionRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChimpTestHowToPlay() {
    SectionHeader(Icons.Outlined.EmojiEvents, stringResource(R.string.how_to_play_goal_title))
    BodyText(stringResource(R.string.chimp_how_to_play_goal))

    HorizontalDivider(Modifier.padding(vertical = 20.dp))
    SectionHeader(Icons.Outlined.PanTool, stringResource(R.string.how_to_play_controls_title))
    BodyText(stringResource(R.string.chimp_how_to_play_controls))

    HorizontalDivider(Modifier.padding(vertical = 20.dp))
    SectionHeader(Icons.Outlined.WarningAmber, stringResource(R.string.how_to_play_wrong_move_title))
    BodyText(stringResource(R.string.chimp_how_to_play_wrong_move))

    HorizontalDivider(Modifier.padding(vertical = 20.dp))
    SectionHeader(Icons.Outlined.Description, stringResource(R.string.how_to_play_rules_title))
    RuleRow(1, R.string.chimp_rule_1_title, R.string.chimp_rule_1_body)
    RuleRow(2, R.string.chimp_rule_2_title, R.string.chimp_rule_2_body)
    RuleRow(3, R.string.chimp_rule_3_title, R.string.chimp_rule_3_body)
    RuleRow(4, R.string.chimp_rule_4_title, R.string.chimp_rule_4_body)

    HorizontalDivider(Modifier.padding(vertical = 20.dp))
    Accordion(stringResource(R.string.how_to_play_tips_title)) {
        TechniqueItem(R.string.chimp_tip_scan_title, R.string.chimp_tip_scan_body)
        TechniqueItem(R.string.chimp_tip_group_title, R.string.chimp_tip_group_body)
    }

    HorizontalDivider(Modifier.padding(vertical = 20.dp))
    Accordion(stringResource(R.string.how_to_play_scoring_title)) {
        BodyText(stringResource(R.string.chimp_how_to_play_scoring))
    }
}

@Composable
private fun TakuzuHowToPlay() {
    SectionHeader(Icons.Outlined.EmojiEvents, stringResource(R.string.how_to_play_goal_title))
    BodyText(stringResource(R.string.takuzu_how_to_play_goal))

    HorizontalDivider(Modifier.padding(vertical = 20.dp))
    SectionHeader(Icons.Outlined.PanTool, stringResource(R.string.how_to_play_controls_title))
    BodyText(stringResource(R.string.takuzu_how_to_play_controls))

    HorizontalDivider(Modifier.padding(vertical = 20.dp))
    SectionHeader(Icons.Outlined.WarningAmber, stringResource(R.string.how_to_play_wrong_move_title))
    BodyText(stringResource(R.string.takuzu_how_to_play_wrong_move))

    HorizontalDivider(Modifier.padding(vertical = 20.dp))
    SectionHeader(Icons.Outlined.Description, stringResource(R.string.how_to_play_rules_title))
    RuleRow(1, R.string.takuzu_rule_1_title, R.string.takuzu_rule_1_body)
    RuleRow(2, R.string.takuzu_rule_2_title, R.string.takuzu_rule_2_body)
    RuleRow(3, R.string.takuzu_rule_3_title, R.string.takuzu_rule_3_body)

    HorizontalDivider(Modifier.padding(vertical = 20.dp))
    Accordion(stringResource(R.string.how_to_play_techniques_title)) {
        TechniqueItem(R.string.takuzu_technique_1_title, R.string.takuzu_technique_1_body)
        TechniqueItem(R.string.takuzu_technique_2_title, R.string.takuzu_technique_2_body)
        TechniqueItem(R.string.takuzu_technique_3_title, R.string.takuzu_technique_3_body)
        TechniqueItem(R.string.takuzu_technique_4_title, R.string.takuzu_technique_4_body)
        TechniqueItem(R.string.takuzu_technique_5_title, R.string.takuzu_technique_5_body)
    }

    HorizontalDivider(Modifier.padding(vertical = 20.dp))
    Accordion(stringResource(R.string.how_to_play_tips_title)) {
        TechniqueItem(R.string.takuzu_tip_watch_flashes_title, R.string.takuzu_tip_watch_flashes_body)
    }

    HorizontalDivider(Modifier.padding(vertical = 20.dp))
    Accordion(stringResource(R.string.how_to_play_scoring_title)) {
        BodyText(stringResource(R.string.takuzu_how_to_play_scoring))
    }
}

@Composable
private fun NonogramHowToPlay() {
    SectionHeader(Icons.Outlined.EmojiEvents, stringResource(R.string.how_to_play_goal_title))
    BodyText(stringResource(R.string.nonogram_how_to_play_goal))

    HorizontalDivider(Modifier.padding(vertical = 20.dp))
    SectionHeader(Icons.Outlined.PanTool, stringResource(R.string.how_to_play_controls_title))
    BodyText(stringResource(R.string.nonogram_how_to_play_controls))

    HorizontalDivider(Modifier.padding(vertical = 20.dp))
    SectionHeader(Icons.Outlined.Description, stringResource(R.string.how_to_play_rules_title))
    RuleRow(1, R.string.nonogram_rule_1_title, R.string.nonogram_rule_1_body)
    RuleRow(2, R.string.nonogram_rule_2_title, R.string.nonogram_rule_2_body)
    RuleRow(3, R.string.nonogram_rule_3_title, R.string.nonogram_rule_3_body)
    RuleRow(4, R.string.nonogram_rule_4_title, R.string.nonogram_rule_4_body)

    HorizontalDivider(Modifier.padding(vertical = 20.dp))
    Accordion(stringResource(R.string.how_to_play_techniques_title)) {
        TechniqueItem(R.string.nonogram_technique_1_title, R.string.nonogram_technique_1_body)
        TechniqueItem(R.string.nonogram_technique_2_title, R.string.nonogram_technique_2_body)
        TechniqueItem(R.string.nonogram_technique_3_title, R.string.nonogram_technique_3_body)
    }

    HorizontalDivider(Modifier.padding(vertical = 20.dp))
    Accordion(stringResource(R.string.how_to_play_scoring_title)) {
        BodyText(stringResource(R.string.nonogram_how_to_play_scoring))
    }
}

@Composable
private fun MinesweeperHowToPlay() {
    SectionHeader(Icons.Outlined.EmojiEvents, stringResource(R.string.how_to_play_goal_title))
    BodyText(stringResource(R.string.minesweeper_how_to_play_goal))

    HorizontalDivider(Modifier.padding(vertical = 20.dp))
    SectionHeader(Icons.Outlined.PanTool, stringResource(R.string.how_to_play_controls_title))
    BodyText(stringResource(R.string.minesweeper_how_to_play_controls))

    HorizontalDivider(Modifier.padding(vertical = 20.dp))
    SectionHeader(Icons.Outlined.WarningAmber, stringResource(R.string.how_to_play_wrong_move_title))
    BodyText(stringResource(R.string.minesweeper_how_to_play_wrong_move))

    HorizontalDivider(Modifier.padding(vertical = 20.dp))
    SectionHeader(Icons.Outlined.Description, stringResource(R.string.how_to_play_rules_title))
    RuleRow(1, R.string.minesweeper_rule_1_title, R.string.minesweeper_rule_1_body)
    RuleRow(2, R.string.minesweeper_rule_2_title, R.string.minesweeper_rule_2_body)
    RuleRow(3, R.string.minesweeper_rule_3_title, R.string.minesweeper_rule_3_body)
    RuleRow(4, R.string.minesweeper_rule_4_title, R.string.minesweeper_rule_4_body)

    HorizontalDivider(Modifier.padding(vertical = 20.dp))
    Accordion(stringResource(R.string.how_to_play_techniques_title)) {
        TechniqueItem(R.string.minesweeper_technique_1_title, R.string.minesweeper_technique_1_body)
        TechniqueItem(R.string.minesweeper_technique_2_title, R.string.minesweeper_technique_2_body)
        TechniqueItem(R.string.minesweeper_technique_3_title, R.string.minesweeper_technique_3_body)
    }

    HorizontalDivider(Modifier.padding(vertical = 20.dp))
    Accordion(stringResource(R.string.how_to_play_tips_title)) {
        TechniqueItem(R.string.minesweeper_tip_1_title, R.string.minesweeper_tip_1_body)
        TechniqueItem(R.string.minesweeper_tip_2_title, R.string.minesweeper_tip_2_body)
        TechniqueItem(R.string.minesweeper_tip_3_title, R.string.minesweeper_tip_3_body)
    }

    HorizontalDivider(Modifier.padding(vertical = 20.dp))
    Accordion(stringResource(R.string.how_to_play_scoring_title)) {
        BodyText(stringResource(R.string.minesweeper_how_to_play_scoring))
    }
}

@Composable
private fun SudokuHowToPlay() {
    SectionHeader(Icons.Outlined.EmojiEvents, stringResource(R.string.how_to_play_goal_title))
    BodyText(stringResource(R.string.sudoku_how_to_play_goal))

    HorizontalDivider(Modifier.padding(vertical = 20.dp))
    SectionHeader(Icons.Outlined.PanTool, stringResource(R.string.how_to_play_controls_title))
    BodyText(stringResource(R.string.sudoku_how_to_play_controls))

    HorizontalDivider(Modifier.padding(vertical = 20.dp))
    SectionHeader(Icons.Outlined.WarningAmber, stringResource(R.string.how_to_play_wrong_move_title))
    BodyText(stringResource(R.string.sudoku_how_to_play_wrong_move))

    HorizontalDivider(Modifier.padding(vertical = 20.dp))
    SectionHeader(Icons.Outlined.Description, stringResource(R.string.how_to_play_rules_title))
    RuleRow(1, R.string.sudoku_rule_1_title, R.string.sudoku_rule_1_body)
    RuleRow(2, R.string.sudoku_rule_2_title, R.string.sudoku_rule_2_body)
    RuleRow(3, R.string.sudoku_rule_3_title, R.string.sudoku_rule_3_body)

    HorizontalDivider(Modifier.padding(vertical = 20.dp))
    Accordion(stringResource(R.string.how_to_play_techniques_title)) {
        TechniqueItem(R.string.sudoku_technique_1_title, R.string.sudoku_technique_1_body)
        TechniqueItem(R.string.sudoku_technique_2_title, R.string.sudoku_technique_2_body)
        TechniqueItem(R.string.sudoku_technique_3_title, R.string.sudoku_technique_3_body)
    }

    HorizontalDivider(Modifier.padding(vertical = 20.dp))
    Accordion(stringResource(R.string.how_to_play_tips_title)) {
        TechniqueItem(R.string.sudoku_tip_1_title, R.string.sudoku_tip_1_body)
        TechniqueItem(R.string.sudoku_tip_2_title, R.string.sudoku_tip_2_body)
    }

    HorizontalDivider(Modifier.padding(vertical = 20.dp))
    Accordion(stringResource(R.string.how_to_play_scoring_title)) {
        BodyText(stringResource(R.string.sudoku_how_to_play_scoring))
    }
}

@Composable
private fun WordSearchHowToPlay() {
    SectionHeader(Icons.Outlined.EmojiEvents, stringResource(R.string.how_to_play_goal_title))
    BodyText(stringResource(R.string.wordsearch_how_to_play_goal))

    HorizontalDivider(Modifier.padding(vertical = 20.dp))
    SectionHeader(Icons.Outlined.PanTool, stringResource(R.string.how_to_play_controls_title))
    BodyText(stringResource(R.string.wordsearch_how_to_play_controls))

    HorizontalDivider(Modifier.padding(vertical = 20.dp))
    SectionHeader(Icons.Outlined.WarningAmber, stringResource(R.string.how_to_play_wrong_move_title))
    BodyText(stringResource(R.string.wordsearch_how_to_play_wrong_move))

    HorizontalDivider(Modifier.padding(vertical = 20.dp))
    SectionHeader(Icons.Outlined.Description, stringResource(R.string.how_to_play_rules_title))
    RuleRow(1, R.string.wordsearch_rule_1_title, R.string.wordsearch_rule_1_body)
    RuleRow(2, R.string.wordsearch_rule_2_title, R.string.wordsearch_rule_2_body)
    RuleRow(3, R.string.wordsearch_rule_3_title, R.string.wordsearch_rule_3_body)

    HorizontalDivider(Modifier.padding(vertical = 20.dp))
    Accordion(stringResource(R.string.how_to_play_tips_title)) {
        TechniqueItem(R.string.wordsearch_tip_1_title, R.string.wordsearch_tip_1_body)
        TechniqueItem(R.string.wordsearch_tip_2_title, R.string.wordsearch_tip_2_body)
    }
}
