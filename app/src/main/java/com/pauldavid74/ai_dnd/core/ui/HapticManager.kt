package com.pauldavid74.ai_dnd.core.ui

import android.content.Context
import android.os.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HapticManager @Inject constructor(
    context: Context
) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
    }

    fun lightTick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            vibratePrimitive(VibrationEffect.Composition.PRIMITIVE_TICK)
        } else {
            legacyVibrate(50)
        }
    }

    fun heavyThud() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vibratePrimitive(VibrationEffect.Composition.PRIMITIVE_THUD)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            vibratePrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK)
        } else {
            legacyVibrate(100)
        }
    }

    fun successCrescendo() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            vibrator?.let {
                if (it.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE)) {
                    it.vibrate(VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE)
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK)
                        .compose())
                } else {
                    legacyVibrate(150)
                }
            }
        } else {
            legacyVibrate(150)
        }
    }

    fun statusWobble() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            vibrator?.let {
                if (it.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE)) {
                    it.vibrate(VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE)
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK)
                        .compose())
                } else {
                    legacyVibrate(200)
                }
            }
        } else {
            legacyVibrate(200)
        }
    }

    /**
     * Resist primitive for UI tension (mandated by PRD).
     */
    fun resist() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vibratePrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            vibratePrimitive(VibrationEffect.Composition.PRIMITIVE_TICK)
        } else {
            legacyVibrate(10)
        }
    }

    private fun vibratePrimitive(primitiveId: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            vibrator?.let {
                if (it.areAllPrimitivesSupported(primitiveId)) {
                    it.vibrate(VibrationEffect.startComposition().addPrimitive(primitiveId).compose())
                }
            }
        }
    }

    private fun legacyVibrate(duration: Long) {
        vibrator?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}
