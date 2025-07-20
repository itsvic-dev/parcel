// SPDX-License-Identifier: GPL-3.0-or-later
package dev.itsvic.parceltracker.ui.views

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.itsvic.parceltracker.R
import dev.itsvic.parceltracker.api.Service
import dev.itsvic.parceltracker.api.Status
import dev.itsvic.parceltracker.db.Parcel
import dev.itsvic.parceltracker.db.ParcelStatus
import dev.itsvic.parceltracker.db.ParcelWithStatus
import dev.itsvic.parceltracker.ui.components.ParcelRow
import dev.itsvic.parceltracker.ui.theme.ParcelTrackerTheme
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeView(
  parcels: List<ParcelWithStatus>,
  onNavigateToAddParcel: () -> Unit,
  onNavigateToParcel: (Parcel) -> Unit,
  onNavigateToSettings: () -> Unit,
) {
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

  Scaffold(
    topBar = {
      LargeTopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        scrollBehavior = scrollBehavior,
      )
    },
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
  ) { innerPadding ->
    LazyColumn(modifier = Modifier.padding(innerPadding)) {
      if (parcels.isEmpty())
        item {
          Text(
            stringResource(R.string.no_parcels_flavor),
            modifier = Modifier.padding(horizontal = 16.dp),
          )
        }

      items(parcels.reversed()) { parcel ->
        ParcelRow(parcel.parcel, parcel.status?.status) { onNavigateToParcel(parcel.parcel) }
      }
    }
  }
}

@Composable
@PreviewLightDark
fun HomeViewPreview() {
  ParcelTrackerTheme {
    HomeView(
      parcels =
        listOf(
          ParcelWithStatus(
            Parcel(0, "My precious package", "EXMPL0001", null, Service.EXAMPLE),
            ParcelStatus(0, Status.InTransit, Instant.now()),
          )
        ),
      onNavigateToAddParcel = {},
      onNavigateToParcel = {},
      onNavigateToSettings = {},
    )
  }
}
