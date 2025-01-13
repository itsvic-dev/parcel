package dev.itsvic.parceltracker

import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.itsvic.parceltracker.db.demoModeParcels
import dev.itsvic.parceltracker.ui.views.HomeView
import kotlinx.parcelize.Parcelize

@Parcelize
class ParcelInfo(val id: Int): Parcelable

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ParcelListDetailRoute(
    demoMode: Boolean = true,
) {
    val db = ParcelApplication.db
    val navigator = rememberListDetailPaneScaffoldNavigator<ParcelInfo>()
    val parcels = if (demoMode)
        remember { derivedStateOf { demoModeParcels } }
    else
        db.parcelDao().getAllWithStatus().collectAsState(emptyList())

    BackHandler(enabled = navigator.canNavigateBack()) {
        navigator.navigateBack()
    }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                HomeView(
                    parcels = parcels.value,
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
                navigator.currentDestination?.content?.let {
                    Scaffold { innerPadding ->
                        Column(modifier = Modifier.padding(innerPadding)) {
                            Text("detail pane: ${it.id}")
                        }
                    }
                }
            }
        }
    )
}
