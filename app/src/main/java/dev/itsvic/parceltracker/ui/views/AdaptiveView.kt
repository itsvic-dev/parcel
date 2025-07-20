// SPDX-License-Identifier: GPL-3.0-or-later
package dev.itsvic.parceltracker.ui.views

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.itsvic.parceltracker.api.Parcel as APIParcel
import dev.itsvic.parceltracker.db.Parcel
import dev.itsvic.parceltracker.db.ParcelWithStatus

@Composable
fun AdaptiveParcelApp(
  windowSizeClass: WindowSizeClass,
  parcels: List<ParcelWithStatus>,
  selectedParcel: Parcel?,
  apiParcel: APIParcel?,
  isLoading: Boolean,
  onNavigateToParcel: (Parcel) -> Unit,
  onNavigateToAddParcel: () -> Unit,
  onNavigateToSettings: () -> Unit,
  onEditParcel: (Parcel) -> Unit,
  onDeleteParcel: (Parcel) -> Unit,
  onArchiveParcel: (Parcel) -> Unit,
  onArchivePromptDismissal: (Parcel) -> Unit,
  settingsContent: @Composable () -> Unit,
  addParcelContent: @Composable () -> Unit,
  homeContent: @Composable () -> Unit,
) {
  val isTablet = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium

  if (isTablet) {
    var currentNavigationItem by remember { mutableStateOf(TabletNavigationItem.HOME) }

    TabletView(
      parcels = parcels,
      selectedParcel = selectedParcel,
      apiParcel = apiParcel,
      isLoading = isLoading,
      currentNavigationItem = currentNavigationItem,
      onNavigateToItem = { currentNavigationItem = it },
      onNavigateToParcel = onNavigateToParcel,
      onNavigateToAddParcel = onNavigateToAddParcel,
      onNavigateToSettings = onNavigateToSettings,
      onEditParcel = onEditParcel,
      onDeleteParcel = onDeleteParcel,
      onArchiveParcel = onArchiveParcel,
      onArchivePromptDismissal = onArchivePromptDismissal,
      settingsContent = settingsContent,
      addParcelContent = addParcelContent,
    )
  } else {
    homeContent()
  }
}
