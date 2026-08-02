package com.bicy.whitenoise.onlinemusic.model.SourceModelsPart

typealias Source = String

object Sources {
    const val KW = "kw"
    const val KG = "kg"
    const val TX = "tx"
    const val WY = "wy"
    const val MG = "mg"
    const val LOCAL = "local"

    val ONLINE = listOf(KW, KG, TX, WY, MG)
    val ALL = listOf(KW, KG, TX, WY, MG, LOCAL)
}

typealias Quality = String

object Qualities {
    const val _128K = "128k"
    const val _192K = "192k"
    const val _320K = "320k"
    const val APE = "ape"
    const val FLAC = "flac"
    const val FLAC24BIT = "flac24bit"
    const val WAV = "wav"

    val ALL = listOf(FLAC24BIT, FLAC, WAV, APE, _320K, _192K, _128K)
}

val SUPPORT_QUALITIES: Map<Source, List<Quality>> = mapOf(
    Sources.KW to listOf(Qualities._128K, Qualities._320K, Qualities.FLAC, Qualities.FLAC24BIT),
    Sources.KG to listOf(Qualities._128K, Qualities._320K, Qualities.FLAC, Qualities.FLAC24BIT),
    Sources.TX to listOf(Qualities._128K, Qualities._320K, Qualities.FLAC, Qualities.FLAC24BIT),
    Sources.WY to listOf(Qualities._128K, Qualities._320K, Qualities.FLAC, Qualities.FLAC24BIT),
    Sources.MG to listOf(Qualities._128K, Qualities._320K, Qualities.FLAC, Qualities.FLAC24BIT),
    Sources.LOCAL to emptyList(),
)

val SUPPORT_ACTIONS: Map<Source, List<String>> = mapOf(
    Sources.KW to listOf("musicUrl", "search"),
    Sources.KG to listOf("musicUrl", "search"),
    Sources.TX to listOf("musicUrl", "search"),
    Sources.WY to listOf("musicUrl", "search"),
    Sources.MG to listOf("musicUrl", "search"),
    Sources.LOCAL to listOf("musicUrl", "lyric", "pic"),
)

object SourceAction {
    const val MUSIC_URL = "musicUrl"
    const val LYRIC = "lyric"
    const val PIC = "pic"
    const val SEARCH = "search"
}
