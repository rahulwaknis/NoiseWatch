package com.example.noisewatch.engine

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.example.noisewatch.model.MeasurementData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
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

class AudioNoiseEngine {

    private var audioRecord: AudioRecord? = null
    @Volatile
    private var isCapturing = false
    private var selectedSampleRate = 48000

    private val _state = MutableStateFlow(AudioMeasurementState())
    val state: StateFlow<AudioMeasurementState> = _state.asStateFlow()

    private val historyList = mutableListOf<Double>()
    private var energySum = 0.0
    private var energyCount = 0
    private var peakMaxDb = 0.0
    private var peakMinDb = 120.0
    private var isClippingDetected = false

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

        val sources = listOf(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC
        )

        var record: AudioRecord? = null
        for (source in sources) {
            try {
                val rec = AudioRecord(
                    source,
                    selectedSampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
                if (rec.state == AudioRecord.STATE_INITIALIZED) {
                    record = rec
                    break
                } else {
                    rec.release()
                }
            } catch (_: Exception) {
                // Try next source
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
        isClippingDetected = false

        try {
            record.startRecording()
            isCapturing = true
            _state.value = AudioMeasurementState(isRecording = true)

            val buffer = ShortArray(bufferSize)
            var lastUpdateMs = System.currentTimeMillis()

            while (isCapturing) {
                val readSize = record.read(buffer, 0, buffer.size)
                if (readSize > 0) {
                    var sumSquare = 0.0
                    var maxSample = 0

                    for (i in 0 until readSize) {
                        val sample = buffer[i].toInt()
                        val absSample = if (sample < 0) -sample else sample
                        if (absSample > maxSample) {
                            maxSample = absSample
                        }
                        val normalized = sample / 32768.0
                        sumSquare += normalized * normalized
                    }

                    // Clipping detection threshold check (PCM samples approaching 32767)
                    if (maxSample >= 32700) {
                        isClippingDetected = true
                    }

                    val rms = sqrt(sumSquare / readSize)
                    val dbfs = if (rms > 0.00001) 20.0 * log10(rms) else -96.0
                    val estimatedDb = (dbfs + DEVELOPMENT_CALIBRATION_OFFSET).coerceIn(30.0, 120.0)

                    val now = System.currentTimeMillis()
                    if ((now - lastUpdateMs) >= 150) {
                        lastUpdateMs = now

                        historyList.add(estimatedDb)
                        if (estimatedDb > peakMaxDb) {
                            peakMaxDb = estimatedDb
                        }
                        if (estimatedDb < peakMinDb && estimatedDb > 0.0) {
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
                }
            }
        } catch (e: Exception) {
            _state.value = _state.value.copy(error = e.localizedMessage ?: "Recording error")
        } finally {
            stopCaptureInternal()
        }
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
