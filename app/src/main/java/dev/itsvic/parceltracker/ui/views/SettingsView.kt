// SPDX-License-Identifier: GPL-3.0-or-later
package dev.itsvic.parceltracker.ui.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
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
import dev.itsvic.parceltracker.dataStore
import dev.itsvic.parceltracker.enqueueNotificationWorker
import dev.itsvic.parceltracker.ui.components.AboutDialog
import dev.itsvic.parceltracker.ui.theme.ParcelTrackerTheme
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView() {
  val context = LocalContext.current
  val demoMode by context.dataStore.data.map { it[DEMO_MODE] == true }.collectAsState(false)
  val unmeteredOnly by
    context.dataStore.data.map { it[UNMETERED_ONLY] == true }.collectAsState(false)
  val clipboardPasteEnabled by
    context.dataStore.data.map { it[CLIPBOARD_PASTE_ENABLED] == true }.collectAsState(false)
  val preferredRegion by
    context.dataStore.data.map { it[PREFERRED_REGION] ?: "" }.collectAsState("")
  val dhlApiKey by context.dataStore.data.map { it[DHL_API_KEY] ?: "" }.collectAsState("")
  
  val coroutineScope = rememberCoroutineScope()
  var regionDropdownExpanded by remember { mutableStateOf(false) }
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  var aboutDialogOpen by remember { mutableStateOf(false) }

  val testPackageName = stringResource(R.string.settings_test_package_name)
  val testPackageStatus = stringResource(R.string.settings_test_package_status)

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
        scrollBehavior = scrollBehavior,
      )
    },
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .padding(innerPadding)
        .padding(horizontal = 16.dp)
        .verticalScroll(rememberScrollState())
    ) {
      Spacer(modifier = Modifier.height(8.dp))

      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { aboutDialogOpen = true },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
                painter = painterResource(R.drawable.icon_foreground),
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = Color.Unspecified
              )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = stringResource(R.string.settings_version_label, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
          Spacer(modifier = Modifier.height(8.dp))
        }
      }
      
      Spacer(modifier = Modifier.height(16.dp))

      Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
          ) {
            Icon(
              painterResource(R.drawable.ic_networkwifi),
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary
            )
            Text(
              text = stringResource(R.string.settings_network),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(start = 8.dp)
            )
          }
          
          Row(
            modifier = Modifier
              .clickable { setUnmeteredOnly(unmeteredOnly.not()) }
              .padding(vertical = 8.dp)
              .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.fillMaxWidth(0.8f)) {
              Text(stringResource(R.string.unmetered_only_setting))
              Text(
                stringResource(R.string.unmetered_only_setting_detail),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Switch(checked = unmeteredOnly, onCheckedChange = { setUnmeteredOnly(it) })
          }
          
          Row(
            modifier = Modifier
              .clickable { setValue(CLIPBOARD_PASTE_ENABLED, clipboardPasteEnabled.not()) }
              .padding(vertical = 8.dp)
              .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.fillMaxWidth(0.8f)) {
              Text(stringResource(R.string.clipboard_paste_enabled))
              Text(
                stringResource(R.string.clipboard_paste_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Switch(
              checked = clipboardPasteEnabled,
              onCheckedChange = { setValue(CLIPBOARD_PASTE_ENABLED, it) },
            )
          }
        }
      }
      
      Spacer(modifier = Modifier.height(16.dp))

      Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
          ) {
            Icon(
              painterResource(R.drawable.ic_language),
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary
            )
            Text(
              text = stringResource(R.string.settings_region),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(start = 8.dp)
            )
          }
          
          Text(
            stringResource(R.string.preferred_region_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
          )

          ExposedDropdownMenuBox(
            expanded = regionDropdownExpanded,
            onExpandedChange = { regionDropdownExpanded = !regionDropdownExpanded },
          ) {
            OutlinedTextField(
              value = when (preferredRegion) {
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
              label = { Text(stringResource(R.string.settings_region)) },
              leadingIcon = {
                Icon(
                  painterResource(R.drawable.ic_language),
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary
                )
              },
              trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = regionDropdownExpanded)
              },
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.menuAnchor().fillMaxWidth(),
            )

            ExposedDropdownMenu(
              expanded = regionDropdownExpanded,
              onDismissRequest = { regionDropdownExpanded = false },
            ) {
              listOf(
                "international" to R.string.region_international,
                "north_america" to R.string.region_north_america,
                "europe" to R.string.region_europe,
                "asia" to R.string.region_asia,
                "belarus" to R.string.country_belarus,
                "bulgaria" to R.string.country_bulgaria,
                "czech" to R.string.country_czech,
                "uk" to R.string.country_uk,
                "ireland" to R.string.country_ireland,
                "poland" to R.string.country_poland,
                "hungary" to R.string.country_hungary,
                "germany" to R.string.country_germany,
                "italy" to R.string.country_italy,
                "romania" to R.string.country_romania,
                "scandinavia" to R.string.country_scandinavia,
                "ukraine" to R.string.country_ukraine,
                "india" to R.string.country_india,
                "thailand" to R.string.country_thailand
              ).forEach { (key, stringRes) ->
                DropdownMenuItem(
                  text = { Text(stringResource(stringRes)) },
                  onClick = {
                    setValue(PREFERRED_REGION, key)
                    regionDropdownExpanded = false
                  },
                )
              }
            }
          }
        }
      }
      
      Spacer(modifier = Modifier.height(16.dp))

      Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
          ) {
            Icon(
              painterResource(R.drawable.ic_vpnkey),
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary
            )
            Text(
              text = stringResource(R.string.settings_api_keys),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(start = 8.dp)
            )
          }
          
          OutlinedTextField(
            value = dhlApiKey,
            onValueChange = { setValue(DHL_API_KEY, it) },
            label = { Text(stringResource(R.string.settings_dhl_api_key_label)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
          )
          
          Text(
            AnnotatedString.fromHtml(
              stringResource(R.string.dhl_api_key_flavor_text),
              linkStyles = TextLinkStyles(
                style = SpanStyle(
                  textDecoration = TextDecoration.Underline,
                  color = MaterialTheme.colorScheme.primary,
                )
              ),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
          )
        }
      }
      
      Spacer(modifier = Modifier.height(16.dp))

      Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
          ) {
            Icon(
              painterResource(R.drawable.ic_science),
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary
            )
            Text(
              text = stringResource(R.string.settings_experimental),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(start = 8.dp)
            )
          }
          
          Row(
            modifier = Modifier
              .clickable { setValue(DEMO_MODE, demoMode.not()) }
              .padding(vertical = 8.dp)
              .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.fillMaxWidth(0.8f)) {
              Text(stringResource(R.string.demo_mode))
              Text(
                stringResource(R.string.demo_mode_detail),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Switch(checked = demoMode, onCheckedChange = { setValue(DEMO_MODE, it) })
          }
        }
      }
      Spacer(modifier = Modifier.height(24.dp))
    }

    if (aboutDialogOpen) {
      AboutDialog { aboutDialogOpen = false }
    }
  }
}

@Composable
@PreviewLightDark
private fun SettingsViewPreview() {
  ParcelTrackerTheme { SettingsView() }
}
