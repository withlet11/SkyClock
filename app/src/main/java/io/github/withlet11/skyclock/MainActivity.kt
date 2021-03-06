/*
 * MainActivity.kt
 *
 * Copyright 2020-2021 Yasuhiro Yamakawa <withlet11@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.withlet11.skyclock

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import io.github.withlet11.skyclock.fragment.LocationSettingFragment
import io.github.withlet11.skyclock.fragment.NorthernSkyClockFragment
import io.github.withlet11.skyclock.fragment.SouthernSkyClockFragment


class MainActivity : AppCompatActivity(), LocationSettingFragment.LocationSettingDialogListener  {
    companion object {
        const val AD_DISPLAY_DURATION = 10000L
    }

    var latitude = 0.0
    private var longitude = 0.0
    var isClockHandsVisible = true
    private var isSouthernSky = false
    // private val handler = Handler()
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private var adView: AdView? = null

    interface LocationChangeObserver {
        fun onLocationChange(latitude: Double, longitude: Double)
    }

    private val observers = mutableListOf<LocationChangeObserver>()

    fun addObserver(observer: LocationChangeObserver) {
        observers.add(observer)
    }

    fun removeObserver(observer: LocationChangeObserver) {
        observers.remove(observer)
    }

    private fun notifyObservers() {
        observers.forEach { it.onLocationChange(latitude, longitude) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.my_toolbar)
        toolbar.setLogo(R.drawable.ic_launcher_foreground)
        toolbar.setTitle(R.string.app_name)

        toolbar.inflateMenu(R.menu.menu_main)

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.item_settings -> {
                    val dialog = LocationSettingFragment()
                    dialog.show(supportFragmentManager, "locationSetting")
                }
                R.id.item_privacy_policy -> {
                    startActivity(Intent(application, PrivacyPolicyActivity::class.java))
                }
                R.id.item_licenses -> {
                    startActivity(Intent(application, LicenseActivity::class.java))
                }
                R.id.item_credits -> {
                    startActivity(Intent(this, OssLicensesMenuActivity::class.java))
                }
                android.R.id.home -> finish()
            }

            true
        }

        // for ads
        MobileAds.initialize(this) {}
        adView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        adView?.loadAd(adRequest)

        loadPreviousSettings()

        val switch: SwitchCompat = findViewById(R.id.view_switch)
        switch.isChecked = isSouthernSky
        switch.setOnCheckedChangeListener { _, b ->
            isSouthernSky = b
            with(getSharedPreferences("observation_position", Context.MODE_PRIVATE).edit()) {
                putBoolean("isSouthernSky", isSouthernSky)
                commit()
            }

            val newFragment =
                (if (b) SouthernSkyClockFragment() else NorthernSkyClockFragment()).apply {
                    arguments = Bundle().apply {
                        putDouble("LATITUDE", latitude)
                        putDouble("LONGITUDE", longitude)
                        putBoolean("CLOCK_HANDS_VISIBILITY", isClockHandsVisible)
                    }
                }

            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.container, newFragment)
            transaction.commit()
        }

        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        val fragment =
            (if (isSouthernSky) SouthernSkyClockFragment() else NorthernSkyClockFragment()).apply {
                arguments = Bundle().apply {
                    putDouble("LATITUDE", latitude)
                    putDouble("LONGITUDE", longitude)
                    putBoolean("CLOCK_HANDS_VISIBILITY", isClockHandsVisible)
                }
            }

        fragmentTransaction.replace(R.id.container, fragment)
        fragmentTransaction.commit()

        val runnable = Runnable {
            val layout: FrameLayout = findViewById(R.id.frameLayoutForAd)
            layout.removeView(adView)
            layout.invalidate()
            adView = null
        }

        handler.postDelayed(runnable, AD_DISPLAY_DURATION)
    }

    override fun onDialogPositiveClick(dialog: DialogFragment) {
        loadPreviousPosition()
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
        // Do nothing
    }

    private fun loadPreviousSettings() {
        val previous = getSharedPreferences("observation_position", Context.MODE_PRIVATE)

        try {
            latitude = previous.getFloat("latitude", 0f).toDouble()
            longitude = previous.getFloat("longitude", 0f).toDouble()
            isSouthernSky = previous.getBoolean("isSouthernSky", false)
        } catch (e: ClassCastException) {
            latitude = 0.0
            longitude = 0.0
            isSouthernSky = false
        } finally {
        }
    }

    private fun loadPreviousPosition() {
        val previous = getSharedPreferences("observation_position", Context.MODE_PRIVATE)

        try {
            latitude = previous.getFloat("latitude", 0f).toDouble()
            longitude = previous.getFloat("longitude", 0f).toDouble()
        } catch (e: ClassCastException) {
            latitude = 0.0
            longitude = 0.0
        } finally {
            notifyObservers()
        }
    }
}