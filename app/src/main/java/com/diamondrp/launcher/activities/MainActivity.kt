package com.diamondrp.launcher.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.diamondrp.launcher.R
import com.diamondrp.launcher.databinding.ActivityMainBinding
import com.diamondrp.launcher.network.RetrofitClient
import com.diamondrp.launcher.network.models.NewsItem
import com.diamondrp.launcher.network.models.ServerStatus
import com.diamondrp.launcher.repository.ConfigRepository
import com.diamondrp.launcher.repository.ServerRepository
import com.diamondrp.launcher.storage.preferences.PreferencesManager
import com.diamondrp.launcher.ui.MainViewModel
import com.diamondrp.launcher.ui.MainViewModelFactory
import com.diamondrp.launcher.ui.widgets.NewsAdapter
import com.diamondrp.launcher.utils.Constants
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var newsAdapter: NewsAdapter

    private val viewModel: MainViewModel by lazy {
        ViewModelProvider(
            this,
            MainViewModelFactory(
                ConfigRepository(RetrofitClient.apiService),
                ServerRepository(),
                PreferencesManager(applicationContext)
            )
        )[MainViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNewsList()
        setupListeners()
        observeUiState()

        viewModel.loadConfig()
    }

    private fun setupNewsList() {
        newsAdapter = NewsAdapter(onNewsClick = ::openNewsLink)
        binding.rvNews.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = newsAdapter
        }
    }

    private fun setupListeners() {
        binding.edtNickname.addTextChangedListener {
            viewModel.onNicknameChanged(it?.toString().orEmpty())
        }

        binding.btnPlay.setOnClickListener { onPlayClicked() }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.btnDiscord.setOnClickListener {
            openUrl(viewModel.uiState.value.config?.socials?.discord)
        }
        binding.btnInstagram.setOnClickListener {
            openUrl(viewModel.uiState.value.config?.socials?.instagram)
        }
        binding.btnWhatsapp.setOnClickListener {
            openUrl(viewModel.uiState.value.config?.socials?.whatsapp)
        }

        binding.cardServerStatus.setOnClickListener { viewModel.refresh() }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.edtNickname.setText(state.nickname)
                binding.txtVersion.text = state.config?.let { "v${it.version}" } ?: ""
                binding.txtServerName.text = state.config?.serverName ?: getString(R.string.app_name)

                renderServerStatus(state.serverStatus, state.isQueryingStatus)
                newsAdapter.submitList(state.news)

                state.errorMessage?.let {
                    Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun renderServerStatus(status: ServerStatus, isQuerying: Boolean) {
        binding.dotStatus.setBackgroundResource(
            if (status.isOnline) R.drawable.dot_online else R.drawable.dot_offline
        )
        binding.txtPlayersOnline.text = if (status.isOnline) {
            "${status.playersOnline}/${status.maxPlayers}"
        } else {
            getString(R.string.status_offline)
        }
        binding.txtPing.text = when {
            isQuerying -> "..."
            status.isOnline -> "${status.pingMs} ms"
            else -> "-- ms"
        }
    }

    private fun onPlayClicked() {
        val errorKey = viewModel.validateNicknameForPlay()
        if (errorKey != null) {
            Snackbar.make(binding.root, R.string.error_nickname_empty, Snackbar.LENGTH_SHORT).show()
            return
        }
        launchSampClient()
    }

    /**
     * Inicia o cliente SA-MP Mobile instalado no dispositivo, passando o nickname
     * e IP/porta do servidor via Intent. O package/ação exata depende do cliente
     * SA-MP Mobile utilizado — ajuste [Constants.SAMP_CLIENT_PACKAGE] e o esquema
     * de Intent conforme a documentação do cliente escolhido.
     */
    private fun launchSampClient() {
        val config = viewModel.uiState.value.config
        if (config == null) {
            Snackbar.make(binding.root, R.string.error_config_load, Snackbar.LENGTH_LONG).show()
            return
        }

        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(Constants.SAMP_CLIENT_PACKAGE)
            if (launchIntent != null) {
                launchIntent.putExtra("nickname", binding.edtNickname.text?.toString())
                launchIntent.putExtra("ip", config.ip)
                launchIntent.putExtra("port", config.port)
                startActivity(launchIntent)
            } else {
                Snackbar.make(binding.root, R.string.error_client_not_found, Snackbar.LENGTH_LONG).show()
            }
        } catch (e: ActivityNotFoundException) {
            Snackbar.make(binding.root, R.string.error_client_not_found, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun openNewsLink(item: NewsItem) {
        openUrl(item.url)
    }

    private fun openUrl(url: String?) {
        if (url.isNullOrBlank()) return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            Snackbar.make(binding.root, "Não foi possível abrir o link.", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.imgLogoSmall.isVisible = true
    }
}

/** Pequena extensão local para simplificar o listener de texto do EditText. */
private fun com.google.android.material.textfield.TextInputEditText.addTextChangedListener(
    onChanged: (CharSequence?) -> Unit
) {
    this.addTextChangedListener(object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = onChanged(s)
        override fun afterTextChanged(s: android.text.Editable?) = Unit
    })
}
