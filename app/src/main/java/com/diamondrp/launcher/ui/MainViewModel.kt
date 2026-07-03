package com.diamondrp.launcher.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diamondrp.launcher.network.models.NewsItem
import com.diamondrp.launcher.network.models.ServerConfig
import com.diamondrp.launcher.network.models.ServerStatus
import com.diamondrp.launcher.repository.ConfigRepository
import com.diamondrp.launcher.repository.ServerRepository
import com.diamondrp.launcher.storage.preferences.PreferencesManager
import com.diamondrp.launcher.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MainUiState(
    val isLoadingConfig: Boolean = true,
    val config: ServerConfig? = null,
    val serverStatus: ServerStatus = ServerStatus.offline(),
    val isQueryingStatus: Boolean = false,
    val news: List<NewsItem> = emptyList(),
    val nickname: String = "",
    val errorMessage: String? = null
)

class MainViewModel(
    private val configRepository: ConfigRepository,
    private val serverRepository: ServerRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadSavedNickname()
    }

    private fun loadSavedNickname() {
        viewModelScope.launch {
            val nickname = preferencesManager.getNickname()
            _uiState.value = _uiState.value.copy(nickname = nickname)
        }
    }

    fun loadConfig() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingConfig = true, errorMessage = null)

            when (val result = configRepository.fetchRemoteConfig()) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingConfig = false,
                        config = result.data,
                        news = result.data.news
                    )
                    queryServerStatus(result.data.ip, result.data.port)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingConfig = false,
                        errorMessage = result.message
                    )
                }
                Resource.Loading -> Unit
            }
        }
    }

    private fun queryServerStatus(ip: String, port: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isQueryingStatus = true)
            val status = serverRepository.queryServer(ip, port)
            _uiState.value = _uiState.value.copy(isQueryingStatus = false, serverStatus = status)
        }
    }

    fun onNicknameChanged(nickname: String) {
        _uiState.value = _uiState.value.copy(nickname = nickname, errorMessage = null)
        viewModelScope.launch { preferencesManager.setNickname(nickname) }
    }

    fun refresh() {
        val config = _uiState.value.config
        if (config != null) {
            queryServerStatus(config.ip, config.port)
        } else {
            loadConfig()
        }
    }

    /** Retorna null se válido, ou uma mensagem de erro para exibir na UI. */
    fun validateNicknameForPlay(): String? {
        val nickname = _uiState.value.nickname.trim()
        return if (nickname.isEmpty()) "error_nickname_empty" else null
    }
}

class MainViewModelFactory(
    private val configRepository: ConfigRepository,
    private val serverRepository: ServerRepository,
    private val preferencesManager: PreferencesManager
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(configRepository, serverRepository, preferencesManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
