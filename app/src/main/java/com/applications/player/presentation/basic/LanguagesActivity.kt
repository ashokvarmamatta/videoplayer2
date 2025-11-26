package com.applications.player.presentation.basic

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.applications.player.R
import com.applications.player.databinding.ActivityLanguagesBinding
import com.applications.player.databinding.ItemLanguageBinding



import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val languageFlagMap = mapOf(
    "en" to R.drawable.flag_en,
    "ar" to R.drawable.flag_ar,
    "bg" to R.drawable.flag_bg,
    "bn" to R.drawable.flag_bn,
    "ca" to R.drawable.flag_ca,
    "cs" to R.drawable.flag_cs,
    "da" to R.drawable.flag_da,
    "de" to R.drawable.flag_de,
    "el" to R.drawable.flag_el,
    "es" to R.drawable.flag_es,
    "fi" to R.drawable.flag_fi,
    "fr" to R.drawable.flag_fr,
    "hi" to R.drawable.flag_hi,
    "hr" to R.drawable.flag_hr,
    "hu" to R.drawable.flag_hu,
    "in" to R.drawable.flag_in,
    "it" to R.drawable.flag_it,
    "iw" to R.drawable.flag_iw,
    "ja" to R.drawable.flag_ja,
    "ko" to R.drawable.flag_ko,
    "lt" to R.drawable.flag_lt,
    "lv" to R.drawable.flag_lv,
    "nb" to R.drawable.flag_nb,
    "nl" to R.drawable.flag_nl,
    "pl" to R.drawable.flag_pl,
    "pt" to R.drawable.flag_pt,
    "ro" to R.drawable.flag_ro,
    "ru" to R.drawable.flag_ru,
    "sk" to R.drawable.flag_sk,
    "sl" to R.drawable.flag_sl,
    "sr" to R.drawable.flag_sr,
    "sv" to R.drawable.flag_sv,
    "ta" to R.drawable.flag_ta,
    "te" to R.drawable.flag_te,
    "th" to R.drawable.flag_th,
    "tr" to R.drawable.flag_tr,
    "ur" to R.drawable.flag_ur,
    "uk" to R.drawable.flag_uk,
    "vi" to R.drawable.flag_vi,
    "zh-rCN" to R.drawable.flag_zh_rcn,
    "zh-rTW" to R.drawable.flag_zh_rtw
)


class LanguagesActivity : AppCompatActivity() {

    private val languageMap = mapOf(
        "en" to "English",
        "ar" to "العربية",
        "bg" to "български",
        "bn" to "বাংলা",
        "ca" to "Català",
        "cs" to "Čeština",
        "da" to "Dansk",
        "de" to "Deutsch",
        "el" to "Ελληνικά",
        "es" to "Español",
        "fi" to "Suomi",
        "fr" to "Français",
        "hi" to "हिन्दी",
        "hr" to "Hrvatski",
        "hu" to "Magyar",
        "in" to "Bahasa Indonesia",
        "it" to "Italiano",
        "iw" to "עברית",
        "ja" to "日本語",
        "ko" to "한국어",
        "lt" to "Lietuvių",
        "lv" to "Latviešu",
        "no" to "Norwegian",
        "nl" to "Nederlands",
        "pl" to "Polski",
        "pt" to "Português",
        "ro" to "Română",
        "ru" to "Русский",
        "sk" to "Slovenčina",
        "sl" to "Slovenščina",
        "sr" to "Српски",
        "sv" to "Svenska",
        "ta" to "தமிழ்",
        "te" to "తెలుగు",
        "th" to "ไทย",
        "tr" to "Türkçe",
        "ur" to "اردو",
        "uk" to "Українська",
        "vi" to "Tiếng Việt",
        "zh-rCN" to "简体中文",
        "zh-rTW" to "繁體中文"
    )

