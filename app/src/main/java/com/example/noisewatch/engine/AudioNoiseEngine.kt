package com.example.noisewatch.engine

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioRecordingConfiguration
import android.media.MediaRecorder
import android.media.MicrophoneInfo
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.noisewatch.model.MeasurementData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * DEVELOPMENT CALIBRATION OFFSET
 *
 * Raw AudioRecord PCM values represent digital full scale (dBFS), not calibrated
 * sound pressure level (dB SPL). This isolated offset is used solely for demonstration
 * purposes in this phase to map digital amplitude to a realistic estimated positive dB range.
 *
 * It must be replaced in a future phase with a formal calibration factor and A-weighting filter.
 */
const val DEVELOPMENT_CALIBRATION_OFFSET = 96.0

private const val LOG_TAG_CONFIG = "NoiseWatchAudioConfig"
private const val LOG_TAG_AUDIO = "NoiseWatchAudio"

data class AudioMeasurementState(
    val currentDb: Double = 40.0,
    val laeq: Double = 40.0,
    val maxDb: Double = 40.0,
    val elapsedSeconds: Int = 0,
    val maxDurationSeconds: Int = 60,
    val history: List<Double> = emptyList(),
    val hasClipped: Boolean = false,
    val isFinished: Boolean = false,
    val isRecording: Boolean = false,
    val error: String? = null
)

class AudioNoiseEngine(private val context: Context? = null) {

    private var audioRecord: AudioRecord? = null
    @Volatile
    private var isCapturing = false
    private var selectedSampleRate = 48000
    private var audioSourceUsedName = "UNKNOWN"
    private var audioSourceUsedInt = -1
    private var reportedSourceStr = "UNKNOWN"

    private val _state = MutableStateFlow(AudioMeasurementState())
    val state: StateFlow<AudioMeasurementState> = _state.asStateFlow()

    private val historyList = mutableListOf<Double>()
    private var energySum = 0.0
    private var energyCount = 0
    private var peakMaxDb = 0.0
    private var peakMinDb = 120.0
    private var isClippingDetected = false

    // Diagnostic tracking variables
    private var rawPeakMaxSample = 0
    private var rawMinDbUnclamped = Double.MAX_VALUE
    private var totalNearClipSamplesAcc = 0L
    private var maxNearClipPctObserved = 0.0

    // Routing / Mic Change tracking
    private var initialRoutedDevName = "UNKNOWN"
    private var initialActiveMicDesc = "UNKNOWN"
    private var routeChangedDuringMeasurement = false
    private var micChangedDuringMeasurement = false
    private var recordingEverSilenced = false

    // Audio effects availability flags
    private var isAgcAvailable = false
    private var isNsAvailable = false
    private var isAecAvailable = false
    private var isAgcEnabled = false
    private var isNsEnabled = false
    private var isAecEnabled = false

    private var audioRecordingCallback: AudioManager.AudioRecordingCallback? = null

