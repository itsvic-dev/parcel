// SPDX-License-Identifier: GPL-3.0-or-later
package dev.itsvic.parceltracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.itsvic.parceltracker.R
import dev.itsvic.parceltracker.api.Status

@Composable
fun FloatingCollapsibleActionBar(
  status: Status?,
  onEdit: () -> Unit,
  onArchive: () -> Unit,
  onDelete: () -> Unit,
  onBackPressed: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  var isExpanded by remember { mutableStateOf(false) }
  var showDeleteDialog by remember { mutableStateOf(false) }
  
  val rotationAngle by animateFloatAsState(
    targetValue = if (isExpanded) 180f else 0f,
    animationSpec = tween(300),
    label = "rotation"
  )

  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(16.dp),
    contentAlignment = Alignment.BottomCenter
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(
          elevation = 8.dp,
          shape = RoundedCornerShape(16.dp)
        ),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
      ),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
      Column {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = stringResource(R.string.actions),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
          )
          Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
            modifier = Modifier.rotate(rotationAngle),
            tint = MaterialTheme.colorScheme.onSurface
          )
        }
        AnimatedVisibility(
          visible = isExpanded,
          enter = expandVertically(
            animationSpec = tween(300)
          ) + fadeIn(animationSpec = tween(300)),
          exit = shrinkVertically(
            animationSpec = tween(300)
          ) + fadeOut(animationSpec = tween(300))
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
          ) {
            if (onBackPressed != null) {
              ActionButton(
                icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.go_back)) },
                text = stringResource(R.string.go_back),
                onClick = onBackPressed
              )
              Spacer(modifier = Modifier.height(8.dp))
            }
            ActionButton(
              icon = { Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit)) },
              text = stringResource(R.string.edit),
              onClick = onEdit
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            if (status == Status.Delivered) {
              ActionButton(
                icon = {
                  Icon(
                    painterResource(R.drawable.archive),
                    contentDescription = stringResource(R.string.archive)
                  )
                },
                text = stringResource(R.string.archive),
                onClick = onArchive
              )
              Spacer(modifier = Modifier.height(8.dp))
            }
            ActionButton(
              icon = { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete)) },
              text = stringResource(R.string.delete),
              onClick = { showDeleteDialog = true },
              isDestructive = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
          }
        }
      }
    }
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
        TextButton(onClick = { showDeleteDialog = false }) { 
          Text(stringResource(R.string.cancel)) 
        }
      },
    )
  }
}

@Composable
fun ActionButton(
  icon: @Composable () -> Unit,
  text: String,
  onClick: () -> Unit,
  isDestructive: Boolean = false,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable { onClick() }
      .background(
        if (isDestructive) 
          MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        else 
          MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
      )
      .padding(16.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier.size(24.dp),
      contentAlignment = Alignment.Center
    ) {
      icon()
    }
    Spacer(modifier = Modifier.width(16.dp))
    Text(
      text = text,
      style = MaterialTheme.typography.bodyLarge,
      color = if (isDestructive) 
        MaterialTheme.colorScheme.error 
      else 
        MaterialTheme.colorScheme.onSurface
    )
  }
}