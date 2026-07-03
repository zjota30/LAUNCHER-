package com.diamondrp.launcher.network.api

import com.diamondrp.launcher.network.models.ServerConfig
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * Endpoints remotos usados pelo launcher.
 *
 * A URL completa do config.json é fornecida em runtime (via [com.diamondrp.launcher.utils.Constants]),
 * por isso usamos @Url em vez de um path fixo — permite trocar o host de configuração
 * sem precisar recompilar o app.
 */
interface ApiService {

    @GET
    suspend fun getServerConfig(@Url url: String): Response<ServerConfig>
}