    @SuppressLint("MissingPermission")
    suspend fun startCapture() = withContext(Dispatchers.IO) {
        stopCaptureInternal() // Ensure any existing session is stopped

        val sampleRates = listOf(48000, 44100, 22050, 16000, 8000)
        var minBufSize = -1

        for (rate in sampleRates) {
            val size = AudioRecord.getMinBufferSize(
                rate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (size > 0) {
                selectedSampleRate = rate
                minBufSize = size
                break
            }
        }

        if (minBufSize <= 0) {
            _state.value = _state.value.copy(error = "AudioRecord hardware initialization failed.")
            return@withContext
        }

        val bufferSize = maxOf(minBufSize * 2, 2048)

        // CAMCORDER is intentionally preferred as the default AudioRecord source because
        // NoiseWatch measures general environmental noise rather than speech. Speech-focused
        // sources (like VOICE_RECOGNITION) apply system filters that attenuate non-speech environmental sound.
        // Fallback gracefully to MIC if CAMCORDER fails to initialize.
        val sourcesToTry = listOf(
            Pair(MediaRecorder.AudioSource.CAMCORDER, "CAMCORDER"),
            Pair(MediaRecorder.AudioSource.MIC, "MIC")
        )

        var record: AudioRecord? = null
        for ((sourceEnum, sourceName) in sourcesToTry) {
            try {
                val rec = AudioRecord(
                    sourceEnum,
                    selectedSampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
                if (rec.state == AudioRecord.STATE_INITIALIZED) {
                    record = rec
                    audioSourceUsedName = sourceName
                    audioSourceUsedInt = sourceEnum
                    break
                } else {
                    rec.release()
                }
            } catch (_: Exception) {
                // Try next fallback source
            }
        }

        if (record == null) {
            _state.value = _state.value.copy(error = "Could not initialize AudioRecord.")
            return@withContext
        }

        audioRecord = record
        historyList.clear()
        energySum = 0.0
        energyCount = 0
        peakMaxDb = 0.0
        peakMinDb = 120.0
        isClippingDetected = false

        rawPeakMaxSample = 0
        rawMinDbUnclamped = Double.MAX_VALUE
        totalNearClipSamplesAcc = 0L
        maxNearClipPctObserved = 0.0
        routeChangedDuringMeasurement = false
        micChangedDuringMeasurement = false
        recordingEverSilenced = false

        // Inspect Audio Effects Availability
        isAgcAvailable = try { AutomaticGainControl.isAvailable() } catch (_: Exception) { false }
        isNsAvailable = try { NoiseSuppressor.isAvailable() } catch (_: Exception) { false }
        isAecAvailable = try { AcousticEchoCanceler.isAvailable() } catch (_: Exception) { false }

        val sessionId = record.audioSessionId
        try {
            if (isAgcAvailable) {
                val agc = AutomaticGainControl.create(sessionId)
                isAgcEnabled = agc?.enabled ?: false
                agc?.release()
            }
            if (isNsAvailable) {
                val ns = NoiseSuppressor.create(sessionId)
                isNsEnabled = ns?.enabled ?: false
                ns?.release()
            }
            if (isAecAvailable) {
                val aec = AcousticEchoCanceler.create(sessionId)
                isAecEnabled = aec?.enabled ?: false
                aec?.release()
            }
        } catch (_: Exception) {
            // Ignore effect query issues
        }

        // Register AudioRecordingCallback if context available
        val audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager != null) {
            try {
                val callback = object : AudioManager.AudioRecordingCallback() {
                    override fun onRecordingConfigChanged(configs: List<AudioRecordingConfiguration>?) {
                        configs?.forEach { config ->
                            if (config.clientAudioSessionId == sessionId) {
                                val silenced = config.isClientSilenced
                                if (silenced) recordingEverSilenced = true
                                reportedSourceStr = getAudioSourceName(config.clientAudioSource)
                                Log.i(
                                    LOG_TAG_CONFIG,
                                    "NW_CONFIG recordingConfigChanged: source=$reportedSourceStr, session=${config.clientAudioSessionId}, isSilenced=$silenced"
                                )
                            }
                        }
                    }
                }
                audioRecordingCallback = callback
                audioManager.registerAudioRecordingCallback(callback, Handler(Looper.getMainLooper()))
            } catch (_: Exception) {
                // Ignore callback registration failure
            }
        }

        // Log Comprehensive Start-of-Measurement Config Block
        logStartConfigBlock(record, bufferSize, minBufSize, audioManager)

        try {
            record.startRecording()
            isCapturing = true
            _state.value = AudioMeasurementState(isRecording = true)

            val buffer = ShortArray(bufferSize)
            var lastUpdateMs = System.currentTimeMillis()
            var lastLogMs = System.currentTimeMillis()

            while (isCapturing) {
                val readSize = record.read(buffer, 0, buffer.size)
                if (readSize > 0) {
                    var sumSquare = 0.0
                    var maxSampleInWindow = 0
                    var nearClipCountInWindow = 0

                    val nearClipThreshold = 32012 // ~0.98 of 32768

                    for (i in 0 until readSize) {
                        val sample = buffer[i].toInt()
                        val absSample = if (sample < 0) -sample else sample
                        if (absSample > maxSampleInWindow) {
                            maxSampleInWindow = absSample
                        }
                        if (absSample >= nearClipThreshold) {
                            nearClipCountInWindow++
                        }
                        val normalized = sample / 32768.0
                        sumSquare += normalized * normalized
                    }

                    if (maxSampleInWindow > rawPeakMaxSample) {
                        rawPeakMaxSample = maxSampleInWindow
                    }
                    totalNearClipSamplesAcc += nearClipCountInWindow

                    val nearClipPctInWindow = (nearClipCountInWindow.toDouble() / readSize) * 100.0
                    if (nearClipPctInWindow > maxNearClipPctObserved) {
                        maxNearClipPctObserved = nearClipPctInWindow
                    }

                    // Clipping detection threshold check (PCM samples approaching 32767)
                    if (maxSampleInWindow >= 32700) {
                        isClippingDetected = true
                    }

                    val rms = sqrt(sumSquare / readSize)
                    val dbfs = if (rms > 0.00001) 20.0 * log10(rms) else -96.0
                    val unclampedSpl = dbfs + DEVELOPMENT_CALIBRATION_OFFSET
                    val estimatedDb = unclampedSpl.coerceIn(30.0, 120.0)

                    // Track raw unclamped minimum from first valid sample
                    if (rawMinDbUnclamped == Double.MAX_VALUE || unclampedSpl < rawMinDbUnclamped) {
                        rawMinDbUnclamped = unclampedSpl
                    }

                    val now = System.currentTimeMillis()

                    // UI State Update (150ms interval)
                    if ((now - lastUpdateMs) >= 150) {
                        lastUpdateMs = now

                        historyList.add(estimatedDb)
                        if (estimatedDb > peakMaxDb) {
                            peakMaxDb = estimatedDb
                        }
                        // Initialize or update peak minimum directly from valid sound calculation
                        if (peakMinDb == 120.0 || estimatedDb < peakMinDb) {
                            peakMinDb = estimatedDb
                        }

                        // Energy domain accumulation for equivalent level
                        val energy = 10.0.pow(estimatedDb / 10.0)
                        energySum += energy
                        energyCount++

                        val currentLaeq = 10.0 * log10(energySum / energyCount)

                        _state.value = _state.value.copy(
                            currentDb = estimatedDb,
                            laeq = currentLaeq,
                            maxDb = peakMaxDb,
                            history = historyList.toList(),
                            hasClipped = isClippingDetected,
                            isRecording = true
                        )
                    }

                    // Structured Diagnostic Logging at INFO level (1-second interval)
                    if ((now - lastLogMs) >= 1000) {
                        lastLogMs = now
                        val currentState = _state.value
                        val normPeak = maxSampleInWindow / 32768.0

                        val activeMicDesc = getActiveMicrophoneDesc(record)
                        val routedDevName = getRoutedDeviceDesc(record)

                        if (initialRoutedDevName != "UNKNOWN" && routedDevName != initialRoutedDevName) {
                            routeChangedDuringMeasurement = true
                        }
                        if (initialActiveMicDesc != "UNKNOWN" && activeMicDesc != initialActiveMicDesc) {
                            micChangedDuringMeasurement = true
                        }

                        val isSilenced = checkIsClientSilenced(audioManager, record.audioSessionId)
                        if (isSilenced) recordingEverSilenced = true

                        Log.i(
                            LOG_TAG_AUDIO,
                            String.format(
                                Locale.US,
                                "NW_AUDIO,t=%ds,rms=%.4f,peak=%d,normPeak=%.4f,dbfs=%.1f,unclampedDb=%.1f,displayDb=%.1f,laeq=%.1f,min=%.1f,rawMin=%.1f,max=%.1f,activeMic=%s,routedDev=%s,isSilenced=%b,nearClipInWindow=%d,nearClipPct=%.1f%%",
                                currentState.elapsedSeconds,
                                rms,
                                maxSampleInWindow,
                                normPeak,
                                dbfs,
                                unclampedSpl,
                                estimatedDb,
                                currentState.laeq,
                                peakMinDb,
                                if (rawMinDbUnclamped == Double.MAX_VALUE) 0.0 else rawMinDbUnclamped,
                                peakMaxDb,
                                activeMicDesc,
                                routedDevName,
                                isSilenced,
                                nearClipCountInWindow,
                                nearClipPctInWindow
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            _state.value = _state.value.copy(error = e.localizedMessage ?: "Recording error")
        } finally {
            logEndConfigSummary()
            stopCaptureInternal()
        }
    }

    private fun logStartConfigBlock(
        record: AudioRecord,
        bufferSize: Int,
        minBufSize: Int,
        audioManager: AudioManager?
    ) {
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL} (${Build.PRODUCT})"
        val unprocessedSupported = if (audioManager != null) {
            audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
        } else false

        val routedDev = getRoutedDeviceDesc(record)
        initialRoutedDevName = routedDev

        val activeMic = getActiveMicrophoneDesc(record)
        initialActiveMicDesc = activeMic

        val prefDev = getPreferredDeviceDesc(record)

        if (audioManager != null) {
            try {
                val configs = audioManager.activeRecordingConfigurations
                val myConfig = configs.firstOrNull { it.clientAudioSessionId == record.audioSessionId }
                if (myConfig != null) {
                    reportedSourceStr = getAudioSourceName(myConfig.clientAudioSource)
                }
            } catch (_: Exception) {}
        }

        Log.i(LOG_TAG_CONFIG, "========== NOISEWATCH AUDIO CONFIG ==========")
        Log.i(LOG_TAG_CONFIG, "PREFERRED_SOURCE=CAMCORDER")
        Log.i(LOG_TAG_CONFIG, "device=$deviceModel [API ${Build.VERSION.SDK_INT}]")
        Log.i(LOG_TAG_CONFIG, "manufacturer=${Build.MANUFACTURER}, model=${Build.MODEL}, product=${Build.PRODUCT}, device=${Build.DEVICE}")
        Log.i(LOG_TAG_CONFIG, "requestedSource=$audioSourceUsedName ($audioSourceUsedInt)")
        Log.i(LOG_TAG_CONFIG, "reportedSource=$reportedSourceStr")
        if (reportedSourceStr != "UNKNOWN" && !reportedSourceStr.startsWith(audioSourceUsedName)) {
            Log.i(LOG_TAG_CONFIG, "DISCREPANCY DETECTED: Requested $audioSourceUsedName ($audioSourceUsedInt), but Android reported $reportedSourceStr")
        }
        Log.i(LOG_TAG_CONFIG, "unprocessedSupported=$unprocessedSupported")
        Log.i(LOG_TAG_CONFIG, "sampleRate=$selectedSampleRate")
        Log.i(LOG_TAG_CONFIG, "encoding=PCM_16BIT (2)")
        Log.i(LOG_TAG_CONFIG, "channels=MONO (1)")
        Log.i(LOG_TAG_CONFIG, "bytesPerSample=2")
        Log.i(LOG_TAG_CONFIG, "minBufferSize=$minBufSize")
        Log.i(LOG_TAG_CONFIG, "bufferSizeBytes=$bufferSize")
        Log.i(LOG_TAG_CONFIG, "windowSamples=${bufferSize / 2}")
        Log.i(LOG_TAG_CONFIG, "sessionId=${record.audioSessionId}")
        Log.i(LOG_TAG_CONFIG, "audioRecordState=${getRecordStateName(record.state)}")
        Log.i(LOG_TAG_CONFIG, "recordingState=${getRecordingStateName(record.recordingState)}")
        Log.i(LOG_TAG_CONFIG, "")
        Log.i(LOG_TAG_CONFIG, "AGC available=$isAgcAvailable enabled=$isAgcEnabled")
        Log.i(LOG_TAG_CONFIG, "NoiseSuppressor available=$isNsAvailable enabled=$isNsEnabled")
        Log.i(LOG_TAG_CONFIG, "AEC available=$isAecAvailable enabled=$isAecEnabled")
        Log.i(LOG_TAG_CONFIG, "")
        Log.i(LOG_TAG_CONFIG, "routedDevice=$routedDev")
        Log.i(LOG_TAG_CONFIG, "preferredDevice=$prefDev")
        Log.i(LOG_TAG_CONFIG, "")

        // Available Microphones
        Log.i(LOG_TAG_CONFIG, "availableMicrophones:")
        if (audioManager != null) {
            try {
                val mics = audioManager.microphones
                if (mics.isEmpty()) {
                    Log.i(LOG_TAG_CONFIG, " - none reported")
                } else {
                    mics.forEach { mic ->
                        val locStr = getMicLocationName(mic.location)
                        val dirStr = getMicDirectionalityName(mic.directionality)
                        Log.i(
                            LOG_TAG_CONFIG,
                            " - Mic ID=${mic.id}, desc=${mic.description}, type=${getDeviceTypeName(mic.type)}, loc=$locStr, dir=$dirStr, sensitivity=${mic.sensitivity} dBFS/Pa, maxSpl=${mic.maxSpl}, minSpl=${mic.minSpl}"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.i(LOG_TAG_CONFIG, " - error querying microphones: ${e.message}")
            }
        } else {
            Log.i(LOG_TAG_CONFIG, " - audioManager null")
        }

        Log.i(LOG_TAG_CONFIG, "")
        Log.i(LOG_TAG_CONFIG, "activeMicrophones:")
        try {
            val activeMics = record.activeMicrophones
            if (activeMics.isNullOrEmpty()) {
                Log.i(LOG_TAG_CONFIG, " - none reported as active")
            } else {
                activeMics.forEach { mic ->
                    Log.i(
                        LOG_TAG_CONFIG,
                        " - Active Mic ID=${mic.id}, desc=${mic.description}, type=${getDeviceTypeName(mic.type)}, loc=${getMicLocationName(mic.location)}"
                    )
                }
            }
        } catch (e: Exception) {
            Log.i(LOG_TAG_CONFIG, " - error querying active microphones: ${e.message}")
        }

        Log.i(LOG_TAG_CONFIG, "")
        Log.i(LOG_TAG_CONFIG, "activeRecordingConfiguration:")
        if (audioManager != null) {
            try {
                val configs = audioManager.activeRecordingConfigurations
                val myConfig = configs.firstOrNull { it.clientAudioSessionId == record.audioSessionId }
                if (myConfig != null) {
                    val clientSrc = getAudioSourceName(myConfig.clientAudioSource)
                    val isSilenced = myConfig.isClientSilenced
                    if (isSilenced) recordingEverSilenced = true

                    val clientFmt = myConfig.format
                    val clientFmtStr = "${clientFmt.sampleRate}Hz/channels=${clientFmt.channelCount}/enc=${clientFmt.encoding}"

                    Log.i(
                        LOG_TAG_CONFIG,
                        " clientSource=$clientSrc, clientSessionId=${myConfig.clientAudioSessionId}, clientFormat=$clientFmtStr, isSilenced=$isSilenced"
                    )
                } else {
                    Log.i(LOG_TAG_CONFIG, " - session not found in activeRecordingConfigurations")
                }
            } catch (e: Exception) {
                Log.i(LOG_TAG_CONFIG, " - error querying active recording configs: ${e.message}")
            }
        } else {
            Log.i(LOG_TAG_CONFIG, " - audioManager null")
        }

        Log.i(LOG_TAG_CONFIG, "=============================================")
    }

    private fun logEndConfigSummary() {
        val currentState = _state.value
        val normPeakMax = rawPeakMaxSample / 32768.0
        val finalRawMin = if (rawMinDbUnclamped == Double.MAX_VALUE) 0.0 else rawMinDbUnclamped

        Log.i(LOG_TAG_CONFIG, "==================================================")
        Log.i(LOG_TAG_CONFIG, "NOISEWATCH AUDIO CONFIG END SUMMARY")
        Log.i(LOG_TAG_CONFIG, "PREFERRED_SOURCE=$audioSourceUsedName")
        Log.i(LOG_TAG_CONFIG, String.format(Locale.US, "duration=%ds", currentState.elapsedSeconds))
        Log.i(LOG_TAG_CONFIG, "requestedSource=$audioSourceUsedName ($audioSourceUsedInt)")
        Log.i(LOG_TAG_CONFIG, "reportedSource=$reportedSourceStr")
        Log.i(LOG_TAG_CONFIG, "routedDevice=$initialRoutedDevName")
        Log.i(LOG_TAG_CONFIG, "activeMicrophone=$initialActiveMicDesc")
        Log.i(LOG_TAG_CONFIG, "routeChangedDuringMeasurement=$routeChangedDuringMeasurement")
        Log.i(LOG_TAG_CONFIG, "activeMicChangedDuringMeasurement=$micChangedDuringMeasurement")
        Log.i(LOG_TAG_CONFIG, "recordingEverSilenced=$recordingEverSilenced")
        Log.i(LOG_TAG_CONFIG, "AGC_available=$isAgcAvailable, AGC_enabled=$isAgcEnabled")
        Log.i(LOG_TAG_CONFIG, "NS_available=$isNsAvailable, NS_enabled=$isNsEnabled")
        Log.i(LOG_TAG_CONFIG, "AEC_available=$isAecAvailable, AEC_enabled=$isAecEnabled")
        Log.i(LOG_TAG_CONFIG, String.format(Locale.US, "finalLaeq=%.1f dB(A)", currentState.laeq))
        Log.i(LOG_TAG_CONFIG, String.format(Locale.US, "displayedMin=%.1f dB(A)", peakMinDb))
        Log.i(LOG_TAG_CONFIG, String.format(Locale.US, "rawUnclampedMin=%.1f dB(A)", finalRawMin))
        Log.i(LOG_TAG_CONFIG, String.format(Locale.US, "finalMax=%.1f dB(A)", peakMaxDb))
        Log.i(LOG_TAG_CONFIG, String.format(Locale.US, "maxRawPeakSample=%d / 32767", rawPeakMaxSample))
        Log.i(LOG_TAG_CONFIG, String.format(Locale.US, "maxNormPeak=%.4f", normPeakMax))
        Log.i(LOG_TAG_CONFIG, String.format(Locale.US, "totalNearClipSamples=%d", totalNearClipSamplesAcc))
        Log.i(LOG_TAG_CONFIG, String.format(Locale.US, "maxNearClipPctInWindow=%.2f%%", maxNearClipPctObserved))
        Log.i(LOG_TAG_CONFIG, "clippingWarningTriggered=$isClippingDetected")
        Log.i(LOG_TAG_CONFIG, "==================================================")
    }

    private fun checkIsClientSilenced(audioManager: AudioManager?, sessionId: Int): Boolean {
        if (audioManager == null) return false
        return try {
            val configs = audioManager.activeRecordingConfigurations
            val myConfig = configs.firstOrNull { it.clientAudioSessionId == sessionId }
            myConfig?.isClientSilenced ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun getRoutedDeviceDesc(record: AudioRecord): String {
        val device = record.routedDevice
        if (device != null) {
            return "${getDeviceTypeName(device.type)} (${device.productName})"
        }
        return "UNKNOWN"
    }

    private fun getPreferredDeviceDesc(record: AudioRecord): String {
        val device = record.preferredDevice
        if (device != null) {
            return "${getDeviceTypeName(device.type)} (${device.productName})"
        }
        return "none"
    }

    private fun getActiveMicrophoneDesc(record: AudioRecord): String {
        try {
            val activeMics = record.activeMicrophones
            if (!activeMics.isNullOrEmpty()) {
                val mic = activeMics[0]
                return "${mic.description} (ID=${mic.id})"
            }
        } catch (_: Exception) {}
        return "none_reported"
    }

    private fun getAudioSourceName(source: Int): String {
        return when (source) {
            MediaRecorder.AudioSource.DEFAULT -> "DEFAULT (0)"
            MediaRecorder.AudioSource.MIC -> "MIC (1)"
            MediaRecorder.AudioSource.VOICE_UPLINK -> "VOICE_UPLINK (2)"
            MediaRecorder.AudioSource.VOICE_DOWNLINK -> "VOICE_DOWNLINK (3)"
            MediaRecorder.AudioSource.VOICE_CALL -> "VOICE_CALL (4)"
            MediaRecorder.AudioSource.CAMCORDER -> "CAMCORDER (5)"
            MediaRecorder.AudioSource.VOICE_RECOGNITION -> "VOICE_RECOGNITION (6)"
            MediaRecorder.AudioSource.VOICE_COMMUNICATION -> "VOICE_COMMUNICATION (7)"
            MediaRecorder.AudioSource.UNPROCESSED -> "UNPROCESSED (9)"
            else -> "SOURCE_$source"
        }
    }

    private fun getRecordStateName(state: Int): String {
        return when (state) {
            AudioRecord.STATE_INITIALIZED -> "INITIALIZED (1)"
            AudioRecord.STATE_UNINITIALIZED -> "UNINITIALIZED (0)"
            else -> "STATE_$state"
        }
    }

    private fun getRecordingStateName(state: Int): String {
        return when (state) {
            AudioRecord.RECORDSTATE_RECORDING -> "RECORDING (3)"
            AudioRecord.RECORDSTATE_STOPPED -> "STOPPED (1)"
            else -> "RECORDSTATE_$state"
        }
    }

    private fun getMicLocationName(location: Int): String {
        return when (location) {
            MicrophoneInfo.LOCATION_MAINBODY -> "MAINBODY"
            MicrophoneInfo.LOCATION_MAINBODY_MOVABLE -> "MAINBODY_MOVABLE"
            MicrophoneInfo.LOCATION_PERIPHERAL -> "PERIPHERAL"
            else -> "UNKNOWN"
        }
    }

    private fun getMicDirectionalityName(directionality: Int): String {
        return when (directionality) {
            MicrophoneInfo.DIRECTIONALITY_OMNI -> "OMNI"
            MicrophoneInfo.DIRECTIONALITY_CARDIOID -> "CARDIOID"
            MicrophoneInfo.DIRECTIONALITY_BI_DIRECTIONAL -> "BI_DIRECTIONAL"
            else -> "UNKNOWN"
        }
    }

    private fun getDeviceTypeName(type: Int): String {
        return when (type) {
            AudioDeviceInfo.TYPE_BUILTIN_MIC -> "BUILTIN_MIC"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "WIRED_HEADSET"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "BLUETOOTH_SCO"
            AudioDeviceInfo.TYPE_USB_DEVICE -> "USB_DEVICE"
            AudioDeviceInfo.TYPE_USB_HEADSET -> "USB_HEADSET"
            AudioDeviceInfo.TYPE_TELEPHONY -> "TELEPHONY"
            else -> "TYPE_$type"
        }
    }

    fun resetSession() {
        stopCaptureInternal()
        historyList.clear()
        energySum = 0.0
        energyCount = 0
        peakMaxDb = 0.0
        peakMinDb = 120.0
        rawPeakMaxSample = 0
        rawMinDbUnclamped = Double.MAX_VALUE
        totalNearClipSamplesAcc = 0L
        maxNearClipPctObserved = 0.0
        isClippingDetected = false
        _state.value = AudioMeasurementState()
    }

    fun updateTimer(elapsedSeconds: Int, maxDurationSeconds: Int = 60) {
        val isFinished = elapsedSeconds >= maxDurationSeconds
        _state.value = _state.value.copy(
            elapsedSeconds = elapsedSeconds,
            maxDurationSeconds = maxDurationSeconds,
            isFinished = isFinished
        )
        if (isFinished) {
            stopCaptureInternal()
        }
    }

    fun stopCapture() {
        stopCaptureInternal()
    }

    private fun stopCaptureInternal() {
        isCapturing = false
        if (audioRecordingCallback != null && context != null) {
            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                audioRecordingCallback?.let { audioManager?.unregisterAudioRecordingCallback(it) }
            } catch (_: Exception) {}
            audioRecordingCallback = null
        }
        try {
            audioRecord?.let {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
                it.release()
            }
        } catch (_: Exception) {
            // Ignore release exceptions
        } finally {
            audioRecord = null
            _state.value = _state.value.copy(isRecording = false)
        }
    }

    fun getFinalMeasurementData(): MeasurementData {
        val currentState = _state.value
        val finalMin = if (peakMinDb < 120.0) peakMinDb else currentState.currentDb
        return MeasurementData(
            laeq = currentState.laeq,
            maxDb = currentState.maxDb,
            minDb = finalMin,
            durationSeconds = currentState.elapsedSeconds,
            history = currentState.history,
            hasClipped = currentState.hasClipped
        )
    }

    fun getSampleRate(): Int = selectedSampleRate
}
