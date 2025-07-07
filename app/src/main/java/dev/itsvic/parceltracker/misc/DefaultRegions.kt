package dev.itsvic.parceltracker.misc

// This is required for the UPS backend, because for some reason it requires
// the region in the locale, and Java does not provide that info unless it was
// provided to it beforehand.
// tldr i18n sucks
val defaultRegionsForLanguageCode =
    mapOf("pl" to "PL", "hu" to "HU", "uk" to "UA", "th" to "TH", "cs" to "CZ")
