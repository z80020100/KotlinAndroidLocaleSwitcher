package com.example.localeswitcher

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.LocaleList
import android.provider.Settings
import android.util.Log
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.util.Locale

object SystemLocaleSwitcher {
    private const val TAG = "LocaleSwitcher"
    enum class Result { UPDATED, PERMISSION_REQUIRED, FAILED }

    fun hasPermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.CHANGE_CONFIGURATION) ==
            PackageManager.PERMISSION_GRANTED && Settings.System.canWrite(context)

    fun switchTo(context: Context, languageTag: String): Result {
        require(languageTag == "en-US" || languageTag == "ja-JP")
        val changeConfigurationGranted = context.checkSelfPermission(
            Manifest.permission.CHANGE_CONFIGURATION
        ) == PackageManager.PERMISSION_GRANTED
        val writeSettingsAllowed = Settings.System.canWrite(context)
        Log.i(TAG, "Switch requested. changeConfigurationGranted=$changeConfigurationGranted")
        Log.i(TAG, "Write settings access checked. allowed=$writeSettingsAllowed")
        if (!changeConfigurationGranted || !writeSettingsAllowed) {
            Log.w(TAG, "Switch rejected. Required permissions are missing.")
            return Result.PERMISSION_REQUIRED
        }
        var stage = "update_locales"
        return try {
            val locales = LocaleList(Locale.forLanguageTag(languageTag))
            Log.i(TAG, "LocalePicker.updateLocales started.")
            HiddenApiBypass.invoke(
                Class.forName("com.android.internal.app.LocalePicker"), null, "updateLocales", locales
            )
            Log.i(TAG, "LocalePicker.updateLocales returned. The result is not yet verified.")
            stage = "get_service"
            val manager = HiddenApiBypass.invoke(
                Class.forName("android.app.ActivityManager"), null, "getService"
            )
            Log.i(TAG, "ActivityManager.getService returned. servicePresent=${manager != null}")
            stage = "get_configuration"
            val configuration = HiddenApiBypass.invoke(
                Class.forName("android.app.IActivityManager"), manager, "getConfiguration"
            ) as Configuration
            val matches = configuration.locales == locales
            Log.i(TAG, "System configuration checked. matchesRequestedLocales=$matches")
            if (matches) Result.UPDATED else Result.FAILED
        } catch (error: Exception) {
            logFailure(stage, error)
            if (hasPermission(context)) Result.FAILED else Result.PERMISSION_REQUIRED
        } catch (error: LinkageError) {
            logFailure(stage, error)
            Result.FAILED
        }
    }

    private fun logFailure(stage: String, error: Throwable) {
        Log.e(TAG, "Switch failed. stage=$stage")
        var cause: Throwable? = error
        repeat(8) { depth ->
            val current = cause ?: return
            Log.e(TAG, "Exception type. depth=$depth type=${current.javaClass.name}")
            current.stackTrace.take(8).forEach { frame ->
                Log.e(TAG, "Stack frame. class=${frame.className} method=${frame.methodName} line=${frame.lineNumber}")
            }
            cause = current.cause
        }
    }
}
