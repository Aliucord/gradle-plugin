package com.aliucord.gradle.models

import com.aliucord.gradle.models.serializers.IntAsStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Build info for Aliucord core.
 */
@Serializable
internal data class BuildInfo(
    @Serializable(with = IntAsStringSerializer::class)
    @SerialName("versionCode")
    val discordVersionCode: Int,

    @SerialName("versionName")
    val discordVersionName: Int,

    //    @SerialName("injectorVersion")
    //    val injectorVersion: SemVer,
    //
    //    @SerialName("patchesVersion")
    //    val patchesVersion: SemVer,
)
