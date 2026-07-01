package com.example.solochef.ui

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.monotonicFrameClock
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * 重惯性 fling behavior。
 *
 * 与 Compose 默认 spline decay 的差异：
 * - 初始速度 × 0.6 → 手指离屏后惯性更轻
 * - 每帧衰减 × 0.95 → 衰减更慢，滑动持续更久
 * - `delay(16ms)` 逐帧让步渲染 → 不阻塞主线程
 */
@Composable
fun rememberSmoothFlingBehavior(): FlingBehavior {
    return remember {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                var velocity = initialVelocity * 0.6f
                while (abs(velocity) > 1f) {
                    delay(16) // 等待下一帧，避免阻塞
                    val delta = velocity * 0.016f
                    scrollBy(delta)
                    velocity *= 0.95f
                }
                return velocity
            }
        }
    }
}
