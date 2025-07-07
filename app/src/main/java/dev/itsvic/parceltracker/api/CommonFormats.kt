package dev.itsvic.parceltracker.api

val emsFormat = """^\w{2}\d{9}\w{2}$""".toRegex()
val digits11Format = """^\d{11}$""".toRegex()
val digits12Format = """^\d{12}$""".toRegex()
val digits18Format = """^\d{18}$""".toRegex()

fun Regex.accepts(string: String): Boolean {
  return this.matchEntire(string) != null
}
