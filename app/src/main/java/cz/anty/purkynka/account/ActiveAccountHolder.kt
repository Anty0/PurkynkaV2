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

package cz.anty.purkynka.account

import android.accounts.Account
import cz.anty.purkynka.account.save.ActiveAccount
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.*
import eu.codetopic.utils.broadcast.LocalBroadcast
import eu.codetopic.utils.ui.view.holder.loading.LoadingVH
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.coroutines.experimental.bg

/**
 * @author anty
 */
class ActiveAccountHolder(private val holder: LoadingVH? = null) {

    companion object {

        private const val LOG_TAG = "ActiveAccountHolder"
    }

    private val accountChangedReceiver = receiver { _, _ ->
        Log.d(LOG_TAG, "accountChangedReceiver.onReceive()")
        updateWithLoading()
    }

    private val listeners = mutableListOf<suspend () -> Unit>()

    var account: Account? = null
        private set
    var accountId: String? = null
        private set

    fun updateWithLoading(): Job {
        val holder = holder
        val self = this.asReference()
        return launch(UI) {
            holder?.showLoading()

            self().update().join()

            delay(500) // Wait few loops to make sure, that content was updated.
            holder?.hideLoading()
        }
    }

    fun update(): Job {
        val self = this.asReference()

        return launch(UI) {
            val (nAccountId, nAccount) = bg { ActiveAccount.getWithId() }.await()

            self().apply {
                if (account == nAccount && accountId == nAccountId)
                    return@apply

                account = nAccount
                accountId = nAccountId

                listeners.forEach { it() }
            }
        }
    }

    fun addChangeListener(listener: suspend () -> Unit) {
        listeners.add(listener)
    }

    fun register(): Job {
        LocalBroadcast.registerReceiver(accountChangedReceiver,
                intentFilter(ActiveAccount.getter))

        return update()
    }

    fun unregister() {
        LocalBroadcast.unregisterReceiver(accountChangedReceiver)
    }
}