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

package cz.anty.purkynka.account.ui

import android.accounts.Account
import android.content.Context
import cz.anty.purkynka.R
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import eu.codetopic.utils.ui.container.items.custom.CustomItemViewHolder
import kotlinx.android.synthetic.main.item_account_spinner.*

/**
 * @author anty
 */
class AccountSpinnerItem(val account: Account, val accountId: String) : CustomItem() {

    override fun onBindViewHolder(holder: CustomItemViewHolder, itemPosition: Int) {
        holder.txtAccountName.text = account.name
    }

    override fun getLayoutResId(context: Context): Int = R.layout.item_account_spinner

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AccountSpinnerItem

        if (account != other.account) return false
        if (accountId != other.accountId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = account.hashCode()
        result = 31 * result + accountId.hashCode()
        return result
    }
}