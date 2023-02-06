package dhruva.microbin.client

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.util.SparseArray
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.valueIterator
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.koushikdutta.ion.Ion
import im.delight.android.webview.AdvancedWebView
import java.io.File
import java.io.FileInputStream
import java.util.*


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), AdvancedWebView.Listener {
    private lateinit var webview: AdvancedWebView
    private lateinit var prefs: SharedPreferences
    private val REQUEST_IMAGE_CAPTURE = 1123

    override fun onPageStarted(url: String?, favicon: Bitmap?) {}
    override fun onPageFinished(url: String?) {}
    override fun onPageError(errorCode: Int, description: String?, failingUrl: String?) {}
    override fun onDownloadRequested(
        url: String?,
        suggestedFilename: String?,
        mimeType: String?,
        contentLength: Long,
        contentDisposition: String?,
        userAgent: String?
    ) {
        if (AdvancedWebView.handleDownload(this, url, suggestedFilename)) {
            Toast.makeText(
                applicationContext, "Download started, check notification", Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(applicationContext, "Unable to Download!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onExternalPageRequest(url: String?) {
        if (AdvancedWebView.Browsers.hasAlternative(this)) {
            AdvancedWebView.Browsers.openUrl(this, url)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webview = findViewById(R.id.webview)
        webview.setListener(this, this)
        webview.setMixedContentAllowed(false)
        val textview = findViewById<EditText>(R.id.textview)
        val qrbutton = findViewById<ImageButton>(R.id.qr)
        prefs = getSharedPreferences(packageName, Context.MODE_PRIVATE)
        prefs.getString("url", "none")?.let {
            if (it == "none" || it == "") {
                webview.loadHtml("<h1>Microbin Client</h1><p>Set your instance url in the bar below</p>")
                textview.hint = getString(R.string.instance_url)
            } else {
                webview.loadUrl(it)
                textview.setText(it)
            }
        }
        qrbutton.setOnClickListener {
            takePhoto()
        }
        textview.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                prefs.edit().putString("url", textview.text.toString()).apply()
                webview.loadUrl(textview.text.toString())
            }
            false
        }
        if (intent.action == Intent.ACTION_SEND) {
            webview.loadHtml("<h1>Uploading!<h1>")
            val recduri: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)
            recduri?.let { uploadFromUri(it) }
        }
    }

    private fun uploadFromUri(recduri: Uri) {
        val cursor = contentResolver.query(recduri, null, null, null, null)
        if (cursor != null) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            val filename = cursor.getString(nameIndex)
            val fd = contentResolver.openFileDescriptor(recduri, "r")
            if (fd != null) {
                val fis = FileInputStream(fd.fileDescriptor)
                val fo = File(cacheDir, filename)
                fo.writeBytes(fis.readBytes())
                fis.close()
                Ion.with(applicationContext).load(prefs.getString("url", "") + "upload")
                    .setMultipartParameter("expiration", "never")
                    .setMultipartParameter("editable", "true")
                    .setMultipartParameter("syntax-highlight", "none")
                    .setMultipartParameter(
                        "content", "Uploaded from Microbin Client for Android"
                    ).setMultipartFile("file", intent.type, fo).asString().withResponse()
                    .setCallback { e, result ->
                        if (e == null) {
                            webview.loadHtml(result.result.toString())
                            fo.delete()
                        } else {
                            Log.i("e: ", e.toString())
                        }
                    }
            }
            fd?.close()
        }
        cursor?.close()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK) {
                val photo = intent?.extras?.get("data") as Bitmap
                detectQrCode(photo)
            }
        } else {
            webview.onActivityResult(requestCode, resultCode, intent)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!webview.onBackPressed()) {
            return
        }
        super.onBackPressed()
    }

    private fun takePhoto() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(applicationContext, "No camera app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun detectQrCode(image: Bitmap) {
        val detector = BarcodeDetector.Builder(this)
            .setBarcodeFormats(Barcode.QR_CODE)
            .build()
        if (!detector.isOperational) {
            Log.d("qr", "Could not setup detector")
            return
        }
        val frame: Frame = Frame.Builder().setBitmap(image).build()
        val barcodes: SparseArray<Barcode> = detector.detect(frame)
        barcodes.valueIterator().forEach {
            webview.loadUrl(it.url.url)
        }
    }
}