package com.quietgrid.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId

const val OPEN_TAB_EXTRA = "open_tab"
const val OPEN_TAB_DAILY = "daily"
private const val LAUNCH_ACTION_EXTRA = "launch_action"
private const val LAUNCH_MIX_ID_EXTRA = "launch_mix_id"
private const val LAUNCH_GAME_ID_EXTRA = "launch_game_id"
private const val LAUNCH_DIFFICULTY_EXTRA = "launch_difficulty"
private const val ACTION_RESUME = "resume"
private const val ACTION_MIXES = "mixes"
private const val ACTION_MIX = "mix"
private const val ACTION_GAME = "game"

sealed interface LaunchRequest {
    data object DailyTab : LaunchRequest
    data object Resume : LaunchRequest
    data object OpenMixes : LaunchRequest
    data class StartMix(val mixId: String) : LaunchRequest
    data class StartGame(val gameId: GameId, val difficulty: Difficulty) : LaunchRequest
}

fun LaunchRequest.toIntent(context: Context): Intent {
    val intent = Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    val path = when (this) {
        LaunchRequest.DailyTab -> {
            intent.putExtra(OPEN_TAB_EXTRA, OPEN_TAB_DAILY)
            OPEN_TAB_DAILY
        }
        LaunchRequest.Resume -> {
            intent.putExtra(LAUNCH_ACTION_EXTRA, ACTION_RESUME)
            ACTION_RESUME
        }
        LaunchRequest.OpenMixes -> {
            intent.putExtra(LAUNCH_ACTION_EXTRA, ACTION_MIXES)
            ACTION_MIXES
        }
        is LaunchRequest.StartMix -> {
            intent.putExtra(LAUNCH_ACTION_EXTRA, ACTION_MIX).putExtra(LAUNCH_MIX_ID_EXTRA, mixId)
            "$ACTION_MIX/${Uri.encode(mixId)}"
        }
        is LaunchRequest.StartGame -> {
            intent.putExtra(LAUNCH_ACTION_EXTRA, ACTION_GAME)
                .putExtra(LAUNCH_GAME_ID_EXTRA, gameId.key)
                .putExtra(LAUNCH_DIFFICULTY_EXTRA, difficulty.key)
            "$ACTION_GAME/${gameId.key}/${difficulty.key}"
        }
    }
    return intent.setData(Uri.parse("quietgrid://launch/$path"))
}

fun Intent?.toLaunchRequest(): LaunchRequest? {
    if (this == null) return null
    if (getStringExtra(OPEN_TAB_EXTRA) == OPEN_TAB_DAILY) return LaunchRequest.DailyTab
    return when (getStringExtra(LAUNCH_ACTION_EXTRA)) {
        ACTION_RESUME -> LaunchRequest.Resume
        ACTION_MIXES -> LaunchRequest.OpenMixes
        ACTION_MIX -> getStringExtra(LAUNCH_MIX_ID_EXTRA)?.let { LaunchRequest.StartMix(it) }
        ACTION_GAME -> {
            val gameId = GameId.entries.firstOrNull { it.key == getStringExtra(LAUNCH_GAME_ID_EXTRA) } ?: return null
            val difficulty = Difficulty.entries.firstOrNull { it.key == getStringExtra(LAUNCH_DIFFICULTY_EXTRA) } ?: return null
            LaunchRequest.StartGame(gameId, difficulty)
        }
        else -> null
    }
}
