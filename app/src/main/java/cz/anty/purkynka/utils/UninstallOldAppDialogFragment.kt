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

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import cz.anty.purkynka.R
import org.jetbrains.anko.alert
import org.jetbrains.anko.appcompat.v7.Appcompat
import org.jetbrains.anko.support.v4.act

/**
 * @author anty
 */
class UninstallOldAppDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            act.alert(Appcompat) {
                titleResource = R.string.dialog_uninstall_old_app_title
                messageResource = R.string.dialog_uninstall_old_app_message
                isCancelable = true
                negativeButton(R.string.but_no) { it.cancel() }
                positiveButton(R.string.but_uninstall) {
                    it.dismiss()
                    startActivity(Intent(
                            Intent.ACTION_DELETE,
                            Uri.fromParts(
                                    "package",
                                    APP_OLD_PACKAGE_NAME,
                                    null
                            )
                    ))
                }
            }.build()
}