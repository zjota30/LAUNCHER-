package com.diamondrp.launcher.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.diamondrp.launcher.BuildConfig
import com.diamondrp.launcher.databinding.ActivityAboutBinding
import com.diamondrp.launcher.network.RetrofitClient
import com.diamondrp.launcher.repository.ConfigRepository
import com.diamondrp.launcher.utils.Resource
import com.google.android.material.snackbar.Snackbar
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class AboutActivity : BaseActivity() {

    private lateinit var binding: ActivityAboutBinding
    private val configRepository by lazy { ConfigRepository(RetrofitClient.apiService) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.txtVersion.text = "Versão ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})"

        binding.btnBack.setOnClickListener { finish() }

        loadSocialLinks()
    }

    private fun loadSocialLinks() {
        lifecycleScope.launch {
            when (val result = configRepository.fetchRemoteConfig()) {
                is Resource.Success -> {
                    val socials = result.data.socials
                    binding.btnDiscord.setOnClickListener { openUrl(socials?.discord) }
                    binding.btnInstagram.setOnClickListener { openUrl(socials?.instagram) }
                    binding.btnWhatsapp.setOnClickListener { openUrl(socials?.whatsapp) }
                }
                else -> Unit
            }
        }
    }

    private fun openUrl(url: String?) {
        if (url.isNullOrBlank()) return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            Snackbar.make(binding.root, "Não foi possível abrir o link.", Snackbar.LENGTH_SHORT).show()
        }
    }
}
