package dev.itsvic.parceltracker

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import dev.itsvic.parceltracker.ui.theme.ParcelTrackerTheme
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader

class LogcatDumperActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      ParcelTrackerTheme {
        Box(modifier = Modifier.background(color = MaterialTheme.colorScheme.background)) {
          Text("Dumping logs, please wait...")
        }
      }
    }

    launcher.launch("parcel-logcat")
  }

  private val launcher =
      registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        Log.d("LogcatDumper", "Got file path $uri, starting...")
        val contentResolver = applicationContext.contentResolver
        uri?.let { uri ->
          contentResolver.openFileDescriptor(uri, "w")?.use { pfd ->
            FileOutputStream(pfd.fileDescriptor).bufferedWriter().use { writer ->
              val process = Runtime.getRuntime().exec("logcat -d")
              BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.copyTo(writer)
              }
            }
          } ?: Log.e("LogcatDumper", "outputStream is null")
        }
        Log.d("LogcatDumper", "Done.")
        finish()
      }
}
