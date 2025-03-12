package dev.itsvic.parceltracker

import android.content.Context
import android.os.Parcelable
import android.util.Log
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.itsvic.parceltracker.api.Parcel
import dev.itsvic.parceltracker.api.ParcelHistoryItem
import dev.itsvic.parceltracker.api.ParcelNonExistentException
import dev.itsvic.parceltracker.api.Status
import dev.itsvic.parceltracker.api.getParcel
import dev.itsvic.parceltracker.db.ParcelWithStatus
import dev.itsvic.parceltracker.db.demoModeParcels
import dev.itsvic.parceltracker.ui.views.HomeView
import dev.itsvic.parceltracker.ui.views.ParcelView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import okio.IOException
import java.time.LocalDateTime

@Parcelize
class ParcelInfo(val id: Int) : Parcelable

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ParcelListDetailRoute(
    demoMode: Boolean = true,
    onNavigateToSettings: () -> Unit,
    onNavigateToAddParcel: () -> Unit,
) {
    val context = LocalContext.current
    val db = ParcelApplication.db
    val navigator = rememberListDetailPaneScaffoldNavigator<ParcelInfo>()
    val scope = rememberCoroutineScope()

    NavigableListDetailPaneScaffold(
        navigator = navigator,
        listPane = {
            AnimatedPane(
                enterTransition = fadeIn(tween(300)) + scaleIn(tween(500), 0.9f),
                exitTransition = slideOutHorizontally(
                    tween(300),
                    targetOffsetX = { -it / 4 }
                ) + fadeOut(tween(300))
            ) {
                val parcels: List<ParcelWithStatus> by if (demoMode)
                    remember { derivedStateOf { demoModeParcels } }
                else
                    db.parcelDao().getAllWithStatus().collectAsState(emptyList())

                HomeView(
                    parcels = parcels,
                    onNavigateToAddParcel = onNavigateToAddParcel,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToParcel = {
                        scope.launch {
                            navigator.navigateTo(
                                ListDetailPaneScaffoldRole.Detail,
                                ParcelInfo(it.id)
                            )
                        }
                    }
                )
            }
        },

        detailPane = {
            AnimatedPane(
                enterTransition = slideInHorizontally(
                    tween(300),
                    initialOffsetX = { it / 4 }
                ) + fadeIn(tween(300)),
            ) {
                navigator.currentDestination?.contentKey?.let { parcelInfo ->
                    val parcelWithStatus: ParcelWithStatus? by
                    if (demoMode)
                        derivedStateOf { demoModeParcels[parcelInfo.id] }
                    else
                        db.parcelDao().getWithStatusById(parcelInfo.id).collectAsState(null)
                    val dbParcel = parcelWithStatus?.parcel
                    val apiParcel = dbParcel?.let { context.getParcelFlow(it).collectAsState(null) }

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
                            canBack = navigator.canNavigateBack(),
                            onBackPressed = {
                                if (navigator.canNavigateBack()) {
                                    scope.launch {
                                        navigator.navigateBack()
                                    }
                                }
                            },
                            onEdit = {},
                            onDelete = {},
                        )
                }
            }
        }
    )
}

fun Context.getParcelFlow(parcel: dev.itsvic.parceltracker.db.Parcel): Flow<Parcel> = flow {
    try {
        emit(getParcel(parcel.parcelId, parcel.postalCode, parcel.service))
    } catch (e: IOException) {
        Log.w("ParcelListDetailRoute", "Failed fetch: $e")
        emit(
            Parcel(
                parcel.parcelId,
                listOf(
                    ParcelHistoryItem(
                        getString(R.string.network_failure_detail),
                        LocalDateTime.now(),
                        ""
                    )
                ),
                Status.NetworkFailure
            )
        )
    } catch (_: ParcelNonExistentException) {
        emit(
            Parcel(
                parcel.parcelId,
                listOf(
                    ParcelHistoryItem(
                        getString(R.string.parcel_doesnt_exist_detail),
                        LocalDateTime.now(),
                        ""
                    )
                ),
                Status.NoData
            )
        )
    }
}
