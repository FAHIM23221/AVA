package com.fahimkhan.ava.core.utils

import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Small shared state between the voice-command side (MainViewModel) and
 * the camera screen (VisionScreen), which live in different ViewModels.
 *
 * - [pendingCapture]: a voice command like "ছবি তোলো" sets this true; the
 *   camera screen takes the photo once its camera is ready, then sets it
 *   back to false. A held flag (not a one-shot event) so a request made
 *   just before the screen opens isn't lost.
 * - [useFrontCamera]: which lens to use.
 */
@Singleton
class CameraRequestState @Inject constructor() {
    val pendingCapture = MutableStateFlow(false)
    val useFrontCamera = MutableStateFlow(true)
}
