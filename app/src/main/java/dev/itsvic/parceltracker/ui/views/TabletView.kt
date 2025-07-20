// SPDX-License-Identifier: GPL-3.0-or-later
package dev.itsvic.parceltracker.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.itsvic.parceltracker.R
import dev.itsvic.parceltracker.api.Parcel as APIParcel
import dev.itsvic.parceltracker.db.Parcel
import dev.itsvic.parceltracker.db.ParcelWithStatus
import dev.itsvic.parceltracker.ui.components.ParcelRow

enum class TabletNavigationItem {
    HOME,
    ADD_PARCEL,
    EDIT_PARCEL,
    SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabletView(
    parcels: List<ParcelWithStatus>,
    selectedParcel: Parcel?,
    apiParcel: APIParcel?,
    isLoading: Boolean,
    currentNavigationItem: TabletNavigationItem,
    onNavigateToItem: (TabletNavigationItem) -> Unit,
    onNavigateToParcel: (Parcel) -> Unit,
    onNavigateToAddParcel: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onEditParcel: (Parcel) -> Unit,
    onDeleteParcel: (Parcel) -> Unit,
    onArchiveParcel: (Parcel) -> Unit,
    onArchivePromptDismissal: (Parcel) -> Unit,
    settingsContent: @Composable () -> Unit = {},
    addParcelContent: @Composable () -> Unit = {},
    editParcelContent: @Composable () -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Row(modifier = Modifier.fillMaxSize()) {
        Card(modifier = Modifier.width(400.dp).fillMaxHeight().padding(8.dp)) {
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp)
                ) {
                    if (parcels.isEmpty()) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                                Text(
                                    stringResource(R.string.no_parcels_flavor),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(parcels.reversed()) { parcel ->
                            Card(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                ParcelRow(
                                    parcel.parcel,
                                    parcel.status?.status,
                                    isSelected = selectedParcel?.id == parcel.parcel.id
                                ) {
                                    onNavigateToParcel(parcel.parcel)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                NavigationBar(modifier = Modifier.fillMaxWidth()) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        label = { Text(stringResource(R.string.home)) },
                        selected = currentNavigationItem == TabletNavigationItem.HOME,
                        onClick = { onNavigateToItem(TabletNavigationItem.HOME) }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        label = { Text(stringResource(R.string.add_parcel)) },
                        selected = currentNavigationItem == TabletNavigationItem.ADD_PARCEL,
                        onClick = { onNavigateToItem(TabletNavigationItem.ADD_PARCEL) }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        label = { Text(stringResource(R.string.settings)) },
                        selected = currentNavigationItem == TabletNavigationItem.SETTINGS,
                        onClick = { onNavigateToItem(TabletNavigationItem.SETTINGS) }
                    )
                }
            }
        }

        // Right panel: Content area
        Card(modifier = Modifier.weight(1f).fillMaxHeight().padding(8.dp)) {
            when (currentNavigationItem) {
                TabletNavigationItem.HOME -> {
                    if (selectedParcel != null) {
                        if (isLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else if (apiParcel != null) {
                            ParcelView(
                                parcel = apiParcel,
                                humanName = selectedParcel.humanName,
                                service = selectedParcel.service,
                                isArchived = selectedParcel.isArchived,
                                archivePromptDismissed = selectedParcel.archivePromptDismissed,
                                onBackPressed = { /* No back button in tablet mode */},
                                onEdit = { onEditParcel(selectedParcel) },
                                onDelete = { onDeleteParcel(selectedParcel) },
                                onArchive = { onArchiveParcel(selectedParcel) },
                                onArchivePromptDismissal = {
                                    onArchivePromptDismissal(selectedParcel)
                                },
                                showBackButton = false
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.headlineMedium,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.select_parcel_to_view),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
                TabletNavigationItem.ADD_PARCEL -> {
                    addParcelContent()
                }
                TabletNavigationItem.EDIT_PARCEL -> {
                    editParcelContent()
                }
                TabletNavigationItem.SETTINGS -> {
                    settingsContent()
                }
            }
        }
    }
}
