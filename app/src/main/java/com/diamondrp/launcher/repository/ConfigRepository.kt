package com.diamondrp.launcher.repository

import com.diamondrp.launcher.network.api.ApiService
import com.diamondrp.launcher.network.models.ServerConfig
import com.diamondrp.launcher.utils.Constants
import com.diamondrp.launcher.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Responsável por buscar o config.json remoto que controla todo o comportamento
 * do launcher (versão, IP do servidor, notícias, link de download, etc).
 */
class ConfigRepository(private val apiService: ApiService) {

    suspend fun fetchRemoteConfig(): Resource<ServerConfig> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getServerConfig(Constants.REMOTE_CONFIG_URL)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Resource.Success(body)
                } else {
                    Resource.Error("Resposta vazia do servidor de configuração.")
                }
            } else {
                Resource.Error("Erro ao buscar configuração (HTTP ${response.code()}).")
            }
        } catch (e: IOException) {
            Resource.Error("Falha de conexão. Verifique sua internet.", e)
        } catch (e: Exception) {
            Resource.Error("Erro inesperado ao carregar configuração: ${e.message}", e)
        }
    }
}
