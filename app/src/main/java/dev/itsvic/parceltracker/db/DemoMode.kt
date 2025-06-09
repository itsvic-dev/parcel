// SPDX-License-Identifier: GPL-3.0-or-later
package dev.itsvic.parceltracker.db

import dev.itsvic.parceltracker.api.Service
import dev.itsvic.parceltracker.api.Status
import java.time.Instant

// Parcel metadata and mock DB for demo mode.

private var demoId = 0

private fun defineDemoParcel(name: String, id: String, status: Status): ParcelWithStatus {
  val internalId = demoId++
  return ParcelWithStatus(
      Parcel(internalId, name, id, null, Service.EXAMPLE, archivePromptDismissed = true),
      ParcelStatus(internalId, status, Instant.now()))
}

val demoModeParcels =
    listOf(
        defineDemoParcel("Phone case", "2503894188", Status.Delivered),
        defineDemoParcel("Keyring tracker", "7301626157", Status.AwaitingPickup),
        defineDemoParcel("Game merch", "6171197286", Status.Preadvice),
    )
