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
 */

package it.ministerodellasalute.verificaC19.ui.main.verification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import dagger.hilt.android.AndroidEntryPoint
import it.ministerodellasalute.verificaC19.*
import it.ministerodellasalute.verificaC19.databinding.FragmentVerificationNewBinding
import it.ministerodellasalute.verificaC19.model.CertificateModel
import it.ministerodellasalute.verificaC19.model.CertificateStatus
import it.ministerodellasalute.verificaC19.model.PersonModel
import it.ministerodellasalute.verificaC19.ui.custom.HomeFragmentDirections
import it.ministerodellasalute.verificaC19.ui.custom.HomeViewModel
import it.ministerodellasalute.verificaC19.ui.custom.MedicinalProduct
import java.util.*

@ExperimentalUnsignedTypes
@AndroidEntryPoint
class VerificationFragment : Fragment(), View.OnClickListener {

    private val args by navArgs<VerificationFragmentArgs>()
    private val viewModel by viewModels<VerificationViewModel>()
    private val homeViewModel: HomeViewModel by activityViewModels()

    private var _binding: FragmentVerificationNewBinding? = null
    private val binding get() = _binding!!
    private lateinit var certificateModel: CertificateModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVerificationNewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // binding.closeButton.setOnClickListener(this)
        binding.validationDate.text = getString(
            R.string.label_validation_timestamp, Date().time.parseTo(
                FORMATTED_VALIDATION_DATE
            )
        )
        viewModel.certificate.observe(viewLifecycleOwner) { certificate ->
            certificate?.let {
                certificateModel = it
                setPersonData(it.person, it.dateOfBirth)
                setupCertStatusView(it)
            }
        }
        viewModel.inProgress.observe(viewLifecycleOwner) {
            binding.progressBar.isVisible = it
        }
        viewModel.init(args.qrCodeText, true)
    }

    private fun setupCertStatusView(cert: CertificateModel) {
        val certStatus = viewModel.getCertificateStatus(cert)
        setBackgroundColor(certStatus)
        setExpiration(cert)
        //  setPersonDetailsVisibility(certStatus)
        setValidationIcon(certStatus)
        setValidationMainText(certStatus)
        // setValidationSubTextVisibility(certStatus)
        // setValidationSubText(certStatus)
        //setLinkViews(certStatus)
    }

    private fun setExpiration(certificateModel: CertificateModel) {
       // binding.vaccineLayout.isVisible = certificateModel.vaccinations != null
        binding.nameSurname.text = certificateModel.person?.familyName.plus(" ").plus(certificateModel.person?.givenName)
        binding.dateBirth.text = certificateModel.dateOfBirth?.parseFromTo(YEAR_MONTH_DAY, FORMATTED_BIRTHDAY_DATE)
        certificateModel.vaccinations?.let { vaccinations ->
            binding.vaccineType.text = MedicinalProduct.valueOf(vaccinations.last().medicinalProduct.replace("/", "")).code
            binding.numberOFVaccines.text = vaccinations.last().totalSeriesOfDoses.toString()
            binding.dateVaccination.text = vaccinations.last().dateOfVaccination.parseFromTo(YEAR_MONTH_DAY, FORMATTED_BIRTHDAY_DATE)
            binding.expireDate.text = vaccinations.last().expireDate.parseFromTo(YEAR_MONTH_DAY, FORMATTED_BIRTHDAY_DATE)
            binding.delete.setOnClickListener {
                homeViewModel.deleteCertificate(args.qrCodeText)
            }
        }
        certificateModel.tests?.let { vaccinations ->
            binding.vaccineTypeTitle.isVisible = false
            binding.vaccineType.isVisible = false
            binding.numberOFVaccinesTitle.text = "Centro"
            binding.numberOFVaccines.text = vaccinations.last().testingCentre
            binding.dateVaccinationTitle.text = "Data del test"
            binding.dateVaccination.text = vaccinations.last().dateTimeOfCollection?.parseFromTo(YEAR_MONTH_DAY, FORMATTED_BIRTHDAY_DATE)
            binding.expireDate.text = vaccinations.last().expireDate.parseFromTo(YEAR_MONTH_DAY, FORMATTED_BIRTHDAY_DATE)
            binding.delete.setOnClickListener {
                homeViewModel.deleteCertificate(args.qrCodeText)
            }
        }

        certificateModel.recoveryStatements?.let { vaccinations ->
            binding.vaccineTypeTitle.isVisible = false
            binding.vaccineType.isVisible = false
            binding.numberOFVaccinesTitle.isVisible = false
            binding.numberOFVaccines.isVisible = false
            binding.dateVaccinationTitle.text = "Data primo tampone positivo"
            binding.dateVaccination.text = vaccinations.last().dateOfFirstPositiveTest?.parseFromTo(YEAR_MONTH_DAY, FORMATTED_BIRTHDAY_DATE)
            binding.expireDate.text = vaccinations.last().expireDate.parseFromTo(YEAR_MONTH_DAY, FORMATTED_BIRTHDAY_DATE)
            binding.delete.setOnClickListener {
                homeViewModel.deleteCertificate(args.qrCodeText)
            }
        }
    }

    private fun setValidationMainText(certStatus: CertificateStatus) {
        binding.certificateValid.text = when (certStatus) {
            CertificateStatus.VALID -> getString(R.string.certificateValid)
            CertificateStatus.PARTIALLY_VALID -> getString(R.string.certificatePartiallyValid)
            CertificateStatus.NOT_GREEN_PASS -> getString(R.string.certificateNotDCC)
            CertificateStatus.NOT_VALID -> getString(R.string.certificateNonValid)
            CertificateStatus.NOT_VALID_YET -> getString(R.string.certificateNonValidYet)
        }
    }

    private fun setValidationIcon(certStatus: CertificateStatus) {
        binding.checkmark.background =
            ContextCompat.getDrawable(
                requireContext(), when (certStatus) {
                    CertificateStatus.VALID -> R.drawable.ic_valid_cert
                    CertificateStatus.NOT_VALID_YET -> R.drawable.ic_not_valid_yet
                    CertificateStatus.PARTIALLY_VALID -> R.drawable.ic_locally_valid
                    CertificateStatus.NOT_GREEN_PASS -> R.drawable.ic_technical_error
                    else -> R.drawable.ic_invalid
                }
            )
    }


    private fun setBackgroundColor(certStatus: CertificateStatus) {
        binding.verificationBackground.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                when (certStatus) {
                    CertificateStatus.VALID -> R.color.green
                    CertificateStatus.PARTIALLY_VALID -> R.color.blue_bg
                    else -> R.color.red_bg
                }
            )
        )
    }

    private fun setPersonData(person: PersonModel?, dateOfBirth: String?) {
        if (!requireArguments().getBoolean("showMoreDetails")) {
            findNavController().navigate(VerificationFragmentDirections.actionVerificationFragmentToHomeFragment())
        }
        if (args.saveGreenPass) {
            viewModel.persistGreenPass(args.qrCodeText)
        }
        val barcodeEncoder = BarcodeEncoder()
        val bitmap = barcodeEncoder.encodeBitmap(args.qrCodeText, BarcodeFormat.QR_CODE, 300, 300)
        binding.qrcodeImage.setImageBitmap(bitmap)
        binding.qrcodeImage.setOnClickListener {
            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToFullScreenQrCode(args.qrCodeText))
        }

    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.close_button -> findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(qrcodeText: String): VerificationFragment {
            return VerificationFragment().apply {
                arguments = Bundle().apply {
                    putString("qrCodeText", qrcodeText)
                    putBoolean("showMoreDetails", true)
                }
            }
        }
    }
}
