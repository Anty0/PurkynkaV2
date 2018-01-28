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

package cz.anty.purkynka.account.save

import android.content.Context
import android.content.SharedPreferences
import cz.anty.purkynka.PrefNames.FILE_NAME_ACTIVE_ACCOUNT_DATA
import eu.codetopic.utils.data.preferences.provider.BasicSharedPreferencesProvider
import eu.codetopic.utils.data.preferences.provider.ISharedPreferencesProvider
import eu.codetopic.utils.data.preferences.support.VersionedContentProviderPreferences

/**
 * @author anty
 */
class ActiveAccountProvider : VersionedContentProviderPreferences<SharedPreferences>(AUTHORITY, ActiveAccount.SAVE_VERSION) {

    companion object {
        const val AUTHORITY = "cz.anty.purkynka.account.active"
    }

    override fun onPreparePreferencesProvider(): ISharedPreferencesProvider<android.content.SharedPreferences> {
        return BasicSharedPreferencesProvider(context, FILE_NAME_ACTIVE_ACCOUNT_DATA, Context.MODE_PRIVATE)
    }

    override fun onUpgrade(editor: android.content.SharedPreferences.Editor, from: Int, to: Int) {
        ActiveAccount.onUpgrade(editor, from, to)
    }
}