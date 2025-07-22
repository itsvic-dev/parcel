// SPDX-License-Identifier: GPL-3.0-or-later
package dev.itsvic.parceltracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.itsvic.parceltracker.api.APIKeyMissingException
import dev.itsvic.parceltracker.api.Parcel as APIParcel
import dev.itsvic.parceltracker.api.ParcelHistoryItem
import dev.itsvic.parceltracker.api.ParcelNonExistentException
import dev.itsvic.parceltracker.api.Status
import dev.itsvic.parceltracker.api.getParcel
import dev.itsvic.parceltracker.db.Parcel
import dev.itsvic.parceltracker.db.ParcelStatus
import dev.itsvic.parceltracker.db.ParcelWithStatus
import dev.itsvic.parceltracker.db.deleteParcel
import dev.itsvic.parceltracker.db.demoModeParcels
import dev.itsvic.parceltracker.ui.theme.ParcelTrackerTheme
import dev.itsvic.parceltracker.ui.components.BottomNavBar
import dev.itsvic.parceltracker.ui.components.EditParcelDialog
import dev.itsvic.parceltracker.ui.views.AddEditParcelView
import dev.itsvic.parceltracker.ui.views.HomeView
import dev.itsvic.parceltracker.ui.views.ParcelView
import dev.itsvic.parceltracker.ui.views.SettingsView
import dev.itsvic.parceltracker.ui.views.TabletNavigationItem
import dev.itsvic.parceltracker.ui.views.TabletView
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import okio.IOException

