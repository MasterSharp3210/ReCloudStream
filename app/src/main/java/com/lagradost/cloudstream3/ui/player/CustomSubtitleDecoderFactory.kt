package com.lagradost.cloudstream3.ui.player

import android.content.Context
import android.text.Layout
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.text.Cue
import androidx.media3.common.util.Consumer
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.text.SubtitleDecoderFactory
import androidx.media3.extractor.text.CuesWithTiming
import androidx.media3.extractor.text.SimpleSubtitleDecoder
import androidx.media3.extractor.text.Subtitle
import androidx.media3.extractor.text.SubtitleDecoder
import androidx.media3.extractor.text.SubtitleParser
import androidx.media3.extractor.text.dvb.DvbParser
import androidx.media3.extractor.text.pgs.PgsParser
import androidx.media3.extractor.text.ssa.SsaParser
import androidx.media3.extractor.text.ttml.TtmlParser
import androidx.media3.extractor.text.tx3g.Tx3gParser
import androidx.media3.extractor.text.webvtt.Mp4WebvttParser
import androidx.media3.extractor.text.webvtt.WebvttParser
import androidx.preference.PreferenceManager
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.ui.subtitles.SaveCaptionStyle
import com.lagradost.cloudstream3.ui.subtitles.SubtitlesFragment
import org.mozilla.universalchardet.UniversalDetector
import java.lang.ref.WeakReference
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * @param fallbackFormat used to create a decoder based on mimetype if the subtitle string is not
 * enough to identify the subtitle format.
 */
