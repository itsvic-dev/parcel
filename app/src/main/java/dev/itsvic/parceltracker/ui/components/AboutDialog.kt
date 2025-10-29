// SPDX-License-Identifier: GPL-3.0-or-later
package dev.itsvic.parceltracker.ui.components

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import dev.itsvic.parceltracker.BuildConfig
import dev.itsvic.parceltracker.R
import dev.itsvic.parceltracker.ui.theme.ParcelTrackerTheme

@Composable
fun AboutDialog(onDismissRequest: () -> Unit) {
  val context = LocalContext.current

  Dialog(onDismissRequest = onDismissRequest) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
      Column(modifier = Modifier.padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
              painter = painterResource(R.drawable.icon_foreground),
              contentDescription = null,
              modifier = Modifier.size(80.dp),
              tint = Color.Unspecified
          )
          Spacer(Modifier.width(16.dp))
          Column(
              horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
                text = "Parcel",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = BuildConfig.VERSION_NAME,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { context.openLinkInBrowser("https://github.com/itsvic-dev/parcel".toUri()) },
            modifier = Modifier.fillMaxWidth()) {
              Icon(painterResource(R.drawable.github), stringResource(R.string.repository))
              Spacer(Modifier.width(8.dp))
              Text(stringResource(R.string.repository))
            }

        Button(
            onClick = {
              context.openLinkInBrowser(
                  "https://github.com/itsvic-dev/parcel/blob/master/LICENSE.md".toUri())
            },
            modifier = Modifier.fillMaxWidth()) {
              Icon(painterResource(R.drawable.license_24px), stringResource(R.string.license))
              Spacer(Modifier.width(8.dp))
              Text(stringResource(R.string.license))
            }

        Button(
            onClick = {
              context.openLinkInBrowser("https://github.com/sponsors/itsvic-dev".toUri())
            },
            modifier = Modifier.fillMaxWidth()) {
              Icon(
                  painterResource(R.drawable.volunteer_activism_24px),
                  stringResource(R.string.donate))
              Spacer(Modifier.width(8.dp))
              Text(stringResource(R.string.donate))
            }
      }
    }
  }
}

@Composable
@PreviewLightDark
private fun AboutDialogPreview() {
  ParcelTrackerTheme { AboutDialog(onDismissRequest = {}) }
}

fun Context.openLinkInBrowser(url: Uri) {
  //    val browserIntent = Intent(Intent.ACTION_VIEW, url)
  //    startActivity(browserIntent)
  val customTabsIntent = CustomTabsIntent.Builder().apply { setShowTitle(true) }.build()
  customTabsIntent.launchUrl(this, url)
}
