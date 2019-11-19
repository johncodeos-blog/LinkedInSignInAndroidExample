package com.example.linkedinsigninexample

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.example.linkedinsigninexample.model.LinkedInEmailModel
import com.example.linkedinsigninexample.model.LinkedInProfileModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.serialization.json.Json
import org.json.JSONObject
import org.json.JSONTokener
import java.io.OutputStreamWriter
import java.lang.ref.WeakReference
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection


class MainActivity : AppCompatActivity() {

    lateinit var linkedinAuthURLFull: String
    lateinit var linkedIndialog: Dialog
    lateinit var linkedinCode: String


    var linkedInId = ""
    var linkedInFirstName = ""
    var linkedInLastName = ""
    var linkedInEmail = ""
    var linkedInProfilePicURL = ""
    var linkedInAccessToken = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val state = "linkedin" + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())

        linkedinAuthURLFull =
            LinkedInConstants.AUTHURL + "?response_type=code&client_id=" + LinkedInConstants.CLIENT_ID + "&scope=" + LinkedInConstants.SCOPE + "&state=" + state + "&redirect_uri=" + LinkedInConstants.REDIRECT_URI

        linkedin_login_btn.setOnClickListener {
            setupLinkedinWebviewDialog(linkedinAuthURLFull)
        }

    }

    // Show LinkedIn login page in a dialog
    @SuppressLint("SetJavaScriptEnabled")
    fun setupLinkedinWebviewDialog(url: String) {
        linkedIndialog = Dialog(this)
        val webView = WebView(this)
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.webViewClient = LinkedInWebViewClient()
        webView.settings.javaScriptEnabled = true
        webView.loadUrl(url)
        linkedIndialog.setContentView(webView)
        linkedIndialog.show()
    }

    // A client to know about WebView navigations
    // For API 21 and above
    @Suppress("OverridingDeprecatedMember")
    inner class LinkedInWebViewClient : WebViewClient() {

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            if (request!!.url.toString().startsWith(LinkedInConstants.REDIRECT_URI)) {
                handleUrl(request.url.toString())

                // Close the dialog after getting the authorization code
                if (request.url.toString().contains("?code=")) {
                    linkedIndialog.dismiss()
                }
                return true
            }
            return false
        }

        // For API 19 and below
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            if (url.startsWith(LinkedInConstants.REDIRECT_URI)) {
                handleUrl(url)

                // Close the dialog after getting the authorization code
                if (url.contains("?code=")) {
                    linkedIndialog.dismiss()
                }
                return true
            }
            return false
        }

        // Check webview url for access token code or error
        private fun handleUrl(url: String) {
            val uri = Uri.parse(url)

            if (url.contains("code")) {
                linkedinCode = uri.getQueryParameter("code") ?: ""
                LinkedinRequestForAccessToken(this@MainActivity, linkedinCode).execute()
            } else if (url.contains("error")) {
                val error = uri.getQueryParameter("error") ?: ""
                Log.e("Error: ", error)
            }
        }
    }


    private class LinkedinRequestForAccessToken
    internal constructor(context: MainActivity, authCode: String) :
        AsyncTask<Void, Void, String>() {

        private val activityReference: WeakReference<MainActivity> = WeakReference(context)

        var code = ""
        val grantType = "authorization_code"

        init {
            this.code = authCode
        }

        val postParams =
            "grant_type=" + grantType + "&code=" + code + "&redirect_uri=" + LinkedInConstants.REDIRECT_URI + "&client_id=" + LinkedInConstants.CLIENT_ID + "&client_secret=" + LinkedInConstants.CLIENT_SECRET

        override fun onPreExecute() {
            //progressBar.show(this@Login, "Please Wait...")
        }

        override fun doInBackground(vararg params: Void): String {
            try {
                val url = URL(LinkedInConstants.TOKENURL)
                val httpsURLConnection = url.openConnection() as HttpsURLConnection
                httpsURLConnection.requestMethod = "POST"
                httpsURLConnection.setRequestProperty(
                    "Content-Type",
                    "application/x-www-form-urlencoded"
                );
                httpsURLConnection.doInput = true
                httpsURLConnection.doOutput = true
                val outputStreamWriter = OutputStreamWriter(httpsURLConnection.outputStream)
                outputStreamWriter.write(postParams)
                outputStreamWriter.flush()
                val response = httpsURLConnection.inputStream.bufferedReader()
                    .use { it.readText() }  // defaults to UTF-8
                val jsonObject = JSONTokener(response).nextValue() as JSONObject

                val accessToken = jsonObject.getString("access_token") //The access token
                Log.d("accessToken is: ", accessToken)

                val expiresIn = jsonObject.getInt("expires_in") //When the access token expires
                Log.d("expires in: ", expiresIn.toString())

                return accessToken
            } catch (e: Exception) {
                e.printStackTrace()
                return ""
            }
        }

        override fun onPostExecute(result: String) {
            // get a reference to the activity if it is still there
            val activity = activityReference.get()
            if (activity == null || activity.isFinishing) return

            // Get user's id, first name, last name, profile pic url
            FetchLinkedInUserProfile(activity, result).execute()
        }
    }

    private class FetchLinkedInUserProfile
    internal constructor(context: MainActivity, accessToken: String) :
        AsyncTask<Void, Void, LinkedInProfileModel?>() {

        private val activityReference: WeakReference<MainActivity> = WeakReference(context)

        var token = ""

        init {
            this.token = accessToken
        }

        val tokenURLFull =
            "https://api.linkedin.com/v2/me?projection=(id,firstName,lastName,profilePicture(displayImage~:playableStreams))&oauth2_access_token=$token"

        override fun doInBackground(vararg params: Void): LinkedInProfileModel? {
            try {
                val url = URL(tokenURLFull)
                val httpsURLConnection = url.openConnection() as HttpsURLConnection
                httpsURLConnection.requestMethod = "GET"
                httpsURLConnection.doInput = true
                httpsURLConnection.doOutput = false
                val response = httpsURLConnection.inputStream.bufferedReader()
                    .use { it.readText() }  // defaults to UTF-8
                val linkedInProfileModel =
                    Json.nonstrict.parse(LinkedInProfileModel.serializer(), response)
                return linkedInProfileModel
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }

        override fun onPostExecute(model: LinkedInProfileModel?) {
            // get a reference to the activity if it is still there
            val activity = activityReference.get()
            if (activity == null || activity.isFinishing) return
            Log.d("LinkedIn Access Token: ", token)
            activity.linkedInAccessToken = token

            // LinkedIn Id
            val linkedinId = model?.id
            Log.d("LinkedIn Id: ", linkedinId ?: "")
            activity.linkedInId = linkedinId ?: ""

            // LinkedIn First Name
            val linkedinFirstName = model?.firstName?.localized?.enUS
            Log.d("LinkedIn First Name: ", linkedinFirstName ?: "")
            activity.linkedInFirstName = linkedinFirstName ?: ""

            // LinkedIn Last Name
            val linkedinLastName = model?.lastName?.localized?.enUS
            Log.d("LinkedIn Last Name: ", linkedinLastName ?: "")
            activity.linkedInLastName = linkedinLastName ?: ""

            // LinkedIn Profile Picture URL
            /*
                 Change row of the 'elements' array to get diffrent size of the profile pic
                 elements[0] = 100x100
                 elements[1] = 200x200
                 elements[2] = 400x400
                 elements[3] = 800x800
            */

            val linkedinProfilePic =
                model?.profilePicture?.displayImage?.elements?.get(2)?.identifiers?.get(0)
                    ?.identifier
            Log.d("LinkedIn Profile URL: ", linkedinProfilePic ?: "Not exists")
            activity.linkedInProfilePicURL = linkedinProfilePic ?: ""

            // Get user's email address
            FetchLinkedInEmailAddress(activity, token).execute()
        }
    }


    private class FetchLinkedInEmailAddress
    internal constructor(context: MainActivity, accessToken: String) :
        AsyncTask<Void, Void, LinkedInEmailModel?>() {
        private val activityReference: WeakReference<MainActivity> = WeakReference(context)

        var token = ""

        init {
            this.token = accessToken
        }

        val tokenURLFull =
            "https://api.linkedin.com/v2/emailAddress?q=members&projection=(elements*(handle~))&oauth2_access_token=$token"

        override fun doInBackground(vararg params: Void): LinkedInEmailModel? {
            try {
                val url = URL(tokenURLFull)
                val httpsURLConnection = url.openConnection() as HttpsURLConnection
                httpsURLConnection.requestMethod = "GET"
                httpsURLConnection.doInput = true
                httpsURLConnection.doOutput = false
                val response = httpsURLConnection.inputStream.bufferedReader()
                    .use { it.readText() }  // defaults to UTF-8
                val linkedInProfileModel =
                    Json.nonstrict.parse(LinkedInEmailModel.serializer(), response)
                return linkedInProfileModel
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }

        override fun onPostExecute(model: LinkedInEmailModel?) {
            // get a reference to the activity if it is still there
            val activity = activityReference.get()
            if (activity == null || activity.isFinishing) return

            // LinkedIn Email
            val linkedinEmail = model?.elements?.get(0)?.elementHandle?.emailAddress
            Log.d("LinkedIn Email: ", linkedinEmail ?: "")
            activity.linkedInEmail = linkedinEmail ?: ""

            val myIntent = Intent(activity, DetailsActivity::class.java)
            myIntent.putExtra("linkedin_id", activity.linkedInId)
            myIntent.putExtra("linkedin_first_name", activity.linkedInFirstName)
            myIntent.putExtra("linkedin_last_name", activity.linkedInLastName)
            myIntent.putExtra("linkedin_email", activity.linkedInEmail)
            myIntent.putExtra("linkedin_profile_pic_url", activity.linkedInProfilePicURL)
            myIntent.putExtra("linkedin_access_token", activity.linkedInAccessToken)
            activity.startActivity(myIntent)
        }
    }
}
