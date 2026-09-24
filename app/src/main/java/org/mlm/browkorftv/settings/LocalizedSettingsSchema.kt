package org.mlm.browkorftv.settings

import io.github.mlmgames.settings.core.ConfirmationConfig
import io.github.mlmgames.settings.core.SettingField
import io.github.mlmgames.settings.core.SettingMeta
import io.github.mlmgames.settings.core.SettingsSchema

class LocalizedSettingsSchema<T>(
    private val delegate: SettingsSchema<T>,
    private val titleResources: Map<String, Int> = emptyMap(),
    private val descriptionResources: Map<String, Int> = emptyMap(),
    private val optionsResources: Map<String, Int> = emptyMap(),
    private val confirmationResources: Map<String, ConfirmationConfig> = emptyMap(),
) : SettingsSchema<T> {
    override val default: T
        get() = delegate.default

    override val fields: List<SettingField<T, *>> = delegate.fields.map { field ->
        val meta = field.meta ?: return@map field
        val localizedMeta = meta.copy(
            titleRes = titleResources[field.name] ?: meta.titleRes,
            descriptionRes = descriptionResources[field.name] ?: meta.descriptionRes,
            optionsRes = optionsResources[field.name] ?: meta.optionsRes,
            confirmation = confirmationResources[field.name] ?: meta.confirmation,
        )
        if (localizedMeta == meta) field else LocalizedSettingField(field, localizedMeta)
    }

    private class LocalizedSettingField<T, V>(
        delegate: SettingField<T, V>,
        override val meta: SettingMeta,
    ) : SettingField<T, V> by delegate
}
