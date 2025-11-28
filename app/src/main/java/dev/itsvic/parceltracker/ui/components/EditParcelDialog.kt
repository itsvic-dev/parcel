// SPDX-License-Identifier: GPL-3.0-or-later
package dev.itsvic.parceltracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.itsvic.parceltracker.R
import dev.itsvic.parceltracker.db.Parcel
import dev.itsvic.parceltracker.ui.views.AddEditParcelContent

@Composable
fun EditParcelDialog(
    parcel: Parcel,
    onDismissRequest: () -> Unit,
    onCompleted: (Parcel) -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(maxHeight = 700.dp, maxWidth = 500.dp)
                .verticalScroll(rememberScrollState())
        ) {
                Text(
                    text = stringResource(R.string.edit_parcel),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                AddEditParcelContent(
                    parcel = parcel,
                    onCompleted = onCompleted,
                    isDialog = true
                )
            }
    }
}