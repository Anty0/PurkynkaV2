/*
 * app
 * Copyright (C)   2017  anty
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
import android.content.ContentResolver
import android.os.Bundle
import org.jetbrains.anko.bundleOf

/**
 * @author anty
 */
object Syncs {

    fun trigger(account: Account, contentAuthority: String, extras: Bundle? = null) {
        ContentResolver.requestSync(
                account,
                contentAuthority,
                bundleOf(
                        ContentResolver.SYNC_EXTRAS_MANUAL to true,
                        ContentResolver.SYNC_EXTRAS_EXPEDITED to true
                ).apply { extras?.let { putAll(it) } }
        )
    }

    fun updateEnabled(enable: Boolean, account: Account, contentAuthority: String,
                     syncFrequency: Long, extras: Bundle = Bundle()) {
        ContentResolver.setIsSyncable(account, contentAuthority, if (enable) 1 else 0)
        ContentResolver.setSyncAutomatically(account, contentAuthority, enable)
        if (enable) ContentResolver.addPeriodicSync(account, contentAuthority, extras, syncFrequency)
        else ContentResolver.removePeriodicSync(account, contentAuthority, extras)
    }
}