@OptIn(UnstableApi::class)
class CustomDecoder(private val fallbackFormat: Format?) : SubtitleParser {
    companion object {
        fun updateForcedEncoding(context: Context) {
            val settingsManager = PreferenceManager.getDefaultSharedPreferences(context)
            val value = settingsManager.getString(
                context.getString(R.string.subtitles_encoding_key),
                null
            )
            overrideEncoding = if (value.isNullOrBlank()) {
                null
            } else {
                value
            }
        }

        private const val DEFAULT_MARGIN: Float = 0.05f
        const val SSA_ALIGNMENT_BOTTOM_LEFT = 1
        const val SSA_ALIGNMENT_BOTTOM_CENTER = 2
        const val SSA_ALIGNMENT_BOTTOM_RIGHT = 3
        const val SSA_ALIGNMENT_MIDDLE_LEFT = 4
        const val SSA_ALIGNMENT_MIDDLE_CENTER = 5
        const val SSA_ALIGNMENT_MIDDLE_RIGHT = 6
        const val SSA_ALIGNMENT_TOP_LEFT = 7
        const val SSA_ALIGNMENT_TOP_CENTER = 8
        const val SSA_ALIGNMENT_TOP_RIGHT = 9

        /** Subtitle offset in milliseconds */
        var subtitleOffset: Long = 0
        private const val UTF_8 = "UTF-8"
        private const val WINDOWS_1252 = "windows-1252"
        private const val TAG = "CustomDecoder"
        private var overrideEncoding: String? = null
        val style: SaveCaptionStyle get() = SubtitlesFragment.getCurrentSavedStyle()
        private val locationRegex = Regex("""\{\\an(\d+)\}""", RegexOption.IGNORE_CASE)
        val bloatRegex =
            listOf(
                Regex(
                    """Support\s+us\s+and\s+become\s+VIP\s+member\s+to\s+remove\s+all\s+ads\s+from\s+(www\.|)OpenSubtitles(\.org|)""",
                    RegexOption.IGNORE_CASE
                ),
                Regex(
                    """Please\s+rate\s+this\s+subtitle\s+at\s+.*\s+Help\s+other\s+users\s+to\s+choose\s+the\s+best\s+subtitles""",
                    RegexOption.IGNORE_CASE
                ),
                Regex(
                    """Contact\s(www\.|)OpenSubtitles(\.org|)\s+today""",
                    RegexOption.IGNORE_CASE
                ),
                Regex(
                    """Advertise\s+your\s+product\s+or\s+brand\s+here""",
                    RegexOption.IGNORE_CASE
                ),
            )

        //https://emptycharacter.com/
        //https://www.fileformat.info/info/unicode/char/200b/index.htm
        fun trimStr(string: String): String {
            return string.trimStart().trim('\uFEFF', '\u200B').replace(
                Regex("[\u00A0\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200A\u205F]"),
                " "
            )
        }

        private fun computeDefaultLineOrPosition(@Cue.AnchorType anchor: Int) = when (anchor) {
            Cue.ANCHOR_TYPE_START -> DEFAULT_MARGIN
            Cue.ANCHOR_TYPE_MIDDLE -> 0.5f
            Cue.ANCHOR_TYPE_END -> 1.0f - DEFAULT_MARGIN
            Cue.TYPE_UNSET -> Cue.DIMEN_UNSET
            else -> Cue.DIMEN_UNSET
        }

        private fun isValidUtf8(bytes: ByteArray): Boolean {
            var i = 0
            while (i < bytes.size) {
                val b = bytes[i].toInt() and 0xFF
                if (b < 0x80) {
                    i++
                } else if (b in 0xC2..0xDF) {
                    if (i + 1 >= bytes.size) return false
                    val b1 = bytes[i + 1].toInt() and 0xFF
                    if (b1 !in 0x80..0xBF) return false
                    i += 2
                } else if (b in 0xE0..0xEF) {
                    if (i + 2 >= bytes.size) return false
                    val b1 = bytes[i + 1].toInt() and 0xFF
                    val b2 = bytes[i + 2].toInt() and 0xFF
                    if (b1 !in 0x80..0xBF || b2 !in 0x80..0xBF) return false
                    i += 3
                } else if (b in 0xF0..0xF4) {
                    if (i + 3 >= bytes.size) return false
                    val b1 = bytes[i + 1].toInt() and 0xFF
                    val b2 = bytes[i + 2].toInt() and 0xFF
                    val b3 = bytes[i + 3].toInt() and 0xFF
                    if (b1 !in 0x80..0xBF || b2 !in 0x80..0xBF || b3 !in 0x80..0xBF) return false
                    i += 4
                } else {
                    return false
                }
            }
            return true
        }

        /**
         * Fixes alignment for cues with {\anX},
         * this is common for .vtt that should be parsed as .srt
         */
        fun Cue.Builder.fixSubtitleAlignment(): Cue.Builder {
            var trimmed = text?.trim() ?: return this
            locationRegex.find(trimmed)?.groupValues?.get(1)?.toIntOrNull()?.let { alignment ->
                this.setSubtitleAlignment(alignment)
            }

            trimmed = trimmed.replace(locationRegex, "")
            setText(trimmed)
            return this
        }

        fun Cue.Builder.setSubtitleAlignment(alignment: Int?): Cue.Builder {
            if (alignment == null) return this
            when (alignment) {
                SSA_ALIGNMENT_BOTTOM_LEFT, SSA_ALIGNMENT_BOTTOM_CENTER, SSA_ALIGNMENT_BOTTOM_RIGHT -> Cue.ANCHOR_TYPE_END
                SSA_ALIGNMENT_MIDDLE_LEFT, SSA_ALIGNMENT_MIDDLE_CENTER, SSA_ALIGNMENT_MIDDLE_RIGHT -> Cue.ANCHOR_TYPE_MIDDLE
                SSA_ALIGNMENT_TOP_LEFT, SSA_ALIGNMENT_TOP_CENTER, SSA_ALIGNMENT_TOP_RIGHT -> Cue.ANCHOR_TYPE_START
                else -> null
            }?.let { anchor ->
                setLineAnchor(anchor)
                setLine(
                    computeDefaultLineOrPosition(anchor), Cue.LINE_TYPE_FRACTION
                )
            }
            when (alignment) {
                SSA_ALIGNMENT_BOTTOM_LEFT, SSA_ALIGNMENT_MIDDLE_LEFT, SSA_ALIGNMENT_TOP_LEFT -> Cue.ANCHOR_TYPE_START
                SSA_ALIGNMENT_BOTTOM_CENTER, SSA_ALIGNMENT_MIDDLE_CENTER, SSA_ALIGNMENT_TOP_CENTER -> Cue.ANCHOR_TYPE_MIDDLE
                SSA_ALIGNMENT_BOTTOM_RIGHT, SSA_ALIGNMENT_MIDDLE_RIGHT, SSA_ALIGNMENT_TOP_RIGHT -> Cue.ANCHOR_TYPE_END
                else -> null
            }?.let { anchor ->
                setPositionAnchor(anchor)
                setPosition(computeDefaultLineOrPosition(anchor))
            }

            when (alignment) {
                SSA_ALIGNMENT_BOTTOM_LEFT, SSA_ALIGNMENT_MIDDLE_LEFT, SSA_ALIGNMENT_TOP_LEFT -> Layout.Alignment.ALIGN_NORMAL
                SSA_ALIGNMENT_BOTTOM_CENTER, SSA_ALIGNMENT_MIDDLE_CENTER, SSA_ALIGNMENT_TOP_CENTER -> Layout.Alignment.ALIGN_CENTER
                SSA_ALIGNMENT_BOTTOM_RIGHT, SSA_ALIGNMENT_MIDDLE_RIGHT, SSA_ALIGNMENT_TOP_RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
                else -> null
            }?.let { anchor ->
                setTextAlignment(anchor)
            }
            return this
        }
    }

