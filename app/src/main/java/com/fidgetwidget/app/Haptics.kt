package com.fidgetwidget.app

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class Haptics(context: Context) {
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= 31)
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        else @Suppress("DEPRECATION") (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)

    private val ok get() = vibrator?.hasVibrator() == true

    fun tick() {
        if (!ok) return
        if (Build.VERSION.SDK_INT >= 29) vibrator!!.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        else vibrator!!.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun click() {
        if (!ok) return
        if (Build.VERSION.SDK_INT >= 29) vibrator!!.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        else vibrator!!.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun success() {
        if (!ok) return
        vibrator!!.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 40, 70, 40, 70, 80), -1))
    }
}
