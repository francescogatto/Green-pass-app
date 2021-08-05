/*
 *  ---license-start
 *  eu-digital-green-certificates / dgca-verifier-app-android
 *  ---
 *  Copyright (C) 2021 T-Systems International GmbH and all other contributors
 *  ---
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ---license-end
 *
 *  Created by mykhailo.nester on 4/27/21 10:41 PM
 */

package it.ministerodellasalute.verificaC19.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.WorkerThread
import androidx.core.content.edit
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface Preferences {

    var resumeToken: Long

    var dateLastFetch: Long

    var validationRulesJson: String?

    fun clear()

    var listOfGreenPass: List<String>
}

/**
 * [Preferences] impl backed by [android.content.SharedPreferences].
 */
class PreferencesImpl(context: Context) : Preferences {

    private var preferences: Lazy<SharedPreferences> = lazy {
        context.applicationContext.getSharedPreferences(USER_PREF, Context.MODE_PRIVATE)
    }

    override var resumeToken by LongPreference(preferences, KEY_RESUME_TOKEN, -1)

    override var dateLastFetch by LongPreference(preferences, KEY_DATE_LAST_FETCH, -1)

    override var validationRulesJson by StringPreference(preferences, KEY_VALIDATION_RULES, "")

    override var listOfGreenPass: List<String> by ListStringPreference(preferences, KEY_LIST_GREEN_PASS, emptySet())


    override fun clear() {
        preferences.value.edit().clear().apply()
    }

    companion object {
        private const val USER_PREF = "dgca.verifier.app.pref"
        private const val KEY_RESUME_TOKEN = "resume_token"
        private const val KEY_DATE_LAST_FETCH = "date_last_fetch"
        private const val KEY_VALIDATION_RULES = "validation_rules"
        private const val KEY_LIST_GREEN_PASS = "KEY_LIST_GREEN_PASS"

    }
}

class StringPreference(
        private val preferences: Lazy<SharedPreferences>,
        private val name: String,
        private val defaultValue: String
) : ReadWriteProperty<Any, String?> {

    @WorkerThread
    override fun getValue(thisRef: Any, property: KProperty<*>): String? {
        return preferences.value.getString(name, defaultValue)
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: String?) {
        preferences.value.edit { putString(name, value) }
    }
}

class ListStringPreference(
    private val preferences: Lazy<SharedPreferences>,
    private val name: String,
    private val defaultValue: Set<String>
) : ReadWriteProperty<Any, List<String>> {

    @WorkerThread
    override fun getValue(thisRef: Any, property: KProperty<*>): List<String> {
        return preferences.value.getStringSet(name, defaultValue)?.toList() ?: emptyList()
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: List<String>) {
        preferences.value.edit { putStringSet(name, value.toSet()) }
    }
}

class LongPreference(
    private val preferences: Lazy<SharedPreferences>,
    private val name: String,
    private val defaultValue: Long
) : ReadWriteProperty<Any, Long> {

    @WorkerThread
    override fun getValue(thisRef: Any, property: KProperty<*>): Long {
        return preferences.value.getLong(name, defaultValue)
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Long) {
        preferences.value.edit { putLong(name, value) }
    }
}