package ru.profikrol.operator.feature.settings

import androidx.annotation.StringRes
import ru.profikrol.operator.R

enum class AppLanguage(
    val code: String,
    @StringRes val labelRes: Int,
) {
    Russian(code = "ru", labelRes = R.string.settings_language_ru),
    English(code = "en", labelRes = R.string.settings_language_en),
    Uzbek(code = "uz", labelRes = R.string.settings_language_uz),
}