    private var realDecoder: SubtitleParser? = null

    private fun getStr(byteArray: ByteArray): Pair<String, Charset> {
        val encoding = try {
            overrideEncoding ?: run {
                val detector = UniversalDetector()
                detector.handleData(byteArray, 0, byteArray.size)
                detector.dataEnd()
                val detected = detector.detectedCharset
                if (detected == null || detected.equals("ISO-8859-1", ignoreCase = true) || detected.equals("WINDOWS-1252", ignoreCase = true)) {
                    if (isValidUtf8(byteArray)) UTF_8 else WINDOWS_1252
                } else {
                    detected
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to detect encoding throwing error")
            logError(e)
            if (isValidUtf8(byteArray)) UTF_8 else WINDOWS_1252
        }

        return try {
            val set = charset(encoding)
            Pair(String(byteArray, set), set)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse using encoding $encoding")
            logError(e)
            Pair(byteArray.decodeToString(), Charset.forName(UTF_8))
        }
    }

    private fun getSubtitleParser(data: String): SubtitleParser? {
        val controlCharsRegex = Regex("""[\p{Cntrl}\p{Cf}]""")
        val trimmedText =
            data.trimStart { it.isWhitespace() || controlCharsRegex.matches(it.toString()) }

        val headerSample = trimmedText.take(500)

        val subtitleParser = when {
            headerSample.contains("WEBVTT", ignoreCase = true) -> WebvttParser()
            headerSample.startsWith("<?xml", ignoreCase = true) || headerSample.startsWith("<tt", ignoreCase = true) -> TtmlParser()
            (headerSample.startsWith("[Script Info]", ignoreCase = true) ||
                    headerSample.startsWith("Title:", ignoreCase = true) ||
                    headerSample.contains("[Events]", ignoreCase = true)) -> SsaParser(fallbackFormat?.initializationData)

            headerSample.contains("-->") || headerSample.firstOrNull()?.isDigit() == true -> CustomSubripParser()
            fallbackFormat != null -> {
                when (fallbackFormat.sampleMimeType) {
                    MimeTypes.TEXT_VTT -> WebvttParser()
                    MimeTypes.TEXT_SSA -> SsaParser(fallbackFormat.initializationData)
                    MimeTypes.APPLICATION_MP4VTT -> Mp4WebvttParser()
                    MimeTypes.APPLICATION_TTML -> TtmlParser()
                    MimeTypes.APPLICATION_SUBRIP -> CustomSubripParser()
                    MimeTypes.APPLICATION_TX3G -> Tx3gParser(fallbackFormat.initializationData)
                    MimeTypes.APPLICATION_DVBSUBS -> DvbParser(fallbackFormat.initializationData)
                    MimeTypes.APPLICATION_PGS -> PgsParser()
                    else -> CustomSubripParser()
                }
            }

            else -> CustomSubripParser()
        }
        return subtitleParser
    }

    val currentSubtitleCues = mutableListOf<SubtitleCue>()

    override fun parse(
        data: ByteArray,
        offset: Int,
        length: Int,
        outputOptions: SubtitleParser.OutputOptions,
        output: Consumer<CuesWithTiming>
    ) {
        val currentStyle = style
        val customOutput = Consumer<CuesWithTiming> { cue ->
            val newCue = CuesWithTiming(cue.cues, cue.startTimeUs, cue.durationUs)

            currentSubtitleCues.add(
                SubtitleCue(
                    newCue.startTimeUs / 1000,
                    newCue.durationUs / 1000,
                    newCue.cues.map { it.text.toString() })
            )

            val updatedCues =
                CuesWithTiming(
                    newCue.cues,
                    newCue.startTimeUs - subtitleOffset.times(1000),
                    newCue.durationUs
                )

            output.accept(updatedCues)
        }
        try {
            val inputString = getStr(data).first
            if (inputString.isNotBlank()) {
                var str: String = trimStr(inputString)
                realDecoder = realDecoder ?: getSubtitleParser(inputString)
                realDecoder?.let { decoder ->
                    if (decoder !is SsaParser) {
                        if (currentStyle.removeBloat)
                            bloatRegex.forEach { rgx ->
                                str = str.replace(rgx, "\n")
                            }
                        if (currentStyle.upperCase) {
                            str = str.uppercase()
                        }
                    }
                }
                val array = str.toByteArray(StandardCharsets.UTF_8)
                realDecoder?.parse(
                    array,
                    0,
                    array.size,
                    outputOptions,
                    customOutput
                )
            }
        } catch (e: Exception) {
            logError(e)
        }
    }

    override fun getCueReplacementBehavior(): Int {
        return realDecoder?.cueReplacementBehavior ?: Format.CUE_REPLACEMENT_BEHAVIOR_MERGE
    }

    override fun reset() {
        currentSubtitleCues.clear()
        super.reset()
    }
}

/** See https://github.com/google/ExoPlayer/blob/release-v2/library/core/src/main/java/com/google/android/exoplayer2/text/SubtitleDecoderFactory.java */
@OptIn(UnstableApi::class)
class CustomSubtitleDecoderFactory : SubtitleDecoderFactory {

    override fun supportsFormat(format: Format): Boolean {
        return listOf(
            MimeTypes.TEXT_VTT,
            MimeTypes.TEXT_SSA,
            MimeTypes.APPLICATION_TTML,
            MimeTypes.APPLICATION_MP4VTT,
            MimeTypes.APPLICATION_SUBRIP,
            MimeTypes.APPLICATION_TX3G,
            MimeTypes.APPLICATION_DVBSUBS,
            MimeTypes.APPLICATION_PGS,
        ).contains(format.sampleMimeType)
    }

    private var activeDecoder: CustomDecoder? = null
    private var latestDecoder: WeakReference<CustomDecoder>? = null

    fun getSubtitleCues(): List<SubtitleCue>? {
        return activeDecoder?.currentSubtitleCues ?: latestDecoder?.get()?.currentSubtitleCues
    }

    override fun createDecoder(format: Format): SubtitleDecoder {
        val parser = CustomDecoder(format)
        activeDecoder = parser
        latestDecoder = WeakReference(parser)

        return DelegatingSubtitleDecoder(
            parser::class.simpleName + "Decoder", parser
        )
    }
}

/** We need to convert the newer SubtitleParser to an older SubtitleDecoder */
@OptIn(UnstableApi::class)
class DelegatingSubtitleDecoder(name: String, private val parser: SubtitleParser) :
    SimpleSubtitleDecoder(name) {

    override fun decode(data: ByteArray, length: Int, reset: Boolean): Subtitle {
        if (reset) {
            parser.reset()
        }
        return parser.parseToLegacySubtitle(data, 0, length);
    }
}
