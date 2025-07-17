// SPDX-License-Identifier: GPL-3.0-or-later
package dev.itsvic.parceltracker

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
val DEMO_MODE = booleanPreferencesKey("demoMode")
val UNMETERED_ONLY = booleanPreferencesKey("unmeteredOnly")
val CLIPBOARD_PASTE_ENABLED = booleanPreferencesKey("clipboardPasteEnabled")
val PREFERRED_REGION = stringPreferencesKey("preferredRegion")

// API key settings
val DHL_API_KEY = stringPreferencesKey("dhlApiKey")
