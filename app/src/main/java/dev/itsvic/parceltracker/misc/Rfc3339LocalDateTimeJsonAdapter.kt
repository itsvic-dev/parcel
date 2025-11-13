package dev.itsvic.parceltracker.misc

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Rfc3339LocalDateTimeJsonAdapter : JsonAdapter<LocalDateTime>() {
  override fun fromJson(reader: JsonReader): LocalDateTime? {
    if (reader.peek() == JsonReader.Token.NULL) {
      return reader.nextNull()
    }
    val string = reader.nextString()
    return LocalDateTime.parse(string, DateTimeFormatter.ISO_DATE_TIME)
  }

  override fun toJson(p0: JsonWriter, p1: LocalDateTime?) {
    TODO()
  }
}
