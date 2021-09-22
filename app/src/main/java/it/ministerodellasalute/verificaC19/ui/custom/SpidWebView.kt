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
 *  Created by fgatto on 26/08/21, 17:23
 */

package it.ministerodellasalute.verificaC19.ui.custom

import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.webkit.*
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import it.ministerodellasalute.verificaC19.R

class SpidWebView : DialogFragment() {

    interface  SpidWebViewListener {
        fun onSuccess(): String
        fun onFail(): String
    }

    lateinit var webView: WebView
    private var spinner: ProgressDialog? = null

    override fun getTheme() = R.style.DialogFull

    private val keyEventListener = DialogInterface.OnKeyListener { dialog, keyCode, event ->
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    if (webView.canGoBack()) {
                        webView.goBack()
                    } else {
                        dismiss()
                    }
                    true
                }
            }
        }
        true
    }

    override fun dismiss() {
        webView.stopLoading()
        dismissAllowingStateLoss()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.setOnKeyListener(keyEventListener)
        return inflater.inflate(R.layout.d_webview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        spinner = ProgressDialog(context)
        spinner!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
        spinner!!.setMessage(context?.getString(R.string.loading))
        // Stops people from accidently cancelling the login flow
        spinner!!.setCanceledOnTouchOutside(false)
        spinner!!.setOnCancelListener { cancel() }
        spinner?.show()
        // First calculate how big the frame layout should be
        setUpWebView()
    }

    fun cancel() {
        dismiss()
    }

    private fun setUpWebView() {
        webView = requireView().findViewById(R.id.webview)
        webView.isVerticalScrollBarEnabled = true
        webView.isHorizontalScrollBarEnabled = true
        webView.webViewClient = DialogWebViewClient()
        webView.settings.databaseEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowContentAccess = true
        webView.settings.allowFileAccess = true
        webView.settings.javaScriptEnabled = true
        webView.settings.javaScriptCanOpenWindowsAutomatically = true
        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.settings.allowFileAccessFromFileURLs = false
        webView.settings.allowUniversalAccessFromFileURLs = false
        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            if (!url.startsWith("data")) {
                Toast.makeText(requireContext(), "Clicca su \"Scarica QRCode\" per importare il Green Pass", Toast.LENGTH_LONG).show()
            } else {
                setFragmentResult("requestKey", bundleOf("url" to url))
                dismissAllowingStateLoss()
            }
        }
        // webView.addJavascriptInterface(LoadListener(), "HTMLOUT")

        val cookieManager = CookieManager.getInstance()
        cookieManager.acceptCookie()
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        //webView.postUrl(startUrl, data.toByteArray())
        webView.loadUrl("https://www.dgc.gov.it/spa/auth/login")
    }

    private inner class DialogWebViewClient : WebViewClient() {

        override fun onReceivedError(
            view: WebView,
            errorCode: Int,
            description: String,
            failingUrl: String
        ) {
            super.onReceivedError(view, errorCode, description, failingUrl)
            Log.d(TAG, "ErrorCode: $errorCode, FailingUrl: $failingUrl")
        }


        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val appPackageName = "it.ipzs.cieid"
            val className = "it.ipzs.cieid.BaseActivity"
            // The webView is about to navigate to the specified host.
            if (request?.url.toString().contains("OpenApp")) {

                val intent = Intent()
                try {
                    intent.setClassName(appPackageName, className)
                    //settare la url caricata dalla webview su /OpenApp
                    intent.data = request?.url
                    intent.action = Intent.ACTION_VIEW
                    startActivityForResult(intent, 0)

                } catch (a: ActivityNotFoundException) {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
                        )
                    )
                }
                return true

            }
            return super.shouldOverrideUrlLoading(view, request)
        }



        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            super.onReceivedSslError(view, handler, error)
            handler.cancel()
           // sendErrorToListener(SPIDDialogException(null, ERROR_FAILED_SSL_HANDSHAKE, null))
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            Log.d(TAG, "Webview loading URL: $url")
            super.onPageStarted(view, url, favicon)
            if (url.contains("https://www.dgc.gov.it/spa/auth/home", true))
                Toast.makeText(requireContext(), "Scarica il Green Pass come QRCode per importarlo", Toast.LENGTH_LONG).show()
        }

        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)
            Log.d(TAG, "onReceivedError: $error")
        }

        override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
            super.onReceivedHttpError(view, request, errorResponse)
            Log.d(TAG, "onReceivedHttpError: ${request?.url}")
            Log.d(TAG, "onReceivedHttpError: ${errorResponse?.statusCode}")

        }

        var lastCookie = ""
        var isPosteId = false
        override fun onPageFinished(view: WebView, url: String) {
            Log.d(TAG, "On page finished: $url")
            super.onPageFinished(view, url)
            try {
                if (spinner?.isShowing == true)
                    spinner?.dismiss()
            } catch (ex: java.lang.Exception) {
            }
            webView.visibility = View.VISIBLE
        }

        var lastHeaders: MutableMap<String, String>? = null
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            if (request.url.toString().contains("/OpenApp?")) {

                // val intent = Intent(Intent.ACTION_VIEW, Uri.parse(request.url.toString()))
                // startActivity(context, intent, null)
            }
            if (request.url.toString().contains("areariservata", true)) {
                Log.d(TAG, "HEADERS: " + request.requestHeaders)
                //  if (request.requestHeaders.containsKey("Authorization")) {
                lastHeaders = request.requestHeaders.toMutableMap()
                //  }
            }
            return super.shouldInterceptRequest(view, request)
        }
    }

    companion object {
        const val TAG = "Web"

        fun newInstance()  = SpidWebView()

    }

}