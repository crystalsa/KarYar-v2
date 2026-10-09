package com.karyar.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FontCmapTest {

    private fun extractSupportedCodePoints(fontFile: File): Set<Int> {
        assertTrue("Font file must exist: ${fontFile.absolutePath}", fontFile.exists())
        val bytes = fontFile.readBytes()
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)

        val numTables = buffer.getShort(4).toInt() and 0xFFFF
        var cmapOffset = 0
        for (i in 0 until numTables) {
            val recordPos = 12 + i * 16
            val tag = String(bytes, recordPos, 4, Charsets.ISO_8859_1)
            if (tag == "cmap") {
                cmapOffset = buffer.getInt(recordPos + 8)
                break
            }
        }
        assertTrue("cmap table must exist in TTF", cmapOffset > 0)

        val numSubtables = buffer.getShort(cmapOffset + 2).toInt() and 0xFFFF
        val supportedPoints = mutableSetOf<Int>()

        for (i in 0 until numSubtables) {
            val subRecordPos = cmapOffset + 4 + i * 8
            val offset = buffer.getInt(subRecordPos + 4)
            val subtablePos = cmapOffset + offset
            val format = buffer.getShort(subtablePos).toInt() and 0xFFFF

            if (format == 4) {
                val segCountX2 = buffer.getShort(subtablePos + 6).toInt() and 0xFFFF
                val segCount = segCountX2 / 2
                val endCodeOffset = subtablePos + 14
                val startCodeOffset = endCodeOffset + segCount * 2 + 2
                val idDeltaOffset = startCodeOffset + segCount * 2
                val idRangeOffsetOffset = idDeltaOffset + segCount * 2

                for (seg in 0 until segCount) {
                    val endCode = buffer.getShort(endCodeOffset + seg * 2).toInt() and 0xFFFF
                    val startCode = buffer.getShort(startCodeOffset + seg * 2).toInt() and 0xFFFF
                    if (startCode == 0xFFFF) break
                    val idDelta = buffer.getShort(idDeltaOffset + seg * 2).toInt()
                    val idRangeOffsetPos = idRangeOffsetOffset + seg * 2
                    val idRangeOffset = buffer.getShort(idRangeOffsetPos).toInt() and 0xFFFF

                    for (c in startCode..endCode) {
                        val glyphId = if (idRangeOffset == 0) {
                            (c + idDelta) and 0xFFFF
                        } else {
                            val glyphPos = idRangeOffsetPos + idRangeOffset + (c - startCode) * 2
                            val gid = buffer.getShort(glyphPos).toInt() and 0xFFFF
                            if (gid != 0) (gid + idDelta) and 0xFFFF else 0
                        }
                        if (glyphId != 0) {
                            supportedPoints.add(c)
                        }
                    }
                }
            }
        }
        return supportedPoints
    }

    @Test
    fun testVazirmatnContainsAllPersianCharactersAndDigitsAndZwnj() {
        val regularFile = listOf(
            File("src/main/res/font/vazirmatn.ttf"),
            File("app/src/main/res/font/vazirmatn.ttf")
        ).firstOrNull { it.exists() } ?: File("src/main/res/font/vazirmatn.ttf")
        val regularCmap = extractSupportedCodePoints(regularFile)

        // Essential Persian Letters: ا-ی including پ, چ, ژ, گ, ک, ی
        val persianAlphabet = "ابپتثجچحخدذرزژسشصضطظعغفقکگلمنوهی"
        for (ch in persianAlphabet) {
            assertTrue(
                "Persian letter '$ch' (U+${Integer.toHexString(ch.code).uppercase()}) must be in Vazirmatn cmap",
                regularCmap.contains(ch.code)
            )
        }

        // Persian Digits: ۰-۹
        val persianDigits = "۰۱۲۳۴۵۶۷۸۹"
        for (digit in persianDigits) {
            assertTrue(
                "Persian digit '$digit' must be in Vazirmatn cmap",
                regularCmap.contains(digit.code)
            )
        }

        // Zero-Width Non-Joiner (نیم‌فاصله)
        val zwnj = '\u200C'
        assertTrue(
            "Zero-Width Non-Joiner (U+200C) must be in Vazirmatn cmap",
            regularCmap.contains(zwnj.code)
        )

        // Check total glyphs is comprehensive (not a 229 Latin-only subset)
        assertTrue(
            "Vazirmatn must contain comprehensive charset (>500 glyphs)",
            regularCmap.size > 500
        )
    }

    @Test
    fun testVazirmatnBoldContainsAllPersianCharacters() {
        val boldFile = listOf(
            File("src/main/res/font/vazirmatn_bold.ttf"),
            File("app/src/main/res/font/vazirmatn_bold.ttf")
        ).firstOrNull { it.exists() } ?: File("src/main/res/font/vazirmatn_bold.ttf")
        val boldCmap = extractSupportedCodePoints(boldFile)

        val persianAlphabet = "ابپتثجچحخدذرزژسشصضطظعغفقکگلمنوهی۰۱۲۳۴۵۶۷۸۹\u200C"
        for (ch in persianAlphabet) {
            assertTrue(
                "Character '$ch' must be in Vazirmatn Bold cmap",
                boldCmap.contains(ch.code)
            )
        }
    }
}
