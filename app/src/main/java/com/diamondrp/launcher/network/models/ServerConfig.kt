package com.diamondrp.launcher.network.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Representa o config.json remoto que controla o comportamento do launcher.
 * Este arquivo é hospedado externamente (CDN, GitHub Pages, servidor próprio, etc.)
 * e é baixado toda vez que o launcher inicia.
 */
@JsonClass(generateAdapter = true)
data class ServerConfig(
    @Json(name = "serverName") val serverName: String,
    @Json(name = "ip") val ip: String,
    @Json(name = "port") val port: Int,
    @Json(name = "version") val version: String,
    @Json(name = "minClientVersion") val minClientVersion: String? = null,
    @Json(name = "downloadUrl") val downloadUrl: String,
    @Json(name = "fileSize") val fileSize: Long = 0L,
    @Json(name = "fileChecksum") val fileChecksum: String? = null,
    @Json(name = "changelog") val changelog: List<String> = emptyList(),
    @Json(name = "news") val news: List<NewsItem> = emptyList(),
    @Json(name = "socials") val socials: SocialLinks? = null,
    @Json(name = "maintenance") val maintenance: Boolean = false,
    @Json(name = "maintenanceMessage") val maintenanceMessage: String? = null
)

@JsonClass(generateAdapter = true)
data class SocialLinks(
    @Json(name = "discord") val discord: String? = null,
    @Json(name = "instagram") val instagram: String? = null,
    @Json(name = "whatsapp") val whatsapp: String? = null,
    @Json(name = "website") val website: String? = null
)
