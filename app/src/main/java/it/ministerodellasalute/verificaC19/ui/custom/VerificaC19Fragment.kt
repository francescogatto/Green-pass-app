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
 *  Created by fgatto on 31/07/21, 19:02
 */

package it.ministerodellasalute.verificaC19.ui.custom

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import it.ministerodellasalute.verificaC19.BuildConfig
import it.ministerodellasalute.verificaC19.FORMATTED_DATE_LAST_SYNC
import it.ministerodellasalute.verificaC19.R
import it.ministerodellasalute.verificaC19.databinding.ActivityFirstBinding
import it.ministerodellasalute.verificaC19.parseTo
import it.ministerodellasalute.verificaC19.ui.FirstViewModel

@ExperimentalUnsignedTypes
@AndroidEntryPoint
class VerificaC19Fragment : BaseFragment(), View.OnClickListener {

    private var _binding: ActivityFirstBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModels<FirstViewModel>()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ActivityFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.qrButton.setOnClickListener(this)

        binding.versionText.text = getString(R.string.version, BuildConfig.VERSION_NAME)

        viewModel.getDateLastSync().let {
            binding.dateLastSyncText.text = getString(R.string.lastSyncDate, if (it == -1L) getString(R.string.notAvailable) else it.parseTo(FORMATTED_DATE_LAST_SYNC))
        }

        viewModel.fetchStatus.observe(viewLifecycleOwner) {
            if (it) {
                binding.qrButton.isEnabled = false
                binding.dateLastSyncText.text = getString(R.string.loading)
            } else {
                binding.qrButton.isEnabled = true
                viewModel.getDateLastSync().let { date ->
                    binding.dateLastSyncText.text = getString(R.string.lastSyncDate, if (date == -1L) getString(R.string.notAvailable) else date.parseTo(FORMATTED_DATE_LAST_SYNC))
                }
            }
        }

        binding.privacyPolicy.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.dgc.gov.it/web/pn.html"))
            startActivity(browserIntent)
        }
    }


    override fun openQrCodeReader() {
        findNavController().navigate(VerificaC19FragmentDirections.actionVerificaC19FragmentToCodeReaderFragment())
    }

    override fun onClick(v: View?) {
        viewModel.getDateLastSync().let {
            if (it == -1L) {
                createNoKeyAlert()
                return
            }
        }
        when (v?.id) {
            R.id.qrButton -> checkCameraPermission()
        }
    }



}