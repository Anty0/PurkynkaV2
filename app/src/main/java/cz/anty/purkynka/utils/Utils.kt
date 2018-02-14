/*
 * app
 * Copyright (C)   2018  anty
 *
 * This program is free  software: you can redistribute it and/or modify
 * it under the terms  of the GNU General Public License as published by
 * the Free Software  Foundation, either version 3 of the License, or
 * (at your option) any  later version.
 *
 * This program is distributed in the hope that it  will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied  warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.   See the
 * GNU General Public License for more details.
 *
 * You  should have received a copy of the GNU General Public License
 * along  with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package cz.anty.purkynka.utils

import android.accounts.Account
import android.content.ContentResolver
import android.content.SyncStatusObserver
import android.graphics.Color
import android.os.Build
import android.support.annotation.ColorInt
import cz.anty.purkynka.BuildConfig
import eu.codetopic.java.utils.JavaExtensions.ifNull
import eu.codetopic.java.utils.JavaExtensions.ifTrue
import eu.codetopic.java.utils.JavaExtensions.join
import eu.codetopic.java.utils.log.Log
import io.michaelrocks.bimap.BiMap
import io.michaelrocks.bimap.HashBiMap
import io.michaelrocks.bimap.MutableBiMap
import kotlinx.coroutines.experimental.withTimeoutOrNull
import kotlin.coroutines.experimental.suspendCoroutine
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/**
 * @author anty
 */
object Utils {

    private const val LOG_TAG = "Utils"

    @ColorInt
    private val colorNeutral: Int = Color.rgb(187, 222, 251)
    private val colorSegments: Array<Array<Int>> = arrayOf(
            arrayOf(204, 255, 144),
            arrayOf(244, 255, 129),
            arrayOf(255, 209, 128),
            arrayOf(255, 158, 128),
            arrayOf(255, 138, 128)
    )

    @ColorInt
    fun colorForValue(value: Int?, size: Int): Int {
        if (value == null) return colorNeutral

        // diff r:  40  22   0   0
        // diff g:   0 -46 -51 -20
        // diff b: -15 - 1   0   0

        val fixedValue = min(max(0, value), size - 1)

        val segmentSize = size / 5F
        val segment = (fixedValue / segmentSize).toInt()
        val segmentMove = (fixedValue % segmentSize) / segmentSize

        val segmentColor = colorSegments[segment]
        if (segmentMove == 0F) return segmentColor.let { Color.rgb(it[0], it[1], it[2]) }

        val nextSegmentColor = colorSegments[segment + 1]

        val rgbColor = segmentColor.join(nextSegmentColor) { sc, nsc -> round(sc + (nsc - sc) * segmentMove).toInt() }
        return Color.rgb(rgbColor[0], rgbColor[1], rgbColor[2])
    }

    val userAgent: String
        get() = "Purkynka/${BuildConfig.VERSION_NAME} (Android ${Build.VERSION.RELEASE}; " +
                "Linux; rv:${BuildConfig.VERSION_CODE}; cz-cs) Mozilla/5.0 Gecko/20100101 Firefox/58.0"

    private const val TIMEOUT_SYNC_ACTIVE_MILIS = 30L * 1_000L

    suspend fun awaitForSyncCompleted(account: Account, contentAuthority: String): Boolean {
        // Sync sometimes reports, that is not started, at the beginning,
        // so we must wait for sync start before we can wait for sync end.
        awaitForSyncAdd(account, contentAuthority)

        return awaitForSyncActiveOrEnd(account, contentAuthority) ifTrue {
            awaitForSyncEnd(account, contentAuthority)
        }
    }

    suspend fun awaitForSyncAdd(account: Account, contentAuthority: String) = awaitForSyncState {
        (ContentResolver.isSyncActive(account, contentAuthority) ||
                ContentResolver.isSyncPending(account, contentAuthority))
                .also {
                    Log.d(LOG_TAG, "awaitForSyncAdd(account=$account," +
                            " contentAuthority=$contentAuthority)" +
                            " -> (conditionResult=$it)")
                }
    }

    suspend fun awaitForSyncActiveOrEnd(account: Account, contentAuthority: String) =
            withTimeoutOrNull(TIMEOUT_SYNC_ACTIVE_MILIS) {
                awaitForSyncState {
                    (ContentResolver.isSyncActive(account, contentAuthority) ||
                            !ContentResolver.isSyncPending(account, contentAuthority))
                            .also {
                                Log.d(LOG_TAG, "awaitForSyncActiveOrEnd(account=$account," +
                                        " contentAuthority=$contentAuthority)" +
                                        " -> (conditionResult=$it)")
                            }
                }
            } ifNull {
                Log.w(LOG_TAG, "awaitForSyncActiveOrEnd(account=$account," +
                        " contentAuthority=$contentAuthority) -> Timeout reached")
            } != null

    suspend fun awaitForSyncEnd(account: Account, contentAuthority: String) = awaitForSyncState {
        (!ContentResolver.isSyncActive(account, contentAuthority) &&
                !ContentResolver.isSyncPending(account, contentAuthority))
                .also {
                    Log.d(LOG_TAG, "awaitForSyncEnd(account=$account," +
                            " contentAuthority=$contentAuthority)" +
                            " -> (conditionResult=$it)")
                }
    }

    private suspend inline fun awaitForSyncState(crossinline condition: () -> Boolean) =
            suspendCoroutine<Unit> { cont ->
                if (run(condition)) {
                    cont.resume(Unit)
                    return@suspendCoroutine
                }

                var observerHandle: Any? = null
                val observer = SyncStatusObserver {
                    if (!run(condition)) return@SyncStatusObserver

                    observerHandle?.let {
                        synchronized(it) {
                            if (observerHandle == null) return@SyncStatusObserver
                            observerHandle = null
                            ContentResolver.removeStatusChangeListener(it)
                            cont.resume(Unit)
                        }
                    }
                }

                val mask = ContentResolver.SYNC_OBSERVER_TYPE_PENDING or
                        ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE
                observerHandle = ContentResolver.addStatusChangeListener(mask, observer)

                // WTF is 'which' argument of SyncStatusObserver?!
                // There is nothing in android documentation...
                observer.onStatusChanged(0)
            }

    fun <K : Any, V : Any> biMapOf(vararg pairs: Pair<K, V>): BiMap<K, V> =
            mutableBiMapOf(*pairs)

    fun <K : Any, V : Any> mutableBiMapOf(vararg pairs: Pair<K, V>): MutableBiMap<K, V> =
            pairs.toMap(HashBiMap(pairs.size))
}