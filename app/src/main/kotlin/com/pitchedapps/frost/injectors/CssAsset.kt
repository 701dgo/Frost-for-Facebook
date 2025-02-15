/*
 * Copyright 2020 Allan Wang
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
package com.pitchedapps.frost.injectors

import android.webkit.WebView
import com.pitchedapps.frost.prefs.Prefs

/**
 * Small misc inline css assets
 */
enum class CssAsset(private val content: String) : InjectorContract {
    FullSizeImage("div._4prr[style*=\"max-width\"][style*=\"max-height\"]{max-width:none !important;max-height:none !important}"),

    /*
     * Remove top margin and hide some contents from the top bar and home page (as it's our base url)
     */
    Menu("#bookmarks_flyout{margin-top:0 !important}#m_news_feed_stream,#MComposer{display:none !important}")
    ;

    val injector: JsInjector by lazy {
        JsBuilder().css(content).single("css-small-assets-$name").build()
    }

    override fun inject(webView: WebView, prefs: Prefs) {
        injector.inject(webView, prefs)
    }
}
