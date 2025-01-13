package dev.itsvic.parceltracker

import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.itsvic.parceltracker.api.Parcel
import dev.itsvic.parceltracker.api.getParcel
import dev.itsvic.parceltracker.db.ParcelWithStatus
import dev.itsvic.parceltracker.db.demoModeParcels
import dev.itsvic.parceltracker.ui.views.HomeView
import dev.itsvic.parceltracker.ui.views.ParcelView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.parcelize.Parcelize

@Parcelize
class ParcelInfo(val id: Int) : Parcelable

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ParcelListDetailRoute(
    demoMode: Boolean = true,
) {
    val db = ParcelApplication.db
    val navigator = rememberListDetailPaneScaffoldNavigator<ParcelInfo>()
    BackHandler(enabled = navigator.canNavigateBack()) {
        navigator.navigateBack()
    }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                val parcels: List<ParcelWithStatus> by if (demoMode)
                    remember { derivedStateOf { demoModeParcels } }
                else
                    db.parcelDao().getAllWithStatus().collectAsState(emptyList())

                HomeView(
                    parcels = parcels,
                    onNavigateToAddParcel = {},
                    onNavigateToSettings = {},
                    onNavigateToParcel = {
                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, ParcelInfo(it.id))
                    }
                )
            }
        },

        detailPane = {
            AnimatedPane {
                navigator.currentDestination?.content?.let { parcelInfo ->
                    val parcelWithStatus: ParcelWithStatus? by
                    if (demoMode)
                        derivedStateOf { demoModeParcels[parcelInfo.id] }
                    else
                        db.parcelDao().getWithStatusById(parcelInfo.id).collectAsState(null)
                    val dbParcel = parcelWithStatus?.parcel
                    val apiParcel = dbParcel?.let { getParcelFlow(it).collectAsState(null) }

                    if (apiParcel?.value == null)
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.background)
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        )
                        {
                            CircularProgressIndicator()
                        }
                    else
                        ParcelView(
                            apiParcel.value!!,
                            dbParcel.humanName,
                            dbParcel.service,
                            onBackPressed = {},
                            onEdit = {},
                            onDelete = {},
                        )
                }
            }
        }
    )
}

fun getParcelFlow(parcel: dev.itsvic.parceltracker.db.Parcel): Flow<Parcel> = flow {
    emit(getParcel(parcel.parcelId, parcel.postalCode, parcel.service))
}
