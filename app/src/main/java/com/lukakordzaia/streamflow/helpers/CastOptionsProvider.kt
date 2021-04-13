package com.lukakordzaia.streamflow.helpers

import android.content.Context
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.lukakordzaia.streamflow.R

object CastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions {
        return CastOptions.Builder()
            .setReceiverApplicationId(context.getString(R.string.cast_app_id))
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context?): List<SessionProvider>? {
        return null
    }
}