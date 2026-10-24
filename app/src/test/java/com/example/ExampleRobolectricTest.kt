package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ApexFrame", appName)
  }

  @Test
  fun `verify default recording settings and effective bitrate`() {
    val settings = RecordingSettings()
    assertEquals(ResolutionOption.FHD_1080P, settings.resolution)
    assertEquals(FpsOption.FPS_60, settings.fps)
    assertEquals(AudioSourceOption.DUAL_MIXED, settings.audioSource)
    assertEquals(3, settings.countdownSeconds)
    assertEquals(true, settings.stealthOverlayEnabled)
    assertEquals(12_000_000, settings.calculateEffectiveBitrate())
  }
}
