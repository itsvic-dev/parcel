// SPDX-License-Identifier: GPL-3.0-or-later
package dev.itsvic.parceltracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import dev.itsvic.parceltracker.R
import dev.itsvic.parceltracker.api.Status

@Composable
fun ParcelActionBar(
  status: Status?,
  onEdit: () -> Unit,
  onArchive: () -> Unit,
  onDelete: () -> Unit
) {
  var showDeleteDialog by remember { mutableStateOf(false) }

  NavigationBar {
    NavigationBarItem(
      icon = { Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit)) },
      label = { Text(stringResource(R.string.edit)) },
      selected = false,
      onClick = onEdit
    )

    if (status == Status.Delivered) {
      NavigationBarItem(
        icon = {
          Icon(
            painterResource(R.drawable.archive),
            contentDescription = stringResource(R.string.archive))
        },
        label = { Text(stringResource(R.string.archive)) },
        selected = false,
        onClick = onArchive
      )
    }

    NavigationBarItem(
      icon = {
        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
      },
      label = { Text(stringResource(R.string.delete)) },
      selected = false,
      onClick = { showDeleteDialog = true }
    )
  }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.delete_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                     Text(stringResource(R.string.cancel))
                 }
            }
        )
    }
}