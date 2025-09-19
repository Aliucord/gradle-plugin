package com.aliucord.gradle

import com.android.build.gradle.BaseExtension
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.kotlin.dsl.getByName

internal object Constants {
    const val TASK_GROUP = "aliucord"
    const val TASK_GROUP_INTERNAL = "aliucordInternal"
}

internal fun ExtensionContainer.getAndroid(): BaseExtension =
    getByName<BaseExtension>("android")
