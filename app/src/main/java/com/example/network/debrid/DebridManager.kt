package com.example.network.debrid

import android.util.Log

class DebridManager(
    private val torboxKeyProvider: () -> String,
    private val realDebridKeyProvider: () -> String
) {
    private val tag = "DebridManager"

    val torboxProvider = TorboxProvider(torboxKeyProvider)
    val realDebridProvider = RealDebridProvider(realDebridKeyProvider)

    fun hasAnyProviderConfigured(): Boolean {
        return torboxProvider.isConfigured() || realDebridProvider.isConfigured()
    }

    suspend fun resolve(queryOrMagnet: String): DebridResult {
        val torboxConfigured = torboxProvider.isConfigured()
        val rdConfigured = realDebridProvider.isConfigured()

        if (!torboxConfigured && !rdConfigured) {
            return DebridResult.Error(
                message = "No Debrid provider is configured. Please add your Torbox or Real-Debrid API key in Settings.",
                providerName = "None"
            )
        }

        // Try Torbox first if configured
        if (torboxConfigured) {
            Log.d(tag, "Attempting stream resolution via Torbox...")
            val result = torboxProvider.resolveStream(queryOrMagnet)
            if (result is DebridResult.Success) {
                return result
            }
            Log.w(tag, "Torbox resolution did not succeed (${(result as? DebridResult.Error)?.message}), checking fallback...")
        }

        // Fallback to Real-Debrid if configured
        if (rdConfigured) {
            Log.d(tag, "Attempting stream resolution via Real-Debrid...")
            val result = realDebridProvider.resolveStream(queryOrMagnet)
            if (result is DebridResult.Success) {
                return result
            }
            Log.w(tag, "Real-Debrid resolution did not succeed (${(result as? DebridResult.Error)?.message})")
            return result
        }

        return DebridResult.Error(
            message = "Failed to resolve stream with configured Debrid provider(s)",
            providerName = "Debrid"
        )
    }
}
