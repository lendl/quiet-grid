package com.quietgrid.app.ui.components

import com.quietgrid.app.core.GameId

object SharedElementKeys {
    fun gameIdentity(gameId: GameId): String = "game-identity-${gameId.key}"
}
