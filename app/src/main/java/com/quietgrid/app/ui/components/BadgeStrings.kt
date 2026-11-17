package com.quietgrid.app.ui.components

import androidx.annotation.StringRes
import com.quietgrid.app.R
import com.quietgrid.app.core.badges.BadgeDef
import com.quietgrid.app.core.badges.BadgeIcon
import com.quietgrid.app.core.badges.JourneyStage

class BadgeText(@param:StringRes val nameRes: Int, @param:StringRes val descRes: Int)

private val JOURNEY_TEXT: Map<JourneyStage, BadgeText> = mapOf(
    JourneyStage.CHAPTER_ONE to BadgeText(R.string.badge_chapter_one_name, R.string.badge_chapter_one_desc),
    JourneyStage.JOURNEY_END to BadgeText(R.string.badge_journey_end_name, R.string.badge_journey_end_desc),
    JourneyStage.EVERY_STAR to BadgeText(R.string.badge_every_star_name, R.string.badge_every_star_desc),
)

private val BADGE_TEXT: Map<String, BadgeText> = mapOf(
    "full_tour" to BadgeText(R.string.badge_full_tour_name, R.string.badge_full_tour_desc),
    "expert_of_many" to BadgeText(R.string.badge_expert_of_many_name, R.string.badge_expert_of_many_desc),
    "mixed_bag" to BadgeText(R.string.badge_mixed_bag_name, R.string.badge_mixed_bag_desc),
    "all_journeys" to BadgeText(R.string.badge_all_journeys_name, R.string.badge_all_journeys_desc),
    "starry_road" to BadgeText(R.string.badge_starry_road_name, R.string.badge_starry_road_desc),
    "slow_and_steady" to BadgeText(R.string.badge_slow_and_steady_name, R.string.badge_slow_and_steady_desc),
    "picked_back_up" to BadgeText(R.string.badge_picked_back_up_name, R.string.badge_picked_back_up_desc),
    "pumpkin_patch" to BadgeText(R.string.badge_pumpkin_patch_name, R.string.badge_pumpkin_patch_desc),
    "sudoku_summit" to BadgeText(R.string.badge_sudoku_summit_name, R.string.badge_sudoku_summit_desc),
    "killersudoku_cage_keeper" to BadgeText(R.string.badge_killersudoku_cage_keeper_name, R.string.badge_killersudoku_cage_keeper_desc),
    "minisudoku_small_summit" to BadgeText(R.string.badge_minisudoku_small_summit_name, R.string.badge_minisudoku_small_summit_desc),
    "animaldoku_top_of_the_herd" to BadgeText(R.string.badge_animaldoku_top_of_the_herd_name, R.string.badge_animaldoku_top_of_the_herd_desc),
    "arrowescape_no_bumps" to BadgeText(R.string.badge_arrowescape_no_bumps_name, R.string.badge_arrowescape_no_bumps_desc),
    "starbattle_night_owl" to BadgeText(R.string.badge_starbattle_night_owl_name, R.string.badge_starbattle_night_owl_desc),
    "takuzu_even_keel" to BadgeText(R.string.badge_takuzu_even_keel_name, R.string.badge_takuzu_even_keel_desc),
    "nonogram_full_picture" to BadgeText(R.string.badge_nonogram_full_picture_name, R.string.badge_nonogram_full_picture_desc),
    "blockescape_clear_way" to BadgeText(R.string.badge_blockescape_clear_way_name, R.string.badge_blockescape_clear_way_desc),
    "circuit_live_wire" to BadgeText(R.string.badge_circuit_live_wire_name, R.string.badge_circuit_live_wire_desc),
    "links_unbroken" to BadgeText(R.string.badge_links_unbroken_name, R.string.badge_links_unbroken_desc),
    "wordswap_word_perfect" to BadgeText(R.string.badge_wordswap_word_perfect_name, R.string.badge_wordswap_word_perfect_desc),
    "minesweeper_clean_sweep" to BadgeText(R.string.badge_minesweeper_clean_sweep_name, R.string.badge_minesweeper_clean_sweep_desc),
    "minesweeper_every_flag" to BadgeText(R.string.badge_minesweeper_every_flag_name, R.string.badge_minesweeper_every_flag_desc),
    "minesweeper_wide_open" to BadgeText(R.string.badge_minesweeper_wide_open_name, R.string.badge_minesweeper_wide_open_desc),
    "wordsearch_hidden_word" to BadgeText(R.string.badge_wordsearch_hidden_word_name, R.string.badge_wordsearch_hidden_word_desc),
    "wordsearch_every_corner" to BadgeText(R.string.badge_wordsearch_every_corner_name, R.string.badge_wordsearch_every_corner_desc),
    "wordsearch_bright_eyes" to BadgeText(R.string.badge_wordsearch_bright_eyes_name, R.string.badge_wordsearch_bright_eyes_desc),
    "wordguess_last_line" to BadgeText(R.string.badge_wordguess_last_line_name, R.string.badge_wordguess_last_line_desc),
    "wordguess_in_three" to BadgeText(R.string.badge_wordguess_in_three_name, R.string.badge_wordguess_in_three_desc),
    "wordguess_in_two" to BadgeText(R.string.badge_wordguess_in_two_name, R.string.badge_wordguess_in_two_desc),
    "guessbynumbers_cracked_open" to BadgeText(R.string.badge_guessbynumbers_cracked_open_name, R.string.badge_guessbynumbers_cracked_open_desc),
    "guessbynumbers_few_clues" to BadgeText(R.string.badge_guessbynumbers_few_clues_name, R.string.badge_guessbynumbers_few_clues_desc),
    "guessbynumbers_three_clues" to BadgeText(R.string.badge_guessbynumbers_three_clues_name, R.string.badge_guessbynumbers_three_clues_desc),
    "themeclear_spotless" to BadgeText(R.string.badge_themeclear_spotless_name, R.string.badge_themeclear_spotless_desc),
    "themeclear_deep_theme" to BadgeText(R.string.badge_themeclear_deep_theme_name, R.string.badge_themeclear_deep_theme_desc),
    "themeclear_deep_and_spotless" to BadgeText(R.string.badge_themeclear_deep_and_spotless_name, R.string.badge_themeclear_deep_and_spotless_desc),
    "blockfill_two_at_once" to BadgeText(R.string.badge_blockfill_two_at_once_name, R.string.badge_blockfill_two_at_once_desc),
    "blockfill_three_at_once" to BadgeText(R.string.badge_blockfill_three_at_once_name, R.string.badge_blockfill_three_at_once_desc),
    "blockfill_clean_board" to BadgeText(R.string.badge_blockfill_clean_board_name, R.string.badge_blockfill_clean_board_desc),
    "game2048_1024" to BadgeText(R.string.badge_game2048_1024_name, R.string.badge_game2048_1024_desc),
    "game2048_2048" to BadgeText(R.string.badge_game2048_2048_name, R.string.badge_game2048_2048_desc),
    "game2048_4096" to BadgeText(R.string.badge_game2048_4096_name, R.string.badge_game2048_4096_desc),
    "chimptest_twelve" to BadgeText(R.string.badge_chimptest_twelve_name, R.string.badge_chimptest_twelve_desc),
    "chimptest_sixteen" to BadgeText(R.string.badge_chimptest_sixteen_name, R.string.badge_chimptest_sixteen_desc),
    "chimptest_twenty" to BadgeText(R.string.badge_chimptest_twenty_name, R.string.badge_chimptest_twenty_desc),
    "sequencememory_twelve" to BadgeText(R.string.badge_sequencememory_twelve_name, R.string.badge_sequencememory_twelve_desc),
    "sequencememory_sixteen" to BadgeText(R.string.badge_sequencememory_sixteen_name, R.string.badge_sequencememory_sixteen_desc),
    "sequencememory_twenty" to BadgeText(R.string.badge_sequencememory_twenty_name, R.string.badge_sequencememory_twenty_desc),
)

fun badgeText(def: BadgeDef): BadgeText {
    val icon = def.icon
    return if (icon is BadgeIcon.Journey) JOURNEY_TEXT.getValue(icon.stage) else BADGE_TEXT.getValue(def.id)
}
