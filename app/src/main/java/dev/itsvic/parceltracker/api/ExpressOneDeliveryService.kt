package dev.itsvic.parceltracker.api

import dev.itsvic.parceltracker.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import okhttp3.Request

object ExpressOneDeliveryService : DeliveryService {
  override val nameResource: Int = R.string.service_express_one
  override val acceptsPostCode: Boolean = false
  override val requiresPostCode: Boolean = false

  override suspend fun getParcel(trackingId: String, postalCode: String?): Parcel {
    val url = "https://tracking.expressone.hu/?plc_number=${trackingId}&sender_id="

    val request = Request.Builder().url(url).build()

    val response = api_client.newCall(request).execute()

    if (!response.isSuccessful) {
      throw ParcelNonExistentException()
    }

    val html = response.body?.string() ?: throw ParcelNonExistentException()

    if (html.contains("Nincs találat") || html.contains("No results") || html.contains("error")) {
      throw ParcelNonExistentException()
    }

    var currentStatus = Status.Unknown
    val htmlLower = html.lowercase(Locale.getDefault())

    when {
      htmlLower.contains("kézbesítve") || htmlLower.contains("delivered") -> {
        currentStatus = Status.Delivered
      }
      htmlLower.contains("kiszállítás") ||
          htmlLower.contains("out for delivery") ||
          htmlLower.contains("kiadva futárnak") -> {
        currentStatus = Status.OutForDelivery
      }
      htmlLower.contains("depóba érkezett") || htmlLower.contains("arrived at depot") -> {
        currentStatus = Status.InWarehouse
      }
      htmlLower.contains("feldolgozás") ||
          htmlLower.contains("processing") ||
          htmlLower.contains("átszállítás") -> {
        currentStatus = Status.InTransit
      }
      htmlLower.contains("átvétel") || htmlLower.contains("pickup") -> {
        currentStatus = Status.AwaitingPickup
      }
      else -> {
        currentStatus = Status.InTransit
      }
    }

    val history = mutableListOf<ParcelHistoryItem>()

    val datePattern = "<h5><span>(\\d{4}-\\d{2}-\\d{2})</span></h5>".toRegex()
    val rowPattern =
        "<td[^>]*>(\\d{2}:\\d{2}:\\d{2})</td>\\s*<td[^>]*>([^<]*)</td>\\s*<td[^>]*>([^<]*)</td>"
            .toRegex()

    val dateMatches = datePattern.findAll(html)

    for (dateMatch in dateMatches) {
      val dateText = dateMatch.groupValues[1]

      val startIndex = dateMatch.range.last
      val nextDateMatch = datePattern.find(html, startIndex + 1)
      val endIndex = nextDateMatch?.range?.first ?: html.length

      val tableSection = html.substring(startIndex, endIndex)
      val rowMatches = rowPattern.findAll(tableSection)

      for (rowMatch in rowMatches) {
        val timeText = rowMatch.groupValues[1]
        val codeText = rowMatch.groupValues[2].trim()
        val descriptionText = rowMatch.groupValues[3].trim()

        if (timeText.isNotEmpty() && descriptionText.isNotEmpty()) {
          try {
            val dateTimeString = "$dateText $timeText"
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val dateTime = LocalDateTime.parse(dateTimeString, formatter)

            var cleanDescription = descriptionText.replace("&nbsp;", " ").trim()
            val locationMatch = "\\(([^)]+)\\)".toRegex().find(cleanDescription)
            val location = locationMatch?.groupValues?.get(1) ?: ""

            if (location.isNotEmpty()) {
              cleanDescription = cleanDescription.replace("($location)", "").trim()
            }

            history.add(ParcelHistoryItem(cleanDescription, dateTime, location))
          } catch (e: Exception) {
            continue
          }
        }
      }
    }

    history.sortByDescending { it.time }

    if (history.isEmpty()) {
      history.add(
          ParcelHistoryItem(
              "Csomag nyomon követése elindítva", LocalDateTime.now(), "Express One Hungary"))
    }

    return Parcel(trackingId, history, currentStatus)
  }
}
