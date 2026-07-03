package com.diamondrp.launcher.utils

object Constants {

    /**
     * URL do config.json remoto. Troque para o endpoint real do seu servidor/CDN
     * antes de gerar o build de produção (ex: GitHub Pages, S3, seu próprio backend).
     */
    const val REMOTE_CONFIG_URL = "https://raw.githubusercontent.com/diamondrp/launcher-config/main/config.json"

    const val PREFS_NAME = "diamond_rp_prefs"
    const val KEY_NICKNAME = "key_nickname"
    const val KEY_THEME = "key_theme"
    const val KEY_LANGUAGE = "key_language"
    const val KEY_LAST_CONFIG_CACHE = "key_last_config_cache"
    const val KEY_INSTALLED_VERSION = "key_installed_version"

    const val DOWNLOAD_FOLDER = "diamondrp_downloads"
    const val GAME_DATA_FOLDER = "diamondrp_gamedata"

    const val SAMP_CLIENT_PACKAGE = "com.rockstargames.gtasa" // ajustar para o package real do cliente SA-MP Mobile usado
    const val SAMP_QUERY_TIMEOUT_MS = 3000

    const val DOWNLOAD_CHANNEL_ID = "diamond_rp_download_channel"
    const val DOWNLOAD_NOTIFICATION_ID = 1001
}
