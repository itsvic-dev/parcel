// SPDX-License-Identifier: GPL-3.0-or-later
package dev.itsvic.parceltracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.itsvic.parceltracker.R

@Composable
fun BottomNavBar(
  currentRoute: String,
  onNavigateToHome: () -> Unit,
  onNavigateToAddParcel: () -> Unit,
  onNavigateToSettings: () -> Unit,
) {
  NavigationBar {
    NavigationBarItem(
      icon = { Icon(Icons.Filled.Home, contentDescription = stringResource(R.string.home)) },
      label = { Text(stringResource(R.string.home)) },
      selected = currentRoute.contains("HomePage"),
      onClick = onNavigateToHome,
    )
    NavigationBarItem(
      icon = { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add)) },
      label = { Text(stringResource(R.string.add)) },
      selected = currentRoute.contains("AddParcelPage"),
      onClick = onNavigateToAddParcel,
    )
    NavigationBarItem(
      icon = {
        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
      },
      label = { Text(stringResource(R.string.settings)) },
      selected = currentRoute.contains("SettingsPage"),
      onClick = onNavigateToSettings,
    )
  }
}
