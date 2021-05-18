package com.mittylabs.elaps.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mittylabs.elaps.R
import com.mittylabs.elaps.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    companion object {
        fun launchingIntent(context: Context): Intent {
            return Intent(context, SettingsActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appbar.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
    }


    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}