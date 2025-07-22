// SPDX-License-Identifier: GPL-3.0-or-later
package dev.itsvic.parceltracker.ui.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.itsvic.parceltracker.CLIPBOARD_PASTE_ENABLED
import dev.itsvic.parceltracker.PREFERRED_REGION
import dev.itsvic.parceltracker.R
import dev.itsvic.parceltracker.api.Service
import dev.itsvic.parceltracker.api.getDeliveryService
import dev.itsvic.parceltracker.api.getDeliveryServiceName
import dev.itsvic.parceltracker.api.serviceOptions
import dev.itsvic.parceltracker.dataStore
import dev.itsvic.parceltracker.db.Parcel
import dev.itsvic.parceltracker.ui.theme.ParcelTrackerTheme
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditParcelView(parcel: Parcel?, onBackPressed: () -> Unit, onCompleted: (Parcel) -> Unit) {
  val isEdit = parcel != null
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

  Scaffold(
    topBar = {
      LargeTopAppBar(
        title = {
          Text(stringResource(if (isEdit) R.string.edit_parcel else R.string.add_a_parcel))
        },
        scrollBehavior = scrollBehavior,
      )
    },
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp)
        .verticalScroll(rememberScrollState()),
    ) {
      AddEditParcelContent(
        parcel = parcel,
        onCompleted = onCompleted,
        isDialog = false
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditParcelContent(
  parcel: Parcel?,
  onCompleted: (Parcel) -> Unit,
  isDialog: Boolean = false
) {
  val isEdit = parcel != null
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val clipboardPasteEnabled by
    context.dataStore.data.map { it[CLIPBOARD_PASTE_ENABLED] == true }.collectAsState(false)
  val preferredRegion by
    context.dataStore.data.map { it[PREFERRED_REGION] ?: "" }.collectAsState("")

  var humanName by remember { mutableStateOf(parcel?.humanName ?: "") }
  var trackingId by remember { mutableStateOf(parcel?.parcelId ?: "") }
  var idError by remember { mutableStateOf(false) }
  var specifyPostalCode by remember { mutableStateOf(parcel?.postalCode != null) }
  var postalCode by remember { mutableStateOf(parcel?.postalCode ?: "") }
  var postalCodeError by remember { mutableStateOf(false) }
  var service by remember { mutableStateOf(parcel?.service ?: Service.UNDEFINED) }
  var serviceError by remember { mutableStateOf(false) }

  val backend = if (service != Service.UNDEFINED) getDeliveryService(service) else null

  fun validateInputs(): Boolean {
    idError = false
    serviceError = false
    postalCodeError = false

    var success = true
    if (trackingId.isBlank()) {
      success = false
      idError = true
    }
    if (service == Service.UNDEFINED) {
      success = false
      serviceError = true
    }
    if (
      ((backend?.acceptsPostCode == true && specifyPostalCode) ||
        (backend?.requiresPostCode == true)) && postalCode.isBlank()
    ) {
      success = false
      postalCodeError = true
    }

    if (!success) return false
    return true
  }

  var expanded by remember { mutableStateOf(false) }
  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

  val sortedServiceOptions =
    serviceOptions.sortedWith(
      compareBy<Service> {
          val isPreferredRegion =
            when (preferredRegion) {
              "international" ->
                it in listOf(Service.CAINIAO, Service.DHL, Service.GLS, Service.UPS, Service.FPX)
              "north_america" -> it == Service.UNIUNI
              "europe" ->
                it in
                  listOf(
                    Service.BELPOST,
                    Service.SAMEDAY_BG,
                    Service.DPD_UK,
                    Service.EVRI,
                    Service.AN_POST,
                    Service.ALLEGRO_ONEBOX,
                    Service.INPOST,
                    Service.ORLEN_PACZKA,
                    Service.POLISH_POST,
                    Service.GLS_HUNGARY,
                    Service.MAGYAR_POSTA,
                    Service.SAMEDAY_HU,
                    Service.IMILE,
                    Service.DPD_GER,
                    Service.HERMES,
                    Service.POSTE_ITALIANE,
                    Service.SAMEDAY_RO,
                    Service.POSTNORD,
                    Service.NOVA_POSHTA,
                    Service.UKRPOSHTA,
                    Service.PACKETA,
                    Service.EXPRESS_ONE,
                  )
              "asia" -> it in listOf(Service.EKART, Service.SPX_TH)
              "belarus" -> it == Service.BELPOST
              "bulgaria" -> it == Service.SAMEDAY_BG
              "uk" -> it in listOf(Service.DPD_UK, Service.EVRI)
              "ireland" -> it == Service.AN_POST
              "poland" ->
                it in
                  listOf(
                    Service.ALLEGRO_ONEBOX,
                    Service.INPOST,
                    Service.ORLEN_PACZKA,
                    Service.POLISH_POST,
                  )
              "hungary" ->
                it in
                  listOf(
                    Service.GLS_HUNGARY,
                    Service.MAGYAR_POSTA,
                    Service.SAMEDAY_HU,
                    Service.EXPRESS_ONE,
                    Service.IMILE,
                  )
              "germany" -> it in listOf(Service.DPD_GER, Service.HERMES)
              "italy" -> it == Service.POSTE_ITALIANE
              "romania" -> it == Service.SAMEDAY_RO
              "scandinavia" -> it == Service.POSTNORD
              "ukraine" -> it in listOf(Service.NOVA_POSHTA, Service.UKRPOSHTA)
              "india" -> it == Service.EKART
              "thailand" -> it == Service.SPX_TH
              else -> false
            }
          if (isPreferredRegion) 0 else 1
        }
        .thenBy {
          when (it) {
            Service.CAINIAO,
            Service.DHL,
            Service.GLS,
            Service.UPS,
            Service.FPX -> 0
            Service.UNIUNI -> 1
            Service.BELPOST,
            Service.SAMEDAY_BG,
            Service.PACKETA,
            Service.DPD_UK,
            Service.EVRI,
            Service.AN_POST,
            Service.ALLEGRO_ONEBOX,
            Service.INPOST,
            Service.ORLEN_PACZKA,
            Service.POLISH_POST,
            Service.GLS_HUNGARY,
            Service.MAGYAR_POSTA,
            Service.SAMEDAY_HU,
            Service.EXPRESS_ONE,
            Service.DPD_GER,
            Service.HERMES,
            Service.POSTE_ITALIANE,
            Service.SAMEDAY_RO,
            Service.POSTNORD,
            Service.NOVA_POSHTA,
            Service.EKART,
            Service.SPX_TH,
            Service.IMILE,
            Service.UKRPOSHTA -> 2
            else -> 4
          } as Comparable<*>?
        }
        .thenBy {
          when (it) {
            Service.BELPOST -> "A_Belarus"
            Service.SAMEDAY_BG -> "B_Bulgaria"
            Service.PACKETA -> "C_Europe"
            Service.DPD_UK,
            Service.EVRI -> "D_UK"
            Service.AN_POST -> "E_Ireland"
            Service.ALLEGRO_ONEBOX,
            Service.INPOST,
            Service.ORLEN_PACZKA,
            Service.POLISH_POST -> "F_Poland"
            Service.GLS_HUNGARY,
            Service.MAGYAR_POSTA,
            Service.SAMEDAY_HU,
            Service.EXPRESS_ONE,
            Service.IMILE -> "G_Hungary"
            Service.DPD_GER,
            Service.HERMES -> "H_Germany"
            Service.POSTE_ITALIANE -> "I_Italy"
            Service.SAMEDAY_RO -> "J_Romania"
            Service.POSTNORD -> "K_Scandinavia"
            Service.NOVA_POSHTA,
            Service.UKRPOSHTA -> "L_Ukraine"
            else -> it.name
          }
        }
        .thenBy {
          if (trackingId.isNotBlank()) {
            val backend = getDeliveryService(it)
            if (backend?.acceptsFormat(trackingId) == true) 0 else 1
          } else {
            0
          }
        }
    )

  Column(
    modifier = Modifier
      .padding(horizontal = if (isDialog) 0.dp else 16.dp)
      .sizeIn(maxWidth = 600.dp)
      .fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    if (!isDialog) {
      Spacer(modifier = Modifier.height(8.dp))
    }
          ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
          ) {
            Column(
              modifier = Modifier.padding(20.dp),
              verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
              OutlinedTextField(
                value = humanName,
                onValueChange = { humanName = it },
                singleLine = true,
                label = { Text(stringResource(R.string.parcel_name)) },
                leadingIcon = {
                    Icon(
                      painter = painterResource(R.drawable.ic_label),
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
              )

              OutlinedTextField(
                value = trackingId,
                onValueChange = {
                  trackingId = it
                  idError = false
                },
                singleLine = true,
                label = { Text(stringResource(R.string.tracking_id)) },
                leadingIcon = {
                    Icon(
                      painter = painterResource(R.drawable.package_2),
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  },
                modifier = Modifier.fillMaxWidth(),
                isError = idError,
                trailingIcon = {
                  if (clipboardPasteEnabled) {
                    IconButton(
                      onClick = {
                        clipboardManager.getText()?.text?.let { clipboardText ->
                          trackingId = clipboardText
                          idError = false
                        }
                      }
                    ) {
                      Icon(
                        painter = painterResource(id = R.drawable.ic_contentpaste),
                        contentDescription = stringResource(R.string.clipboard_paste),
                        modifier = Modifier.size(20.dp),
                      )
                    }
                  }
                },
                supportingText = { if (idError) Text(stringResource(R.string.tracking_id_error_text)) },
                shape = RoundedCornerShape(12.dp)
              )
              ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                  value =
                    if (service == Service.UNDEFINED) ""
                    else stringResource(getDeliveryServiceName(service)!!),
                  onValueChange = {},
                  modifier =
                    Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                  readOnly = true,
                  label = { Text(stringResource(R.string.delivery_service)) },
                  leadingIcon = {
                     Icon(
                       painter = painterResource(R.drawable.outline_local_shipping_24),
                       contentDescription = null,
                       tint = MaterialTheme.colorScheme.onSurfaceVariant
                     )
                   },
                  trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                  colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                  isError = serviceError,
                  supportingText = { if (serviceError) Text(stringResource(R.string.service_error_text)) },
                  shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                  expanded = expanded, 
                  onDismissRequest = { expanded = false },
                  modifier = Modifier
                    .background(
                      MaterialTheme.colorScheme.surface,
                      RoundedCornerShape(12.dp)
                    )
                    .border(
                      1.dp,
                      MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                      RoundedCornerShape(12.dp)
                    )
                ) {
                  var currentCategory = ""
                  sortedServiceOptions.forEach { option ->
                    val category =
                      when (option) {
                        Service.CAINIAO,
                        Service.DHL,
                        Service.GLS,
                        Service.UPS,
                        Service.FPX -> stringResource(R.string.category_international)
                        Service.UNIUNI -> stringResource(R.string.category_north_america)
                        Service.BELPOST -> stringResource(R.string.category_europe_belarus)
                        Service.SAMEDAY_BG -> stringResource(R.string.category_europe_bulgaria)
                        Service.PACKETA -> stringResource(R.string.category_europe_czech)
                        Service.DPD_UK,
                        Service.EVRI -> stringResource(R.string.category_europe_uk)
                        Service.AN_POST -> stringResource(R.string.category_europe_ireland)
                        Service.ALLEGRO_ONEBOX,
                        Service.INPOST,
                        Service.ORLEN_PACZKA,
                        Service.POLISH_POST -> stringResource(R.string.category_europe_poland)
                        Service.GLS_HUNGARY -> stringResource(R.string.category_europe_hungary)
                        Service.MAGYAR_POSTA -> stringResource(R.string.category_europe_hungary)
                        Service.SAMEDAY_HU -> stringResource(R.string.category_europe_hungary)
                        Service.DPD_GER,
                        Service.HERMES -> stringResource(R.string.category_europe_germany)
                        Service.POSTE_ITALIANE -> stringResource(R.string.category_europe_italy)
                        Service.SAMEDAY_RO -> stringResource(R.string.category_europe_romania)
                        Service.POSTNORD -> stringResource(R.string.category_europe_scandinavia)
                        Service.NOVA_POSHTA,
                        Service.UKRPOSHTA -> stringResource(R.string.category_europe_ukraine)
                        Service.EKART -> stringResource(R.string.category_asia_india)
                        Service.SPX_TH -> stringResource(R.string.category_asia_thailand)
                        Service.IMILE -> stringResource(R.string.category_europe_hungary)
                        Service.EXPRESS_ONE -> stringResource(R.string.category_europe_hungary)
                        else -> stringResource(R.string.category_other)
                      }

                    if (category != currentCategory) {
                      currentCategory = category
                      DropdownMenuItem(
                        text = {
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                          ) {
                            Icon(
                              painter = painterResource(R.drawable.outline_local_shipping_24),
                              contentDescription = null,
                              tint = MaterialTheme.colorScheme.primary,
                              modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                              text = category,
                              style = MaterialTheme.typography.labelMedium,
                              color = MaterialTheme.colorScheme.primary,
                              fontWeight = FontWeight.SemiBold
                            )
                          }
                        },
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.background(
                          MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                        )
                      )
                    }

                    DropdownMenuItem(
                      text = { 
                        Text(
                          text = "  " + stringResource(getDeliveryServiceName(option)!!),
                          style = MaterialTheme.typography.bodyMedium,
                          modifier = Modifier.padding(start = 8.dp)
                        )
                      },
                      onClick = {
                        service = option
                        expanded = false
                        serviceError = false
                      },
                      modifier = Modifier
                        .background(
                          if (service == option) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                          else Color.Transparent
                        )
                    )
                  }
                }
              }
              
              AnimatedVisibility(
                backend?.acceptsPostCode == true || backend?.requiresPostCode == true
              ) {
                Column(
                  verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                  AnimatedVisibility(backend?.acceptsPostCode == true && !backend.requiresPostCode) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween,
                      modifier = Modifier.fillMaxWidth(),
                    ) {
                      Column(modifier = Modifier.fillMaxWidth(0.8f)) {
                        Text(stringResource(R.string.specify_a_postal_code))
                        Text(
                          stringResource(R.string.specify_postal_code_flavor_text),
                          fontSize = 14.sp,
                          lineHeight = 21.sp,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                      }
                      Checkbox(checked = specifyPostalCode, onCheckedChange = { specifyPostalCode = it })
                    }
                  }

                  AnimatedVisibility(
                    backend?.requiresPostCode == true ||
                      (backend?.requiresPostCode == false && backend.acceptsPostCode && specifyPostalCode)
                  ) {
                    OutlinedTextField(
                      value = postalCode,
                      onValueChange = {
                        postalCode = it
                        postalCodeError = false
                      },
                      singleLine = true,
                      label = { Text(stringResource(R.string.postal_code)) },
                      leadingIcon = {
                         Icon(
                             painter = painterResource(R.drawable.outline_pin_drop_24),
                           contentDescription = null,
                           tint = MaterialTheme.colorScheme.onSurfaceVariant
                         )
                       },
                      modifier = Modifier.fillMaxWidth(),
                      isError = postalCodeError,
                      supportingText = {
                        if (postalCodeError) Text(stringResource(R.string.postal_code_error_text))
                      },
                      shape = RoundedCornerShape(12.dp)
                    )
                  }
                }
              }
            }
          }
          Button(
            onClick = {
              val isOk = validateInputs()
              if (isOk) {
                onCompleted(
                  Parcel(
                    id = parcel?.id ?: 0,
                    humanName = humanName.ifBlank { context.getString(R.string.undefinied_packagename) },
                    parcelId = trackingId,
                    service = service,
                    postalCode =
                      if (
                        backend?.requiresPostCode == true ||
                          (backend?.acceptsPostCode == true && specifyPostalCode)
                      )
                        postalCode
                      else null,
                  )
                )
              }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text(stringResource(if (isEdit) R.string.save else R.string.add_parcel))
          }
           
           Spacer(modifier = Modifier.height(16.dp))
         }
 }

@Composable
@PreviewLightDark
fun AddParcelPreview() {
  ParcelTrackerTheme { AddEditParcelView(null, onBackPressed = {}, onCompleted = {}) }
}

@Composable
@PreviewLightDark
fun EditParcelPreview() {
  ParcelTrackerTheme {
    AddEditParcelView(
      Parcel(0, "Test", "Test", null, Service.EXAMPLE),
      onBackPressed = {},
      onCompleted = {},
    )
  }
}
