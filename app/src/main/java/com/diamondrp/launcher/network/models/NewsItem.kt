package com.diamondrp.launcher.network.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NewsItem(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String,
    @Json(name = "imageUrl") val imageUrl: String? = null,
    @Json(name = "date") val date: String? = null,
    @Json(name = "url") val url: String? = null
)
