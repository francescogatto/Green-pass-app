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
 *  Created by fgatto on 31/07/21, 19:30
 */

package it.ministerodellasalute.verificaC19.ui.custom

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import it.ministerodellasalute.verificaC19.R

abstract class BaseFragment : Fragment() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                openQrCodeReader()
            }
        }

    fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            createPermissionAlert()
        } else {
            openQrCodeReader()
        }
    }

    abstract fun openQrCodeReader()

    fun createPermissionAlert() {
        try {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle(getString(R.string.privacyTitle))
            builder.setMessage(getString(R.string.privacy))
            builder.setPositiveButton(getString(R.string.next)) { dialog, which ->
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            builder.setNegativeButton(getString(R.string.back)) { dialog, which ->
            }
            val dialog = builder.create()
            dialog.show()
        } catch (e: Exception) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun createNoKeyAlert() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.noKeyAlertTitle))
        builder.setMessage(getString(R.string.noKeyAlertMessage))
        builder.setPositiveButton(getString(R.string.ok)) { dialog, which ->
        }
        val dialog = builder.create()
        dialog.show()
    }

}