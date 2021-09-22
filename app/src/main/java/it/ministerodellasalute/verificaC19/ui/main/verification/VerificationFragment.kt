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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import dgca.verifier.app.decoder.model.VerificationResult
import it.ministerodellasalute.verificaC19.FORMATTED_BIRTHDAY_DATE
import it.ministerodellasalute.verificaC19.R
import it.ministerodellasalute.verificaC19.YEAR_MONTH_DAY
import it.ministerodellasalute.verificaC19.data.local.Preferences
import it.ministerodellasalute.verificaC19.databinding.FragmentVerificationBinding
import it.ministerodellasalute.verificaC19.model.CertificateModel
import it.ministerodellasalute.verificaC19.model.PersonModel
import it.ministerodellasalute.verificaC19.model.TestResult
import it.ministerodellasalute.verificaC19.parseFromTo
import it.ministerodellasalute.verificaC19.ui.custom.HomeFragmentDirections
import it.ministerodellasalute.verificaC19.ui.custom.HomeViewModel
import it.ministerodellasalute.verificaC19.ui.custom.MedicinalProduct
import java.lang.Exception
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ofPattern
import java.util.*
import javax.inject.Inject
import kotlin.properties.Delegates

@AndroidEntryPoint
class VerificationFragment : Fragment(), View.OnClickListener {

    private val args by navArgs<VerificationFragmentArgs>()
    private val viewModel by viewModels<VerificationViewModel>()
    private var _binding: FragmentVerificationBinding? = null
    private val binding get() = _binding!!
    private lateinit var certificateModel: CertificateModel
    private val homeViewModel: HomeViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVerificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.closeButton.setOnClickListener(this)
        binding.nextQrButton.setOnClickListener(this)
        binding.homeButton.setOnClickListener(this)

        viewModel.verificationResult.observe(viewLifecycleOwner) {
            setCertStatusUI(it)
        }

        viewModel.certificate.observe(viewLifecycleOwner) { certificate ->
            certificate?.let {
                certificateModel = it
                setPersonData(it.person, it.dateOfBirth)
            }
        }

        viewModel.inProgress.observe(viewLifecycleOwner) {
            binding.progressBar.isVisible = it
        }

