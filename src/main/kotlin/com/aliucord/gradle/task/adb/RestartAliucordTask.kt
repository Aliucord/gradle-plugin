/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.aliucord.gradle.task.adb

import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Force (re)starts the main Aliucord activity on configured devices.
 */
@DisableCachingByDefault
public abstract class RestartAliucordTask : AdbTask() {
    init {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    public fun run() {
        val args = arrayListOf(
            "start",
            "-S", // Force restart app
            "-n", "com.aliucord/com.discord.app.AppActivity\$Main",
        )
        if (this.waitForDebugger.get())
            args += "-D"

        this.runAdbShell("am", *args.toTypedArray())

        logger.lifecycle("Restarted Aliucord on configured devices")
    }
}
