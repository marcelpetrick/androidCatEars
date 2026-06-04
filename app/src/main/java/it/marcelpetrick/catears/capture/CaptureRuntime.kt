// SPDX-FileCopyrightText: 2026 Marcel Petrick <mail@marcelpetrick.it>
// SPDX-License-Identifier: GPL-3.0-or-later

package it.marcelpetrick.catears.capture

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.util.Locale
import javax.inject.Inject
import kotlin.random.Random

interface CaptureRuntime {
    val ioDispatcher: CoroutineDispatcher
    fun nowMillis(): Long
    fun randomSuffix(): String
}

class DefaultCaptureRuntime @Inject constructor() : CaptureRuntime {
    override val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun randomSuffix(): String = String.format(Locale.US, "%04x", Random.nextInt(SUFFIX_RANGE))

    private companion object {
        const val SUFFIX_RANGE = 0x10000
    }
}