class MainActivity : ComponentActivity() {
  @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) handleNotificationPermissionStuff()

    parcelToOpen = mutableIntStateOf(intent.getIntExtra("openParcel", -1))

    setContent {
      val parcelToOpen by parcelToOpen
      val windowSizeClass = calculateWindowSizeClass(this)

      ParcelTrackerTheme {
        Box(modifier = Modifier.background(color = MaterialTheme.colorScheme.background)) {
          ParcelAppNavigation(parcelToOpen, windowSizeClass)
        }
      }
    }
  }

  companion object {
    lateinit var parcelToOpen: MutableIntState
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    parcelToOpen.intValue = intent.getIntExtra("openParcel", -1)
  }

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  fun handleNotificationPermissionStuff() {
    val requestPermissionLauncher =
      registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
          Log.d("MainActivity", "Notification permissions granted")
        } else {
          Log.d("MainActivity", "Notification permissions NOT granted")
        }
      }

    when {
      ContextCompat.checkSelfPermission(
        applicationContext,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED -> {
        // We can post notifications
      }
      // TODO: educational UI maybe?
      else -> {
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }
}

@Serializable object HomePage

@Serializable object SettingsPage

@Serializable data class ParcelPage(val parcelDbId: Int)

@Serializable object AddParcelPage

@Serializable data class EditParcelPage(val parcelDbId: Int)

@Composable
fun ParcelAppNavigation(parcelToOpen: Int, windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass) {
  val db = ParcelApplication.db
  val navController = rememberNavController()
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val demoMode by context.dataStore.data.map { it[DEMO_MODE] == true }.collectAsState(false)

  LaunchedEffect(parcelToOpen) {
    if (parcelToOpen != -1) {
      navController.navigate(route = ParcelPage(parcelToOpen)) { popUpTo(HomePage) }
    }
  }

  val animDuration = 400
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route ?: "HomePage"
  
  val isTablet = windowSizeClass.widthSizeClass >= androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Medium

  var selectedParcel by remember { mutableStateOf<Parcel?>(null) }
  var apiParcel by remember { mutableStateOf<APIParcel?>(null) }
  var isLoadingParcel by remember { mutableStateOf(false) }
  var currentTabletNavItem by remember { mutableStateOf(TabletNavigationItem.HOME) }
  
  val parcels =
      if (demoMode) derivedStateOf { demoModeParcels }
      else db.parcelDao().getAllWithStatus().collectAsState(initial = emptyList())

  LaunchedEffect(selectedParcel) {
    if (selectedParcel != null) {
      isLoadingParcel = true
      launch(Dispatchers.IO) {
        try {
          if (selectedParcel!!.isArchived) {
             val localHistory = db.parcelHistoryDao().getAllById(selectedParcel!!.id).first()
             if (localHistory.isNotEmpty()) {
               apiParcel = APIParcel(
                   selectedParcel!!.parcelId,
                   localHistory.map { dev.itsvic.parceltracker.api.ParcelHistoryItem(it.description, it.time, it.location) },
                   Status.Delivered
               )
             } else {
               apiParcel = context.getParcel(selectedParcel!!.parcelId, selectedParcel!!.postalCode, selectedParcel!!.service)
             }
          } else {
            apiParcel = context.getParcel(selectedParcel!!.parcelId, selectedParcel!!.postalCode, selectedParcel!!.service)
            
            if (!demoMode) {
              val zone = ZoneId.systemDefault()
              val lastChange = apiParcel!!.history.first().time.atZone(zone).toInstant()
              val status = ParcelStatus(selectedParcel!!.id, apiParcel!!.currentStatus, lastChange)
              val existingStatus = db.parcelStatusDao().get(selectedParcel!!.id)
              if (existingStatus == null) {
                db.parcelStatusDao().insert(status)
              } else {
                db.parcelStatusDao().update(status)
              }
            }
          }
        } catch (e: Exception) {
          Log.w("MainActivity", "Failed fetch: $e")
          apiParcel = APIParcel(
              selectedParcel!!.parcelId,
              listOf(ParcelHistoryItem(context.getString(R.string.network_failure_detail), LocalDateTime.now(), "")),
              Status.NetworkFailure
          )
        }
        isLoadingParcel = false
      }
    } else {
      apiParcel = null
      isLoadingParcel = false
    }
  }
  
  if (isTablet) {
    TabletView(
        parcels = parcels.value,
        selectedParcel = selectedParcel,
        apiParcel = apiParcel,
        isLoading = isLoadingParcel,
        currentNavigationItem = currentTabletNavItem,
        onNavigateToItem = { currentTabletNavItem = it },
        onNavigateToParcel = { 
          selectedParcel = it
          currentTabletNavItem = TabletNavigationItem.HOME
        },
        onNavigateToAddParcel = { currentTabletNavItem = TabletNavigationItem.ADD_PARCEL },
        onNavigateToSettings = { currentTabletNavItem = TabletNavigationItem.SETTINGS },
        onEditParcel = { parcel ->
          selectedParcel = parcel
          currentTabletNavItem = TabletNavigationItem.EDIT_PARCEL
        },
        onDeleteParcel = { parcel ->
          if (demoMode) {
            Toast.makeText(context, context.getString(R.string.demo_mode_action_block), Toast.LENGTH_SHORT).show()
            return@TabletView
          }
          scope.launch(Dispatchers.IO) {
            deleteParcel(parcel)
            selectedParcel = null
          }
        },
        onArchiveParcel = { parcel ->
          if (parcel.isArchived || demoMode) {
            if (demoMode) {
              Toast.makeText(context, context.getString(R.string.demo_mode_action_block), Toast.LENGTH_SHORT).show()
            }
            return@TabletView
          }
          scope.launch(Dispatchers.IO) {
            val updatedParcel = parcel.copy(isArchived = true)
            db.parcelDao().update(updatedParcel)
            if (apiParcel != null) {
              db.parcelHistoryDao().insert(
                  apiParcel!!.history.map {
                    dev.itsvic.parceltracker.db.ParcelHistoryItem(
                        description = it.description,
                        location = it.location,
                        time = it.time,
                        parcelId = parcel.id
                    )
                  }
              )
            }
            selectedParcel = updatedParcel
          }
        },
        onArchivePromptDismissal = { parcel ->
          if (demoMode) {
            Toast.makeText(context, context.getString(R.string.demo_mode_action_block), Toast.LENGTH_SHORT).show()
            return@TabletView
          }
          scope.launch(Dispatchers.IO) {
            val updatedParcel = parcel.copy(archivePromptDismissed = true)
            db.parcelDao().update(updatedParcel)
            selectedParcel = updatedParcel
          }
        },
        settingsContent = {
          SettingsView()
        },
        addParcelContent = {
          AddEditParcelView(
              null,
              onBackPressed = { currentTabletNavItem = TabletNavigationItem.HOME },
              onCompleted = { parcel ->
                if (demoMode) {
                  Toast.makeText(context, context.getString(R.string.demo_mode_action_block), Toast.LENGTH_SHORT).show()
                  return@AddEditParcelView
                }
                scope.launch(Dispatchers.IO) {
                  val id = db.parcelDao().insert(parcel)
                  currentTabletNavItem = TabletNavigationItem.HOME
                  selectedParcel = parcel.copy(id = id.toInt())
                }
              }
          )
        },
        editParcelContent = {
          selectedParcel?.let { parcel ->
            AddEditParcelView(
                parcel,
                onBackPressed = { currentTabletNavItem = TabletNavigationItem.HOME },
                onCompleted = { updatedParcel ->
                  if (demoMode) {
                    Toast.makeText(context, context.getString(R.string.demo_mode_action_block), Toast.LENGTH_SHORT).show()
                    return@AddEditParcelView
                  }
                  scope.launch(Dispatchers.IO) {
                    db.parcelDao().update(updatedParcel)
                    currentTabletNavItem = TabletNavigationItem.HOME
                    selectedParcel = updatedParcel
                  }
                }
            )
          }
        }
    )
    return
  }

  Scaffold(
      bottomBar = {
        if (currentRoute.contains("HomePage") || currentRoute.contains("SettingsPage") || currentRoute.contains("AddParcelPage")) {
          BottomNavBar(
              currentRoute = currentRoute,
              onNavigateToHome = {
                navController.navigate(route = HomePage) {
                  popUpTo(HomePage) { inclusive = true }
                }
              },
              onNavigateToAddParcel = {
                navController.navigate(route = AddParcelPage)
              },
              onNavigateToSettings = {
                navController.navigate(route = SettingsPage)
              }
          )
        }
      }
  ) { innerPadding ->
    NavHost(
        navController = navController,
        startDestination = HomePage,
        enterTransition = {
          slideIntoContainer(
              towards = AnimatedContentTransitionScope.SlideDirection.Start,
              animationSpec = tween(animDuration, easing = androidx.compose.animation.core.FastOutSlowInEasing),
              initialOffset = { it / 3 }) + fadeIn(tween(animDuration / 2, delayMillis = animDuration / 4))
        },
        exitTransition = { 
          fadeOut(tween(animDuration / 2)) + 
          scaleOut(tween(animDuration, easing = androidx.compose.animation.core.FastOutSlowInEasing), 0.95f) +
          slideOutOfContainer(
              towards = AnimatedContentTransitionScope.SlideDirection.Start,
              animationSpec = tween(animDuration, easing = androidx.compose.animation.core.FastOutSlowInEasing),
              targetOffset = { -it / 6 })
        },
        popEnterTransition = { 
          fadeIn(tween(animDuration / 2, delayMillis = animDuration / 4)) + 
          scaleIn(tween(animDuration, easing = androidx.compose.animation.core.FastOutSlowInEasing), 0.95f) +
          slideIntoContainer(
              towards = AnimatedContentTransitionScope.SlideDirection.End,
              animationSpec = tween(animDuration, easing = androidx.compose.animation.core.FastOutSlowInEasing),
              initialOffset = { -it / 6 })
        },
        popExitTransition = {
          slideOutOfContainer(
              towards = AnimatedContentTransitionScope.SlideDirection.End,
              animationSpec = tween(animDuration, easing = androidx.compose.animation.core.FastOutSlowInEasing),
              targetOffset = { it / 3 }) + fadeOut(tween(animDuration / 2))
        },
        modifier = Modifier.padding(innerPadding)
    ) {
    composable<HomePage> {
      HomeView(
          parcels = parcels.value,
          onNavigateToAddParcel = { navController.navigate(route = AddParcelPage) },
          onNavigateToParcel = { navController.navigate(route = ParcelPage(it.id)) },
          onNavigateToSettings = { navController.navigate(route = SettingsPage) },
      )
    }

    composable<SettingsPage> { SettingsView() }

    composable<ParcelPage> { backStackEntry ->
      val route: ParcelPage = backStackEntry.toRoute()
      val parcelWithStatus: ParcelWithStatus? by
          if (demoMode) derivedStateOf { demoModeParcels[route.parcelDbId] }
          else db.parcelDao().getWithStatusById(route.parcelDbId).collectAsState(null)
      val dbHistory: List<dev.itsvic.parceltracker.db.ParcelHistoryItem> by
          db.parcelHistoryDao().getAllById(route.parcelDbId).collectAsState(listOf())
      var apiParcel: APIParcel? by remember { mutableStateOf(null) }

      val dbParcel = parcelWithStatus?.parcel

      LaunchedEffect(parcelWithStatus) {
        if (dbParcel != null && !dbParcel.isArchived) {
          launch(Dispatchers.IO) {
            try {
              apiParcel =
                  context.getParcel(dbParcel.parcelId, dbParcel.postalCode, dbParcel.service)

              if (!demoMode) {
                val zone = ZoneId.systemDefault()
                val lastChange = apiParcel!!.history.first().time.atZone(zone).toInstant()
                val status =
                    ParcelStatus(
                        dbParcel.id,
                        apiParcel!!.currentStatus,
                        lastChange,
                    )
                if (parcelWithStatus?.status == null) {
                  db.parcelStatusDao().insert(status)
                } else {
                  db.parcelStatusDao().update(status)
                }
              }
            } catch (e: IOException) {
              Log.w("MainActivity", "Failed fetch: $e")
              apiParcel =
                  APIParcel(
                      dbParcel.parcelId,
                      listOf(
                          ParcelHistoryItem(
                              context.getString(R.string.network_failure_detail),
                              LocalDateTime.now(),
                              "")),
                      Status.NetworkFailure)
            } catch (_: ParcelNonExistentException) {
              apiParcel =
                  APIParcel(
                      dbParcel.parcelId,
                      listOf(
                          ParcelHistoryItem(
                              context.getString(R.string.parcel_doesnt_exist_detail),
                              LocalDateTime.now(),
                              "")),
                      Status.NoData)
            } catch (_: APIKeyMissingException) {
              apiParcel =
                  APIParcel(
                      dbParcel.parcelId,
                      listOf(
                          ParcelHistoryItem(
                              context.getString(R.string.error_no_api_key_provided),
                              LocalDateTime.now(),
                              "")),
                      Status.NetworkFailure)
            }
          }
        }
      }

      val fakeApiParcel =
          parcelWithStatus?.let {
            APIParcel(
                id = it.parcel.parcelId,
                currentStatus = if (it.status != null) it.status.status else Status.Unknown,
                history =
                    dbHistory.map { item ->
                      ParcelHistoryItem(item.description, item.time, item.location)
                    })
          }

      if (apiParcel == null && dbParcel?.isArchived == false || dbParcel == null)
          Box(
              modifier =
                  Modifier.background(color = MaterialTheme.colorScheme.background).fillMaxSize(),
              contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
              }
      else
          ParcelView(
              if (dbParcel.isArchived) fakeApiParcel!! else apiParcel!!,
              dbParcel.humanName,
              dbParcel.service,
              dbParcel.isArchived,
              dbParcel.archivePromptDismissed,
              onBackPressed = { navController.popBackStack() },
              onEdit = { navController.navigate(EditParcelPage(dbParcel.id)) },
              onDelete = {
                if (demoMode) {
                  Toast.makeText(
                          context,
                          context.getString(R.string.demo_mode_action_block),
                          Toast.LENGTH_SHORT)
                      .show()
                  return@ParcelView
                }

                scope.launch(Dispatchers.IO) {
                  deleteParcel(dbParcel)
                  scope.launch { navController.popBackStack(HomePage, false) }
                }
              },
              onArchive = {
                if (dbParcel.isArchived) return@ParcelView
                if (demoMode) {
                  Toast.makeText(
                          context,
                          context.getString(R.string.demo_mode_action_block),
                          Toast.LENGTH_SHORT)
                      .show()
                  return@ParcelView
                }
                scope.launch(Dispatchers.IO) {
                  db.parcelDao().update(dbParcel.copy(isArchived = true))
                  db.parcelHistoryDao()
                      .insert(
                          apiParcel!!.history.map {
                            dev.itsvic.parceltracker.db.ParcelHistoryItem(
                                description = it.description,
                                location = it.location,
                                time = it.time,
                                parcelId = dbParcel.id,
                            )
                          })
                }
              },
              onArchivePromptDismissal = {
                if (demoMode) {
                  Toast.makeText(
                          context,
                          context.getString(R.string.demo_mode_action_block),
                          Toast.LENGTH_SHORT)
                      .show()
                  return@ParcelView
                }
                scope.launch(Dispatchers.IO) {
                  db.parcelDao().update(dbParcel.copy(archivePromptDismissed = true))
                }
              },
          )
    }

    composable<AddParcelPage> {
      AddEditParcelView(
          null,
          onBackPressed = { navController.popBackStack() },
          onCompleted = {
            if (demoMode) {
              Toast.makeText(
                      context,
                      context.getString(R.string.demo_mode_action_block),
                      Toast.LENGTH_SHORT)
                  .show()
              return@AddEditParcelView
            }

            scope.launch(Dispatchers.IO) {
              val id = db.parcelDao().insert(it)
              scope.launch {
                navController.navigate(route = ParcelPage(id.toInt())) { popUpTo(HomePage) }
              }
            }
          },
      )
    }

    composable<EditParcelPage> { backStackEntry ->
      val route: EditParcelPage = backStackEntry.toRoute()
      val parcel: Parcel? by
          if (demoMode) derivedStateOf { demoModeParcels[route.parcelDbId].parcel }
          else db.parcelDao().getById(route.parcelDbId).collectAsState(null)

      if (parcel == null)
          return@composable Box(
              modifier =
                  Modifier.background(color = MaterialTheme.colorScheme.background).fillMaxSize(),
              contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
              }

      EditParcelDialog(
          parcel = parcel!!,
          onDismissRequest = { navController.popBackStack() },
          onCompleted = {
            if (demoMode) {
              Toast.makeText(
                      context,
                      context.getString(R.string.demo_mode_action_block),
                      Toast.LENGTH_SHORT)
                  .show()
              return@EditParcelDialog
            }

            scope.launch(Dispatchers.IO) {
              db.parcelDao().update(it)
              scope.launch { navController.popBackStack() }
            }
          },
      )
    }
  }
  }
}
