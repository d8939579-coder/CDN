package com.example.cdn.utils

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object RootUtils {

    /**
     * Checks if the device is rooted by checking standard root indicators.
     * @return true if rooted, false otherwise.
     */
    fun isDeviceRooted(): Boolean {
        val paths = listOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su"
        )

        return paths.any { File(it).exists() }
    }

    /**
     * Executes a command as root.
     * @param command the command to execute.
     * @return Result of the command execution, or an error message if it fails.
     */
    fun executeCommand(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                output.toString()
            } else {
                "Error executing command: Exit code $exitCode"
            }
        } catch (e: Exception) {
            "Exception: ${e.localizedMessage}"
        }
    }
}

