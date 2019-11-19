package com.example.linkedinsigninexample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_details.*

class DetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)

        val linkedinId = intent.getStringExtra("linkedin_id")
        val linkedinFirstName = intent.getStringExtra("linkedin_first_name")
        val linkedinLastName = intent.getStringExtra("linkedin_last_name")
        val linkedinEmail = intent.getStringExtra("linkedin_email")
        val linkedinProfilePicURL = intent.getStringExtra("linkedin_profile_pic_url")
        val linkedinAccessToken = intent.getStringExtra("linkedin_access_token")

        linkedin_id_textview.text = linkedinId
        linkedin_first_name_textview.text = linkedinFirstName
        linkedin_last_name_textview.text = linkedinLastName
        linkedin_email_textview.text = linkedinEmail
        if (linkedinProfilePicURL == "") {
            linkedin_profile_pic_url_textview.text = "Not Exist"
        } else {
            linkedin_profile_pic_url_textview.text = linkedinProfilePicURL
        }
        linkedin_access_token_textview.text = linkedinAccessToken
    }
}
