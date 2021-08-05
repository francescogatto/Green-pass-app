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
 *  Created by fgatto on 02/08/21, 08:10
 */

package it.ministerodellasalute.verificaC19.ui.custom

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.ministerodellasalute.verificaC19.data.local.Preferences
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val preferences: Preferences) : ViewModel() {

    private val _refreshHome = MutableLiveData<Boolean>()
    val refreshHome: LiveData<Boolean> = _refreshHome

    fun deleteCertificate(qrCodeText: String) {
        val tmpList = preferences.listOfGreenPass.toMutableList()
        tmpList.remove(qrCodeText)
        preferences.listOfGreenPass = tmpList
        viewModelScope.launch {
            //trick provvisorio
            _refreshHome.value = true
            _refreshHome.value = false
        }
    }


}