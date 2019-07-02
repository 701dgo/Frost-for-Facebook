/*
 * Copyright 2018 Allan Wang
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.pitchedapps.frost

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.webkit.CookieManager
import android.widget.ImageView
import android.widget.TextView
import ca.allanwang.kau.internal.KauBaseActivity
import ca.allanwang.kau.utils.buildIsLollipopAndUp
import ca.allanwang.kau.utils.setIcon
import ca.allanwang.kau.utils.string
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.pitchedapps.frost.activities.LoginActivity
import com.pitchedapps.frost.activities.SelectorActivity
import com.pitchedapps.frost.db.CookieDao
import com.pitchedapps.frost.db.CookieEntity
import com.pitchedapps.frost.db.CookieModel
import com.pitchedapps.frost.db.FbTabModel
import com.pitchedapps.frost.db.GenericDao
import com.pitchedapps.frost.db.getTabs
import com.pitchedapps.frost.db.save
import com.pitchedapps.frost.db.saveTabs
import com.pitchedapps.frost.db.selectAll
import com.pitchedapps.frost.facebook.FbCookie
import com.pitchedapps.frost.utils.BiometricUtils
import com.pitchedapps.frost.utils.L
import com.pitchedapps.frost.utils.Prefs
import com.pitchedapps.frost.utils.launchImageActivity
import com.pitchedapps.frost.utils.launchNewTask
import com.pitchedapps.frost.utils.loadAssets
import com.raizlabs.android.dbflow.kotlinextensions.from
import com.raizlabs.android.dbflow.kotlinextensions.select
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.util.ArrayList

/**
 * Created by Allan Wang on 2017-05-28.
 */
class StartActivity : KauBaseActivity() {

    private val cookieDao: CookieDao by inject()
    private val genericDao: GenericDao by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!buildIsLollipopAndUp) { // not supported
            showInvalidSdkView()
            return
        }

        try {
            // TODO add better descriptions
            CookieManager.getInstance()
        } catch (e: Exception) {
            showInvalidWebView()
        }

        launch {
            val authDefer = BiometricUtils.authenticate(this@StartActivity)
            try {
                migrate()
                FbCookie.switchBackUser()
                val cookies = ArrayList(cookieDao.selectAll())
                L.i { "Cookies loaded at time ${System.currentTimeMillis()}" }
                L._d {
                    "Cookies: ${cookies.joinToString(
                        "\t",
                        transform = CookieEntity::toSensitiveString
                    )}"
                }
                loadAssets()
                authDefer.await()
                when {
                    cookies.isEmpty() -> launchNewTask<LoginActivity>()
                    // Has cookies but no selected account
                    Prefs.userId == -1L -> launchNewTask<SelectorActivity>(cookies)
                    else -> launchImageActivity("https://images.pexels.com/photos/374870/pexels-photo-374870.jpeg?auto=compress&cs=tinysrgb&dpr=1&w=500")
//                    else -> startActivity<MainActivity>(intentBuilder = {
//                        putParcelableArrayListExtra(EXTRA_COOKIES, cookies)
//                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
//                            Intent.FLAG_ACTIVITY_SINGLE_TOP
//                    })
                }
            } catch (e: Exception) {
                L._e(e) { "Load start failed" }
                showInvalidWebView()
            }
        }
    }

    /**
     * Migrate from dbflow to room
     * TODO delete dbflow data
     */
    private suspend fun migrate() = withContext(Dispatchers.IO) {
        if (cookieDao.selectAll().isNotEmpty()) return@withContext
        val cookies = (select from CookieModel::class).queryList()
            .map { CookieEntity(it.id, it.name, it.cookie) }
        if (cookies.isNotEmpty()) {
            cookieDao.save(cookies)
            L._d { "Migrated cookies ${cookieDao.selectAll()}" }
        }
        val tabs = (select from FbTabModel::class).queryList().map(FbTabModel::tab)
        if (tabs.isNotEmpty()) {
            genericDao.saveTabs(tabs)
            L._d { "Migrated tabs ${genericDao.getTabs()}" }
        }
        deleteDatabase("Cookies.db")
        deleteDatabase("FrostTabs.db")
    }

    private fun showInvalidWebView() =
        showInvalidView(R.string.error_webview)

    private fun showInvalidSdkView() {
        val text = String.format(string(R.string.error_sdk), Build.VERSION.SDK_INT)
        showInvalidView(text)
    }

    private fun showInvalidView(textRes: Int) =
        showInvalidView(string(textRes))

    private fun showInvalidView(text: String) {
        setContentView(R.layout.activity_invalid)
        findViewById<ImageView>(R.id.invalid_icon)
            .setIcon(GoogleMaterial.Icon.gmd_adb, -1, Color.WHITE)
        findViewById<TextView>(R.id.invalid_text).text = text
    }
}
