package com.diamondrp.launcher.network.models

/**
 * Representa o estado ao vivo do servidor SA-MP, obtido via query UDP
 * (protocolo nativo do SA-MP, feito em [com.diamondrp.launcher.repository.ServerRepository])
 * e não via JSON remoto — por isso não é uma data class de rede Retrofit.
 */
data class ServerStatus(
    val isOnline: Boolean,
    val playersOnline: Int = 0,
    val maxPlayers: Int = 0,
    val pingMs: Long = -1,
    val hostname: String? = null,
    val gameMode: String? = null
) {
    companion object {
        fun offline() = ServerStatus(isOnline = false)
    }
}
