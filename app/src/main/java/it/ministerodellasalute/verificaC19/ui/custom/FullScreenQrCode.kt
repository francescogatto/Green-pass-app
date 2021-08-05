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
 *  Created by fgatto on 04/08/21, 13:09
 */

package it.ministerodellasalute.verificaC19.ui.custom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import it.ministerodellasalute.verificaC19.databinding.FHomeBinding
import it.ministerodellasalute.verificaC19.databinding.FQrcodeBinding
import it.ministerodellasalute.verificaC19.ui.main.verification.VerificationFragmentArgs

class FullScreenQrCode: Fragment() {

    private val args by navArgs<FullScreenQrCodeArgs>()

    private var _binding: FQrcodeBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val barcodeEncoder = BarcodeEncoder()
        val bitmap = barcodeEncoder.encodeBitmap(args.qrCodeText, BarcodeFormat.QR_CODE, 600, 600)
        binding.qrcode.setImageBitmap(bitmap)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FQrcodeBinding.inflate(inflater, container, false)
        return binding.root
    }
}