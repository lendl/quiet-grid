package com.quietgrid.app.platform

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings

private const val TAP_DURATION_MS = 20L
private const val CORRECT_DURATION_MS = 35L
private const val INCORRECT_DURATION_MS = 45L

class AndroidHaptics(context: Context, private val appEnabled: Boolean) : Haptics {
    private val vibrator: Vibrator = defaultVibrator(context)
    private val systemEnabled: Boolean = systemHapticsEnabled(context)

    override fun tapFeedback() = fire(tickEffect())
    override fun correctFeedback() = fire(clickEffect())
    override fun incorrectFeedback() = fire(doubleClickEffect())

    private fun fire(effect: VibrationEffect) {
        if (!appEnabled || !systemEnabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
        } else {
            vibrator.vibrate(effect)
        }
    }

    private fun tickEffect(): VibrationEffect = predefinedOrFallback(VibrationEffect.EFFECT_TICK, TAP_DURATION_MS)
    private fun clickEffect(): VibrationEffect = predefinedOrFallback(VibrationEffect.EFFECT_CLICK, CORRECT_DURATION_MS)
    private fun doubleClickEffect(): VibrationEffect = predefinedOrFallback(VibrationEffect.EFFECT_DOUBLE_CLICK, INCORRECT_DURATION_MS)

    private fun predefinedOrFallback(predefinedEffect: Int, fallbackDurationMs: Long): VibrationEffect =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            VibrationEffect.createPredefined(predefinedEffect)
        } else {
            VibrationEffect.createOneShot(fallbackDurationMs, VibrationEffect.DEFAULT_AMPLITUDE)
        }
}

private fun defaultVibrator(context: Context): Vibrator =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

private fun systemHapticsEnabled(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU || legacyHapticSettingEnabled(context)

@Suppress("DEPRECATION")
private fun legacyHapticSettingEnabled(context: Context): Boolean =
    Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0
