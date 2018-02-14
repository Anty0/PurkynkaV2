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

package cz.anty.purkynka.lunches

import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.Fragment
import cz.anty.purkynka.utils.Constants.ICON_LUNCHES
import cz.anty.purkynka.R
import cz.anty.purkynka.account.save.ActiveAccount
import cz.anty.purkynka.dashboard.DashboardFragment
import cz.anty.purkynka.lunches.save.LunchesLoginData
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.AndroidExtensions.getIconics
import eu.codetopic.utils.ui.activity.fragment.IconProvider
import eu.codetopic.utils.ui.activity.fragment.ThemeProvider
import eu.codetopic.utils.ui.activity.navigation.NavigationFragment
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.support.v4.ctx
import proguard.annotation.Keep
import proguard.annotation.KeepName

/**
 * @author anty
 */
@Keep
@KeepName
@ContainerOptions(CacheImplementation.SPARSE_ARRAY)
class LunchesDecideFragment : NavigationFragment(), ThemeProvider, IconProvider {

    companion object {

        private const val LOG_TAG = "LunchesDecideFragment"

        const val EXTRA_TARGET_CLASS_NAME = "TARGET_CLASS_NAME"
    }

    override val themeId: Int
        get() = R.style.AppTheme_Lunches
    override val icon: Bitmap
        get() = ctx.getIconics(ICON_LUNCHES).sizeDp(48).toBitmap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("UNCHECKED_CAST")
        val targetClass = try {
            Class.forName(
                    arguments?.getString(EXTRA_TARGET_CLASS_NAME) ?: run {
                        Log.e(LOG_TAG, "onCreate()" +
                                " -> Target class not received, switching to dashboard")
                        switchFragment(DashboardFragment::class.java)
                        return
                    }
            ) as? Class<out Fragment> ?: run {
                Log.e(LOG_TAG, "onCreate()" +
                        " -> Target class is not fragment, switching to dashboard")
                switchFragment(DashboardFragment::class.java)
                return
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "onCreate()" +
                    " -> Failed to find target class, switching to dashboard")
            switchFragment(DashboardFragment::class.java)
            return
        }

        val holder = holder
        launch(UI) {
            holder.showLoading()

            run switcher@ {
                val accountId = bg { ActiveAccount.getId() }.await() ?: run {
                    Log.w(LOG_TAG, "onCreate()" +
                            " -> No active account, switching to dashboard")
                    switchFragment(DashboardFragment::class.java)
                    return@switcher
                }

                val userLoggedId = bg { LunchesLoginData.loginData.isLoggedIn(accountId) }.await()

                switchFragment(
                        if (userLoggedId) targetClass
                        else LunchesLoginFragment::class.java
                )
            }

            holder.hideLoading()
        }
    }
}