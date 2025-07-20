// SPDX-License-Identifier: GPL-3.0-or-later
package dev.itsvic.parceltracker.ui.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import dev.itsvic.parceltracker.BuildConfig
import dev.itsvic.parceltracker.CLIPBOARD_PASTE_ENABLED
import dev.itsvic.parceltracker.DEMO_MODE
import dev.itsvic.parceltracker.DHL_API_KEY
import dev.itsvic.parceltracker.PREFERRED_REGION
import dev.itsvic.parceltracker.R
import dev.itsvic.parceltracker.UNMETERED_ONLY
import dev.itsvic.parceltracker.api.ParcelHistoryItem
import dev.itsvic.parceltracker.api.Service
import dev.itsvic.parceltracker.api.Status
import dev.itsvic.parceltracker.dataStore
import dev.itsvic.parceltracker.db.Parcel
import dev.itsvic.parceltracker.enqueueNotificationWorker
import dev.itsvic.parceltracker.sendNotification
import dev.itsvic.parceltracker.ui.components.AboutDialog
import dev.itsvic.parceltracker.ui.components.LogcatButton
import dev.itsvic.parceltracker.ui.theme.ParcelTrackerTheme
import java.time.LocalDateTime
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(onBackPressed: () -> Unit) {
  val context = LocalContext.current
  val demoMode by context.dataStore.data.map { it[DEMO_MODE] == true }.collectAsState(false)
  val unmeteredOnly by
    context.dataStore.data.map { it[UNMETERED_ONLY] == true }.collectAsState(false)
  val clipboardPasteEnabled by
    context.dataStore.data.map { it[CLIPBOARD_PASTE_ENABLED] == true }.collectAsState(false)
  val preferredRegion by
    context.dataStore.data.map { it[PREFERRED_REGION] ?: "" }.collectAsState("")
  val coroutineScope = rememberCoroutineScope()
  var regionDropdownExpanded by remember { mutableStateOf(false) }
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  var aboutDialogOpen by remember { mutableStateOf(false) }

  val dhlApiKey by context.dataStore.data.map { it[DHL_API_KEY] ?: "" }.collectAsState("")

  fun <T> setValue(key: Preferences.Key<T>, value: T) {
    coroutineScope.launch { context.dataStore.edit { it[key] = value } }
  }

  val setUnmeteredOnly: (Boolean) -> Unit = { value ->
    coroutineScope.launch {
      context.dataStore.edit { it[UNMETERED_ONLY] = value }
      context.enqueueNotificationWorker()
    }
  }

  Scaffold(
    topBar = {
      LargeTopAppBar(
        title = { Text(stringResource(R.string.settings)) },
        navigationIcon = {
          IconButton(onClick = onBackPressed) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.go_back))
          }
        },
        scrollBehavior = scrollBehavior,
      )
    },
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
  ) { innerPadding ->
    Column(Modifier.padding(innerPadding).verticalScroll(rememberScrollState())) {
      Row(
        modifier =
          Modifier.clickable { setUnmeteredOnly(unmeteredOnly.not()) }
            .padding(16.dp, 12.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.fillMaxWidth(0.8f)) {
          Text(stringResource(R.string.unmetered_only_setting))
          Text(
            stringResource(R.string.unmetered_only_setting_detail),
            style = MaterialTheme.typography.bodyMedium,
          )
        }
        Switch(checked = unmeteredOnly, onCheckedChange = { setUnmeteredOnly(it) })
      }

      Row(
        modifier =
          Modifier.clickable { setValue(CLIPBOARD_PASTE_ENABLED, clipboardPasteEnabled.not()) }
            .padding(16.dp, 12.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.fillMaxWidth(0.8f)) {
          Text(stringResource(R.string.clipboard_paste_enabled))
          Text(
            stringResource(R.string.clipboard_paste_description),
            style = MaterialTheme.typography.bodyMedium,
          )
        }
        Switch(
          checked = clipboardPasteEnabled,
          onCheckedChange = { setValue(CLIPBOARD_PASTE_ENABLED, it) },
        )
      }

      Column(modifier = Modifier.padding(16.dp, 12.dp).fillMaxWidth()) {
        Text(stringResource(R.string.preferred_region))
        Text(
          stringResource(R.string.preferred_region_description),
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(bottom = 8.dp),
        )

        ExposedDropdownMenuBox(
          expanded = regionDropdownExpanded,
          onExpandedChange = { regionDropdownExpanded = !regionDropdownExpanded },
        ) {
          OutlinedTextField(
            value =
              when (preferredRegion) {
                "international" -> stringResource(R.string.region_international)
                "north_america" -> stringResource(R.string.region_north_america)
                "europe" -> stringResource(R.string.region_europe)
                "asia" -> stringResource(R.string.region_asia)
                "belarus" -> stringResource(R.string.country_belarus)
                "bulgaria" -> stringResource(R.string.country_bulgaria)
                "czech" -> stringResource(R.string.country_czech)
                "uk" -> stringResource(R.string.country_uk)
                "ireland" -> stringResource(R.string.country_ireland)
                "poland" -> stringResource(R.string.country_poland)
                "hungary" -> stringResource(R.string.country_hungary)
                "germany" -> stringResource(R.string.country_germany)
                "italy" -> stringResource(R.string.country_italy)
                "romania" -> stringResource(R.string.country_romania)
                "scandinavia" -> stringResource(R.string.country_scandinavia)
                "ukraine" -> stringResource(R.string.country_ukraine)
                "india" -> stringResource(R.string.country_india)
                "thailand" -> stringResource(R.string.country_thailand)
                else -> stringResource(R.string.region_international)
              },
            onValueChange = {},
            readOnly = true,
            trailingIcon = {
              ExposedDropdownMenuDefaults.TrailingIcon(expanded = regionDropdownExpanded)
            },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
          )

          ExposedDropdownMenu(
            expanded = regionDropdownExpanded,
            onDismissRequest = { regionDropdownExpanded = false },
          ) {
            DropdownMenuItem(
              text = { Text(stringResource(R.string.region_international)) },
              onClick = {
                setValue(PREFERRED_REGION, "international")
                regionDropdownExpanded = false
              },
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.region_north_america)) },
              onClick = {
                setValue(PREFERRED_REGION, "north_america")
                regionDropdownExpanded = false
              },
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.region_europe)) },
              onClick = {
                setValue(PREFERRED_REGION, "europe")
                regionDropdownExpanded = false
              },
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.region_asia)) },
              onClick = {
                setValue(PREFERRED_REGION, "asia")
                regionDropdownExpanded = false
              },
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.country_belarus)) },
              onClick = {
                setValue(PREFERRED_REGION, "belarus")
                regionDropdownExpanded = false
              },
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.country_bulgaria)) },
              onClick = {
                setValue(PREFERRED_REGION, "bulgaria")
                regionDropdownExpanded = false
              },
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.country_czech)) },
              onClick = {
                setValue(PREFERRED_REGION, "czech")
                regionDropdownExpanded = false
              },
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.country_uk)) },
              onClick = {
                setValue(PREFERRED_REGION, "uk")
                regionDropdownExpanded = false
              },
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.country_ireland)) },
              onClick = {
                setValue(PREFERRED_REGION, "ireland")
                regionDropdownExpanded = false
              },
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.country_poland)) },
              onClick = {
                setValue(PREFERRED_REGION, "poland")
                regionDropdownExpanded = false
              },
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.country_hungary)) },
              onClick = {
                setValue(PREFERRED_REGION, "hungary")
                regionDropdownExpanded = false
              },
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.country_germany)) },
              onClick = {
                setValue(PREFERRED_REGION, "germany")
                regionDropdownExpanded = false
              },
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.country_italy)) },
              onClick = {
                setValue(PREFERRED_REGION, "italy")
                regionDropdownExpanded = false
              },
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.country_romania)) },
              onClick = {
                setValue(PREFERRED_REGION, "romania")
                regionDropdownExpanded = false
              },
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.country_scandinavia)) },
              onClick = {
                setValue(PREFERRED_REGION, "scandinavia")
                regionDropdownExpanded = false
              },
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.country_ukraine)) },
              onClick = {
                setValue(PREFERRED_REGION, "ukraine")
                regionDropdownExpanded = false
              },
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.country_india)) },
              onClick = {
                setValue(PREFERRED_REGION, "india")
                regionDropdownExpanded = false
              },
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.country_thailand)) },
              onClick = {
                setValue(PREFERRED_REGION, "thailand")
                regionDropdownExpanded = false
              },
            )
          }
        }
      }

      Text(
        stringResource(R.string.settings_api_keys),
        modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 2.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      OutlinedTextField(
        dhlApiKey,
        { setValue(DHL_API_KEY, it) },
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
        label = { Text(stringResource(R.string.service_dhl)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
      )

      Text(
        AnnotatedString.fromHtml(
          stringResource(R.string.dhl_api_key_flavor_text),
          linkStyles =
            TextLinkStyles(
              style =
                SpanStyle(
                  textDecoration = TextDecoration.Underline,
                  color = MaterialTheme.colorScheme.primary,
                )
            ),
        ),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
      )

      Text(
        stringResource(R.string.settings_experimental),
        modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 2.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Row(
        modifier =
          Modifier.clickable { setValue(DEMO_MODE, demoMode.not()) }
            .padding(16.dp, 12.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.fillMaxWidth(0.8f)) {
          Text(stringResource(R.string.demo_mode))
          Text(
            stringResource(R.string.demo_mode_detail),
            style = MaterialTheme.typography.bodyMedium,
          )
        }
        Switch(checked = demoMode, onCheckedChange = { setValue(DEMO_MODE, it) })
      }

      if (BuildConfig.DEBUG)
        FilledTonalButton(
          onClick = {
            context.sendNotification(
              Parcel(0xf100f, "Cool stuff", "", null, Service.EXAMPLE),
              Status.OutForDelivery,
              ParcelHistoryItem("The courier has picked up the package", LocalDateTime.now(), ""),
            )
          },
          modifier = Modifier.padding(16.dp, 12.dp).fillMaxWidth(),
        ) {
          Text("Send test notification")
        }

      LogcatButton(modifier = Modifier.padding(16.dp, 12.dp).fillMaxWidth())

      FilledTonalButton(
        onClick = { aboutDialogOpen = true },
        modifier = Modifier.padding(16.dp, 12.dp).fillMaxWidth(),
      ) {
        Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.about_app))
        Text(
          text = " ${stringResource(R.string.about_app)}",
          modifier = Modifier.padding(start = 8.dp),
        )
      }

      Text(
        "Parcel ${BuildConfig.VERSION_NAME}",
        modifier = Modifier.padding(16.dp, 8.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    if (aboutDialogOpen) {
      AboutDialog { aboutDialogOpen = false }
    }
  }
}

@Composable
@PreviewLightDark
private fun SettingsViewPreview() {
  ParcelTrackerTheme { SettingsView(onBackPressed = {}) }
}
