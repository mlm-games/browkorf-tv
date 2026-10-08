package org.mlm.browkorftv.settings

import io.github.mlmgames.settings.core.annotations.SettingValidator
import io.github.mlmgames.settings.core.annotations.ValidationResult
import org.mlm.browkorftv.network.parseProxyUrl

object ProxyUrlValidator : SettingValidator<String> {
    override fun validate(value: String): ValidationResult =
        if (value.isBlank() || runCatching { parseProxyUrl(value) }.isSuccess) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(
                message = "Invalid proxy URL",
                messageKey = BrowkorfSettingsKeys.PROXY_URL_INVALID,
            )
        }
}