// SPDX-License-Identifier: GPL-3.0-or-later
package dev.itsvic.parceltracker.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.itsvic.parceltracker.R
import dev.itsvic.parceltracker.api.Parcel
import dev.itsvic.parceltracker.api.ParcelHistoryItem
import dev.itsvic.parceltracker.api.Service
import dev.itsvic.parceltracker.api.Status
import dev.itsvic.parceltracker.api.getDeliveryServiceName
import dev.itsvic.parceltracker.ui.components.FloatingCollapsibleActionBar
import dev.itsvic.parceltracker.ui.components.ParcelHistoryItemRow
import dev.itsvic.parceltracker.ui.theme.ParcelTrackerTheme
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParcelView(
  parcel: Parcel,
  humanName: String,
  service: Service,
  isArchived: Boolean,
  archivePromptDismissed: Boolean,
  onBackPressed: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  onArchive: () -> Unit,
  onArchivePromptDismissal: () -> Unit,
  showBackButton: Boolean = true,
) {
  Box {
    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = humanName,
              style = MaterialTheme.typography.titleLarge
            )
          },
          navigationIcon = {
            if (showBackButton) {
              IconButton(onClick = onBackPressed) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = stringResource(R.string.go_back)
                )
              }
            }
          }
        )
      },
      bottomBar = {},
    ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .padding(innerPadding)
        .padding(16.dp)
        .padding(bottom = 45.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {

      if (parcel.properties.isNotEmpty()) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp)
          ) {
            Column(modifier = Modifier.padding(20.dp)) {
              Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .background(
                      MaterialTheme.colorScheme.secondaryContainer,
                      CircleShape
                    ),
                  contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                  Icon(
                    painter = painterResource(R.drawable.package_2),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                  )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                  text = stringResource(R.string.additional_info),
                  style = MaterialTheme.typography.titleLarge,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
              
              parcel.properties.entries.forEachIndexed { index, entry ->
                if (index > 0) {
                  HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
                Row(
                  modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                  Text(
                    text = stringResource(entry.key),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Text(
                    text = entry.value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
              }
            }
          }
        }
      }
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = when (parcel.currentStatus) {
              Status.Delivered, Status.PickedUp -> MaterialTheme.colorScheme.primaryContainer
              Status.InTransit, Status.OutForDelivery -> MaterialTheme.colorScheme.tertiaryContainer
              else -> MaterialTheme.colorScheme.surfaceVariant
            }
          )
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            Row(
              verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
              modifier = Modifier.padding(bottom = 12.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .background(
                    when (parcel.currentStatus) {
                      Status.Delivered, Status.PickedUp -> MaterialTheme.colorScheme.primary
                      Status.InTransit, Status.OutForDelivery -> MaterialTheme.colorScheme.tertiary
                      else -> MaterialTheme.colorScheme.outline
                    },
                    CircleShape
                  ),
                contentAlignment = androidx.compose.ui.Alignment.Center
              ) {
                Icon(
                  painter = painterResource(R.drawable.outline_deployed_code_history_24),
                  contentDescription = null,
                  tint = when (parcel.currentStatus) {
                    Status.Delivered, Status.PickedUp -> MaterialTheme.colorScheme.onPrimary
                    Status.InTransit, Status.OutForDelivery -> MaterialTheme.colorScheme.onTertiary
                    else -> MaterialTheme.colorScheme.surface
                  },
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.size(12.dp))
              Text(
                text = stringResource(R.string.current_status),
                style = MaterialTheme.typography.titleMedium,
                color = when (parcel.currentStatus) {
                  Status.Delivered, Status.PickedUp -> MaterialTheme.colorScheme.onPrimaryContainer
                  Status.InTransit, Status.OutForDelivery -> MaterialTheme.colorScheme.onTertiaryContainer
                  else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
              )
            }
            Text(
              text = LocalContext.current.getString(parcel.currentStatus.nameResource),
              style = MaterialTheme.typography.headlineMedium,
              color = when (parcel.currentStatus) {
                Status.Delivered, Status.PickedUp -> MaterialTheme.colorScheme.onPrimaryContainer
                Status.InTransit, Status.OutForDelivery -> MaterialTheme.colorScheme.onTertiaryContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
              }
            )
          }
        }
      }

      if (
        !isArchived &&
          !archivePromptDismissed &&
          (parcel.currentStatus == Status.Delivered || parcel.currentStatus == Status.PickedUp)
      )
        item {
          Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(bottom = 16.dp)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                stringResource(R.string.archive_prompt_question),
                style = MaterialTheme.typography.titleMedium,
              )
              Text(stringResource(R.string.archive_prompt_text))
              Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                FilledTonalButton(onArchivePromptDismissal, modifier = Modifier.weight(1f)) {
                  Text(stringResource(R.string.ignore))
                }
                Button(onArchive, modifier = Modifier.weight(1f)) {
                  Text(stringResource(R.string.archive))
                }
              }
            }
          }
        }
      if (parcel.history.isNotEmpty()) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp)
          ) {
            Column(modifier = Modifier.padding(20.dp)) {
              Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .background(
                      MaterialTheme.colorScheme.tertiaryContainer,
                      CircleShape
                    ),
                  contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                  Icon(
                    painter = painterResource(R.drawable.outline_deployed_code_history_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(20.dp)
                  )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                  text = stringResource(R.string.tracking_history),
                  style = MaterialTheme.typography.titleLarge,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
              
              parcel.history.forEachIndexed { index, historyItem ->
                if (index > 0) {
                  HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                }
                ParcelHistoryItemRow(historyItem)
              }
            }
          }
        }
      }
    }
    FloatingCollapsibleActionBar(
      status = parcel.currentStatus,
      onEdit = onEdit,
      onArchive = onArchive,
      onDelete = onDelete,
      modifier = Modifier.align(Alignment.BottomCenter)
    )
  }
}

@Composable
@PreviewLightDark
fun ParcelViewPreview() {
  val parcel =
    Parcel(
      "EXMPL0001",
      listOf(
        ParcelHistoryItem(
          "The package got lost. Whoops!",
          LocalDateTime.of(2025, 1, 1, 12, 0, 0),
          "Warsaw, Poland",
        ),
        ParcelHistoryItem(
          "Arrived at local warehouse",
          LocalDateTime.of(2025, 1, 1, 10, 0, 0),
          "Warsaw, Poland",
        ),
        ParcelHistoryItem(
          "En route to local warehouse",
          LocalDateTime.of(2024, 12, 1, 12, 0, 0),
          "Netherlands",
        ),
        ParcelHistoryItem("Label created", LocalDateTime.of(2024, 12, 1, 12, 0, 0), "Netherlands"),
      ),
      Status.DeliveryFailure,
    )
  ParcelTrackerTheme {
    ParcelView(
      parcel,
      "My precious package",
      Service.EXAMPLE,
      isArchived = false,
      archivePromptDismissed = false,
      onBackPressed = {},
      onEdit = {},
      onDelete = {},
      onArchive = {},
      onArchivePromptDismissal = {},
    )
  }
}
}
