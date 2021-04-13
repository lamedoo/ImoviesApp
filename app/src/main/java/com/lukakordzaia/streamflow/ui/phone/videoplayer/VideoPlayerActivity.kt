package com.lukakordzaia.streamflow.ui.phone.videoplayer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.cast.framework.CastContext
import com.lukakordzaia.streamflow.R

class VideoPlayerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CastContext.getSharedInstance(this)
        setContentView(R.layout.activity_phone_video_player)
    }
}