        viewModel.init(args.qrCodeText,!requireArguments().getBoolean("showMoreDetails"))
    }

    private fun clearExtraTime(strDateTime: String): String {
        try {

            if (strDateTime.contains("T")) {
                return strDateTime.substring(0, strDateTime.indexOf("T"))
            }
            return strDateTime
        }
        catch (e : Exception)
        {
            return strDateTime
        }
    }

    private fun isAnyTestExpired(it: CertificateModel): TestExpiryValues {

        it.recoveryStatements?.let {

            try {

                val startDate: LocalDate = LocalDate.parse(clearExtraTime(it.last().certificateValidFrom))

                val endDate: LocalDate = LocalDate.parse(clearExtraTime(it.last().certificateValidUntil))
                it.last().expireDate = endDate.toString()
                Log.d("dates", "start:" + startDate.toString() +" end: " +endDate.toString())
                return when {
                    startDate.isAfter(LocalDate.now()) -> TestExpiryValues.FUTURE
                    LocalDate.now().isAfter(endDate) -> TestExpiryValues.EXPIRED
                    else -> TestExpiryValues.VALID
                }
            } catch (e: Exception) {
                return TestExpiryValues.TECHNICAL_ERROR
            }

            return TestExpiryValues.EXPIRED
        }
        it.tests?.let {

            if (it.last().resultType == TestResult.DETECTED) {
                return TestExpiryValues.TECHNICAL_ERROR
            }
            try {

                val odtDateTimeOfCollection = OffsetDateTime.parse(it.last().dateTimeOfCollection)
                val ldtDateTimeOfCollection = odtDateTimeOfCollection.toLocalDateTime()

                val startDate: LocalDateTime =
                    ldtDateTimeOfCollection
                        .plusHours(Integer.parseInt(viewModel.getRapidTestStartHour()).toLong())

                val endDate: LocalDateTime =
                    ldtDateTimeOfCollection
                        .plusHours(Integer.parseInt(viewModel.getRapidTestEndHour()).toLong())
                it.last().expireDate = endDate.toString()
                Log.d("dates", "start:" + startDate.toString() +" end: " +endDate.toString())
                return when {
                    startDate.isAfter(LocalDateTime.now()) -> TestExpiryValues.FUTURE
                    LocalDateTime.now().isAfter(endDate) -> TestExpiryValues.EXPIRED
                    else -> TestExpiryValues.VALID
                }
            } catch (e: Exception) {
                return TestExpiryValues.TECHNICAL_ERROR
            }

            return TestExpiryValues.EXPIRED
        }

        it.vaccinations?.let {
            try {
                when {
                    it.last().doseNumber < it.last().totalSeriesOfDoses -> {
                        val startDate: LocalDate = LocalDate.parse(clearExtraTime(it.last().dateOfVaccination))
                            .plusDays(Integer.parseInt(viewModel.getVaccineStartDayNotComplete(it.last().medicinalProduct)).toLong())

                        val endDate: LocalDate = LocalDate.parse(clearExtraTime(it.last().dateOfVaccination))
                            .plusDays(Integer.parseInt(viewModel.getVaccineEndDayNotComplete(it.last().medicinalProduct)).toLong())
                        Log.d("dates", "start:" + startDate.toString() +" end: " +endDate.toString())
                        it.last().expireDate = endDate.toString()
                        return when {
                            startDate.isAfter(LocalDate.now()) -> TestExpiryValues.FUTURE
                            LocalDate.now().isAfter(endDate) -> TestExpiryValues.EXPIRED
                            else -> TestExpiryValues.VALID
                        }
                    }
                    it.last().doseNumber == it.last().totalSeriesOfDoses -> {
                        val startDate: LocalDate = LocalDate.parse(clearExtraTime(it.last().dateOfVaccination))
                            .plusDays(Integer.parseInt(viewModel.getVaccineStartDayComplete(it.last().medicinalProduct)).toLong())

                        val endDate: LocalDate = LocalDate.parse(clearExtraTime(it.last().dateOfVaccination))
                            .plusDays(Integer.parseInt(viewModel.getVaccineEndDayComplete(it.last().medicinalProduct)).toLong())
                        Log.d("dates", "start:" + startDate.toString() +" end: " +endDate.toString())
                        it.last().expireDate = endDate.toString()
                        return when {
                            startDate.isAfter(LocalDate.now()) -> TestExpiryValues.FUTURE
                            LocalDate.now().isAfter(endDate) -> TestExpiryValues.EXPIRED
                            else -> TestExpiryValues.VALID
                        }
                    }
                    it.last().doseNumber > it.last().totalSeriesOfDoses -> {
                        return TestExpiryValues.TECHNICAL_ERROR
                    }
                    else ->{
                        TestExpiryValues.EXPIRED
                    }
                }
            } catch (e: Exception) {
                return TestExpiryValues.TECHNICAL_ERROR
            }
        }

        return TestExpiryValues.EXPIRED
    }

    private enum class TestExpiryValues {
        TECHNICAL_ERROR,
        EXPIRED,
        FUTURE,
        VALID
    }

    private fun setCertStatusUI(verificationResult: VerificationResult) {
        if (verificationResult.isValid()) {
            val certificateValidityResult = isAnyTestExpired(certificateModel)
            if (certificateValidityResult == TestExpiryValues.VALID) {
                if(args.saveGreenPass) {
                    viewModel.persistGreenPass(args.qrCodeText)
                }
                if(requireArguments().getBoolean("showMoreDetails")) {
                    val barcodeEncoder = BarcodeEncoder()
                    val bitmap = barcodeEncoder.encodeBitmap(args.qrCodeText, BarcodeFormat.QR_CODE, 300, 300)
                    binding.containerCustomPersonDetails.isVisible =  true
                    binding.qrcodeImage.setImageBitmap(bitmap)
                    binding.qrcodeImage.setOnClickListener {
                        findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToFullScreenQrCode(args.qrCodeText))
                    }
                    binding.checkmark.isInvisible =  true
                    binding.closeButton.isVisible= false
                    binding.subtitleText.isVisible = false
                    binding.nextQrButton.isInvisible = true
                    binding.homeButton.isVisible = false
                    //binding.testDebug.isVisible = true
                    //binding.testDebug.text = certificateModel.toString()
                    binding.certificateValid.isVisible = false
                    binding.vaccineLayout.isVisible = certificateModel.vaccinations!=null
                    certificateModel.vaccinations?.let { vaccinations->
                        binding.nameSurname.text = certificateModel.person.familyName.plus(" ").plus(certificateModel.person.givenName)
                        binding.vaccineType.text =  MedicinalProduct.valueOf(vaccinations.last().medicinalProduct.replace("/","")).code
                        binding.numberOFVaccines.text = vaccinations.last().totalSeriesOfDoses.toString()
                        binding.dateBirth.text = certificateModel.dateOfBirth.parseFromTo(YEAR_MONTH_DAY, FORMATTED_BIRTHDAY_DATE)
                        binding.dateVaccination.text = vaccinations.last().dateOfVaccination.parseFromTo(YEAR_MONTH_DAY, FORMATTED_BIRTHDAY_DATE)
                        binding.expireDate.text =vaccinations.last().expireDate.parseFromTo(YEAR_MONTH_DAY, FORMATTED_BIRTHDAY_DATE)
                        binding.delete.setOnClickListener {
                            homeViewModel.deleteCertificate(args.qrCodeText)
                        }
                    }


                } else {
                    binding.containerCustomPersonDetails.isVisible =  false
                    binding.containerPersonDetails.visibility = View.VISIBLE
                    binding.checkmark.background = ContextCompat.getDrawable(requireContext(), R.drawable.ic_checkmark_filled)
                    binding.certificateValid.text = getString(R.string.certificateValid)
                    binding.subtitleText.text = getString(R.string.subtitle_text)
                    binding.nextQrButton.text = getString(R.string.nextQR)
                }
            } else {
                setTestErrorMessage(certificateValidityResult)
            }
        } else {
            binding.containerPersonDetails.visibility = View.GONE
            binding.checkmark.background = ContextCompat.getDrawable(requireContext(), if (verificationResult.cborDecoded) R.drawable.ic_misuse else R.drawable.ic_warning)
            binding.certificateValid.text = if (verificationResult.cborDecoded) getString(R.string.certificateNonValid) else getString(R.string.scanError)
            binding.subtitleText.text = if (verificationResult.cborDecoded) getString(R.string.subtitle_text_nonvalid) else getString(R.string.subtitle_text_notDCC)
        }
    }

    private fun setTestErrorMessage(certificateValidityResult: TestExpiryValues) {
        binding.containerPersonDetails.visibility = View.VISIBLE
        binding.checkmark.background = ContextCompat.getDrawable(requireContext(), R.drawable.ic_misuse)
        binding.certificateValid.text = getString(R.string.certificateNonValid)
        binding.subtitleText.text =
            when (certificateValidityResult) {
                TestExpiryValues.TECHNICAL_ERROR -> getString(R.string.subtitle_text_technicalError)
                TestExpiryValues.EXPIRED -> getString(R.string.subtitle_text_expired)
                TestExpiryValues.FUTURE -> getString(R.string.subtitle_text_future)
                else -> getString(R.string.subtitle_text_technicalError)
            }
    }

    private fun setPersonData(person: PersonModel, dateOfBirth: String) {
        binding.nameText.text = person.familyName.plus(" ").plus(person.givenName)
        binding.nameStandardisedText.text = ""
        binding.birthdateText.text =
            dateOfBirth.parseFromTo(YEAR_MONTH_DAY, FORMATTED_BIRTHDAY_DATE)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.close_button, R.id.home_button -> findNavController().navigate(VerificationFragmentDirections.actionVerificationFragmentToHomeFragment())
            R.id.next_qr_button -> findNavController().navigate(VerificationFragmentDirections.actionVerificationFragmentToHomeFragment())
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