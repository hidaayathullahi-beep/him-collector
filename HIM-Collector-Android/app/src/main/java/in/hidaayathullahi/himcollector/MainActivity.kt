package `in`.hidaayathullahi.himcollector

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.io.OutputStream
import java.util.UUID

/**
 * Thin shell for the HIM Collector app.
 *  - Opens the live collector web app directly (no test page), so collectors have ONE door
 *    and printing always goes through the native Bluetooth bridge (never RawBT).
 *  - Exposes window.HIMNative so the web app can pick a printer and print the Malayalam slip.
 * The web app renders the slip to an image and sends ready-made ESC/POS bytes; this native
 * code only opens a Bluetooth link to the printer and writes those bytes. It never touches text.
 */
class MainActivity : ComponentActivity() {

    private lateinit var web: WebView
    private val SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val PREFS = "him"
    private val KEY_MAC = "printer_mac"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureBtPermission()

        web = WebView(this)
        setContentView(web)

        val s = web.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.databaseEnabled = true
        s.cacheMode = WebSettings.LOAD_DEFAULT
        s.mediaPlaybackRequiresUserGesture = false

        web.webChromeClient = WebChromeClient()
        web.webViewClient = WebViewClient() // keep links inside the app
        web.addJavascriptInterface(Bridge(), "HIMNative")

        // Open the live collector app directly. Collectors never see the test page,
        // so printing always uses the native bridge below (no RawBT, no popup).
        web.loadUrl(getString(R.string.collector_url))
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    private fun ensureBtPermission() {
        if (Build.VERSION.SDK_INT >= 31) {
            val perm = Manifest.permission.BLUETOOTH_CONNECT
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(perm), 1)
            }
        }
    }

    inner class Bridge {
        @JavascriptInterface
        fun isNative(): Boolean = true

        @JavascriptInterface
        fun collectorUrl(): String = getString(R.string.collector_url)

        @JavascriptInterface
        fun selectPrinter() = runOnUiThread { showPicker() }

        /** base64 = the finished ESC/POS bytes built by the web page. */
        @JavascriptInterface
        fun print(base64: String) {
            Thread {
                try {
                    val mac = prefs().getString(KEY_MAC, null)
                    if (mac.isNullOrEmpty()) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Pick the printer first", Toast.LENGTH_SHORT).show()
                            showPicker()
                        }
                        callback(false, "No printer selected")
                        return@Thread
                    }
                    val bytes = Base64.decode(base64, Base64.DEFAULT)
                    writeToPrinter(mac, bytes)
                    callback(true, "")
                } catch (e: Exception) {
                    callback(false, e.message ?: "Print failed")
                }
            }.start()
        }
    }

    private fun prefs() = getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @SuppressLint("MissingPermission")
    private fun writeToPrinter(mac: String, bytes: ByteArray) {
        val adapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
            ?: throw IllegalStateException("Bluetooth not available")
        val dev = adapter.getRemoteDevice(mac)
        var sock: BluetoothSocket? = null
        try {
            sock = dev.createRfcommSocketToServiceRecord(SPP)
            sock.connect()
            val os: OutputStream = sock.outputStream
            os.write(bytes)
            os.flush()
            Thread.sleep(400) // let the printer finish before we drop the link
        } finally {
            try { sock?.close() } catch (_: Exception) {}
        }
    }

    private fun callback(ok: Boolean, msg: String) {
        runOnUiThread {
            web.evaluateJavascript(
                "window.HIMNativeResult && window.HIMNativeResult($ok, ${JSONObject.quote(msg)});",
                null
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun showPicker() {
        val adapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (adapter == null || !adapter.isEnabled) {
            Toast.makeText(this, "Turn on Bluetooth first", Toast.LENGTH_LONG).show()
            return
        }
        val devices = adapter.bondedDevices.toList()
        if (devices.isEmpty()) {
            Toast.makeText(this, "Pair the printer in Android Bluetooth settings first", Toast.LENGTH_LONG).show()
            return
        }
        val names = devices.map { (it.name ?: "Unknown") + "\n" + it.address }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select your printer")
            .setItems(names) { _, i ->
                prefs().edit().putString(KEY_MAC, devices[i].address).apply()
                Toast.makeText(this, "Printer set: ${devices[i].name}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
}
