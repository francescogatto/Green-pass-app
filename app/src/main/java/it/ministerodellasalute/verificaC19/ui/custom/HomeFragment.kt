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
 *  Created by fgatto on 31/07/21, 17:39
 */

package it.ministerodellasalute.verificaC19.ui.custom

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DimenRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.*
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.github.javiersantos.appupdater.AppUpdater
import com.github.javiersantos.appupdater.enums.Display
import com.github.javiersantos.appupdater.enums.Duration
import com.github.javiersantos.appupdater.enums.UpdateFrom
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.messaging.FirebaseMessaging
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import dagger.hilt.android.AndroidEntryPoint
import it.ministerodellasalute.verificaC19.R
import it.ministerodellasalute.verificaC19.databinding.FHomeBinding
import it.ministerodellasalute.verificaC19.ui.custom.SpidWebView.Companion.TAG
import it.ministerodellasalute.verificaC19.ui.main.verification.VerificationFragment
import it.ministerodellasalute.verificaC19.ui.main.verification.VerificationViewModel
import java.io.File
import java.io.FileOutputStream


@AndroidEntryPoint
class HomeFragment : BaseFragment() {

    private var _binding: FHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModels<VerificationViewModel>()
    private val homeViewModel: HomeViewModel by activityViewModels()


    private val selectImageFromGalleryResult = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(requireContext().contentResolver, it))
            } else {
                MediaStore.Images.Media.getBitmap(requireContext().contentResolver, it)
            }
            val text = scanQRImage(bitmap.copy(Bitmap.Config.ARGB_8888, true))
            if (text == null) {
                Toast.makeText(requireContext(), "C'è stato un problema con la lettura del qrcode", Toast.LENGTH_LONG).show()
            } else {
                findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToVerificationFragment(text!!, true))
            }

        }
    }

    private val selectPdfFromGalleryResult = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val bytes = requireActivity().contentResolver.openInputStream(it)?.readBytes()
            val file = File.createTempFile("green_pass", "tmp")
            file.writeBytes(bytes!!)
            val text = getImagesFromPDF(file, File(requireContext().cacheDir.path + "/Pdf"))
            if (text.isEmpty()) {
                Toast.makeText(requireContext(), "C'è stato un problema con la lettura del qrcode", Toast.LENGTH_LONG).show()
            } else {
                findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToVerificationFragment(text, true))
            }
            file.delete()
        }
    }

    override fun openQrCodeReader() {
        findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToCodeReaderFragment(true))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startReviewManager()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
       /* binding.fabCheckGreenPass.isVisible = false
        binding.fabCheckGreenPass.setOnClickListener {
            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToVerificaC19Fragment())
        }*/
        binding.fabAddGreenPassCamera.setOnClickListener {
            checkCameraPermission()
        }
        binding.fabAddGreenPassFile.setOnClickListener {
            selectImageFromGallery()
        }
        binding.fabAddGreenPassPdf.setOnClickListener {
            selectPdfFromGallery()
        }
        binding.fabInfo.setOnClickListener {
            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToAboutMeFragment())
        }
        binding.fabAddGreenPassSpid.setOnClickListener {
            SpidWebView.newInstance().show(parentFragmentManager, "")
        }
        binding.privacyPolicy.setOnClickListener {

        }
        showReviewWindow()
        updateUI()
        setUpdatePlugin()

        binding.viewPager.apply {
            // You need to retain one page on each side so that the next and previous items are visible
            offscreenPageLimit = 2
            // Add a PageTransformer that translates the next and previous items horizontally
            // towards the center of the screen, which makes them visible
            val nextItemVisiblePx = resources.getDimension(R.dimen.viewpager_next_item_visible)
            val currentItemHorizontalMarginPx = resources.getDimension(R.dimen.viewpager_current_item_horizontal_margin)
            val pageTranslationX = nextItemVisiblePx + currentItemHorizontalMarginPx
            val pageTransformer = ViewPager2.PageTransformer { page: View, position: Float ->
                page.translationX = -pageTranslationX * position
                // Next line scales the item's height. You can remove it if you don't want this effect
                page.scaleY = 1 - (0.25f * Math.abs(position))
                // If you want a fading effect uncomment the next line:
                // page.alpha = 0.25f + (1 - abs(position))
            }
            setPageTransformer(pageTransformer)

            // The ItemDecoration gives the current (centered) item horizontal margin so that
            // it doesn't occupy the whole screen width. Without it the items overlap
            val itemDecoration = HorizontalMarginItemDecoration(
                requireContext(),
                R.dimen.viewpager_current_item_horizontal_margin_bis
            )
            addItemDecoration(itemDecoration)
        }
        homeViewModel.refreshHome.observe(viewLifecycleOwner) {
            updateUI()
        }


        setFragmentResultListener("requestKey") { requestKey, bundle ->
            val result = bundle.getString("url")?.replace("data:image/png;base64,", "")
            val decodedString = Base64.decode(result, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
            val text = scanQRImage(bitmap.copy(Bitmap.Config.ARGB_8888, true))
            if (text.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "C'è stato un problema con la lettura del qrcode", Toast.LENGTH_LONG).show()
            } else {
                findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToVerificationFragment(text, true))
            }
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            Log.d(TAG, task.result)
        }
    }


    fun updateUI() {
        if (viewModel.getGreenPass().isNotEmpty()) {
            binding.coordinator.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue_dark))
            val pagerAdapter = ViewPagerAdapter(viewModel.getGreenPass(), requireActivity())
            binding.viewPager.adapter = pagerAdapter
            binding.cardWelcome.isVisible = false
            binding.logoHome.isVisible = false
        } else {
            binding.coordinator.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
            binding.viewPager.isVisible = false
            binding.cardWelcome.isVisible = true
            binding.logoHome.isVisible = true
        }
    }

    private fun selectImageFromGallery() = selectImageFromGalleryResult.launch("image/*")
    private fun selectPdfFromGallery() = selectPdfFromGalleryResult.launch("application/pdf")

    fun scanQRImage(bMap: Bitmap): String? {
        var contents: String? = null
        val intArray = IntArray(bMap.width * bMap.height)
        //copy pixel data from the Bitmap into the 'intArray' array
        bMap.getPixels(intArray, 0, bMap.width, 0, 0, bMap.width, bMap.height)
        val source = RGBLuminanceSource(bMap.width, bMap.height, intArray)
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        val reader = MultiFormatReader()
        try {
            val result = reader.decode(bitmap)
            contents = result.text
        } catch (e: Exception) {
            Log.e("QrTest", "Error decoding barcode", e)
            e.printStackTrace()
        }
        return contents
    }

    // This method is used to extract all pages in image (PNG) format.
    fun getImagesFromPDF(pdfFilePath: File, destinationDir: File): String {
        // Check if destination already exists then delete destination folder.
        if (destinationDir.exists()) {
            destinationDir.delete()
        }
        // Create empty directory where images will be saved.
        destinationDir.mkdirs()
        // Reading pdf in READ Only mode.rgb
        val fileDescriptor = ParcelFileDescriptor.open(pdfFilePath, ParcelFileDescriptor.MODE_READ_ONLY)
        // Initializing PDFRenderer object.
        val renderer = PdfRenderer(fileDescriptor)
        // Getting total pages count.
        val pageCount = renderer.pageCount
        // Iterating pages
        for (i in 0 until pageCount) {
            // Getting Page object by opening page.
            val page = renderer.openPage(i)
            // Creating empty bitmap. Bitmap.Config can be changed.
            val bitmap = Bitmap.createBitmap(page.width * 3, page.height * 3, Bitmap.Config.ARGB_8888)
            // Creating Canvas from bitmap.
            val canvas = Canvas(bitmap)
            // Set White background color.
            canvas.drawColor(Color.WHITE)
            // Draw bitmap.
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            // Rednder bitmap and can change mode too.
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
            // closing page
            page.close()
            // saving image into sdcard.
            val file = File(destinationDir.absolutePath, "image$i.png")
            // check if file already exists, then delete it.
            if (file.exists()) file.delete()
            // Saving image in PNG format with 100% quality.
            try {
                val out = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                val text = scanQRImage(bitmap)
                out.flush()
                out.close()
                file.delete()
                if (!text.isNullOrEmpty()) {
                    return text
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
        return ""
    }

    private var reviewInfo: ReviewInfo? = null
    private lateinit var reviewManager: ReviewManager
    fun showReviewWindow() {
        try {
            if (viewModel.getGreenPass().isEmpty()) return
            val requestFlow = reviewManager.requestReviewFlow()
            requestFlow.addOnCompleteListener { request ->
                if (request.isSuccessful) {
                    reviewInfo = request.result
                    val flow = reviewManager.launchReviewFlow(requireActivity(), reviewInfo!!)
                    flow.addOnSuccessListener {

                    }
                    flow.addOnFailureListener {

                    }
                    flow.addOnCompleteListener {

                    }
                } else {
                    reviewInfo = null
                }
            }
        } catch (ex: Exception) {
        }
    }

    private fun setUpdatePlugin() {
        AppUpdater(requireContext()).setDisplay(Display.DIALOG).setDuration(Duration.INDEFINITE).setUpdateFrom(UpdateFrom.GOOGLE_PLAY).apply { start() }
    }

    fun startReviewManager() {
        reviewManager = ReviewManagerFactory.create(requireContext())

        //Request a ReviewInfo object ahead of time (Pre-cache)
        val requestFlow = reviewManager.requestReviewFlow()
        requestFlow.addOnCompleteListener { request ->
            if (request.isSuccessful) {
                //Received ReviewInfo object
                reviewInfo = request.result
            } else {
                //Problem in receiving object
                reviewInfo = null
            }
        }
    }

}

/**
 * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
 * sequence.
 */
private class ViewPagerAdapter(val listOfGreenPass: List<String>, fa: FragmentActivity) : FragmentStateAdapter(fa) {
    override fun getItemCount(): Int = listOfGreenPass.size

    override fun createFragment(position: Int): Fragment = VerificationFragment.newInstance(listOfGreenPass[position])
}

/**
 * Adds margin to the left and right sides of the RecyclerView item.
 * Adapted from https://stackoverflow.com/a/27664023/4034572
 * @param horizontalMarginInDp the margin resource, in dp.
 */
class HorizontalMarginItemDecoration(context: Context, @DimenRes horizontalMarginInDp: Int) :
    RecyclerView.ItemDecoration() {

    private val horizontalMarginInPx: Int =
        context.resources.getDimension(horizontalMarginInDp).toInt()

    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
    ) {
        outRect.right = horizontalMarginInPx
        outRect.left = horizontalMarginInPx
    }

}