    private var isSplash = true /// true for Splash and false for HomeAct
    private val binding by lazy { ActivityLanguagesBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left,0, systemBars.right, systemBars.bottom)
            insets
        }

        val sp = getSharedPreferences("prefs", MODE_PRIVATE)
        val languageSelected = sp.getBoolean("languageSelected", false)
        val languageCode = sp.getString("language", "en")

        isSplash = getIntent().getBooleanExtra("isSplash", isSplash)

        if (languageSelected) {
            if (isSplash) {
                var isFirst= getSharedPreferences("prefs", MODE_PRIVATE)
                    .getBoolean("isFirst",false)
                if (isFirst) {
                    setUpRV(languageCode = languageCode)
                } else {
                    /*startActivity(
                        Intent().setClass(
                            this@LanguagesActivity,
                            WelcomeActivity::class.java
                        )
                    )*/
                    finish()
                }

            } else {
                setUpRV(languageCode)
               //val appLocale = LocaleListCompat.forLanguageTags(languageCode!!)
               //AppCompatDelegate.setApplicationLocales(appLocale)
               //startActivity(Intent().setClass(this@LanguagesActivity, IntroActivity::class.java))
               //finish()
            }
        } else {
            setUpRV(languageCode)
        }

        binding.btnContinue.setOnClickListener {
            val languageCode = sp.getString("language", "en")
           getSharedPreferences("prefs", MODE_PRIVATE)
                .edit().putBoolean("languageSelected", true)
                .apply()
            val appLocale = LocaleListCompat.forLanguageTags(languageCode)
            AppCompatDelegate.setApplicationLocales(appLocale)
            if (isSplash) {
                /*startActivity(
                    Intent(
                        getApplicationContext(),
                        WelcomeActivity::class.java
                    )
                )*/
              /*  startActivity(
                    Intent().setClass(
                        this@LanguagesActivity,
                        IntroActivity::class.java
                    )
                )*/
                finishAffinity()
            }


        finish()
        }


    }

    private fun setUpRV(languageCode: String?) {
        binding.languagesList.apply {
            layoutManager = LinearLayoutManager(this@LanguagesActivity)
            adapter = LanguageAdapter(languageMap, languageCode) { languageCode ->
               // binding.progressLayout.isVisible = true
                CoroutineScope(Dispatchers.IO).launch {
                    delay(500)
                    getSharedPreferences("prefs", MODE_PRIVATE).edit()
                        .putString("language", languageCode).putBoolean("languageSelected", true)
                        .apply()
                    withContext(Dispatchers.Main) {
                        adapter!!.notifyDataSetChanged()


                        /*val appLocale = LocaleListCompat.forLanguageTags(languageCode)
                       AppCompatDelegate.setApplicationLocales(appLocale)
                        binding.progressLayout.isVisible = false
                        Toast.makeText(
                            this@LanguagesActivity,
                            "Language changed to ${languageMap[languageCode]}",
                            Toast.LENGTH_SHORT
                        ).show()*/
                    }
                }
            }
        }
    }


    companion object {
        @JvmStatic
        fun getIntent(activity: Activity, isSplash: Boolean = false) = Intent()
            .setClass(activity, LanguagesActivity::class.java)
            .putExtra("isSplash", isSplash)
    }

}

class LanguageAdapter(
    languages: Map<String, String>,
    private var selectedLanguage: String?,
    private val onLanguageSelected: (String) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    private val languageList = languages.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_language, parent, false)
        return LanguageViewHolder(view)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        val (code, name) = languageList[position]
        holder.bind(code, name)
    }

    override fun getItemCount() = languageList.size

    inner class LanguageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = ItemLanguageBinding.bind(view)

        fun bind(code: String, name: String) {
            binding.languageName.text = name
            binding.languageIcon.setImageResource(languageFlagMap[code] ?: R.drawable.error_flag)
            binding.root.setOnClickListener {
                selectedLanguage=code
                onLanguageSelected(code)

            notifyDataSetChanged()
            }
            binding.check.setImageResource(if (code == selectedLanguage) R.drawable.circle_primary else R.drawable.circle_primary_transparent)
        }
    }

}