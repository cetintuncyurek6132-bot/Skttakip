package com.example.util

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.util.zip.ZipInputStream

object XlsxParser {

    /**
     * Parses the binary byte array of an .xlsx (Excel) file into CSV-formatted lines
     * separated by semicolons (;).
     */
    fun parseXlsxToCsvLines(bytes: ByteArray): List<String> {
        val entries = mutableMapOf<String, ByteArray>()
        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val name = entry.name.lowercase()
                        if (name == "xl/sharedstrings.xml" || name.startsWith("xl/worksheets/sheet")) {
                            entries[name] = zis.readBytes()
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }

        if (entries.isEmpty()) return emptyList()

        // Step 1: Parse sharedStrings.xml if present
        val sharedStrings = mutableListOf<String>()
        entries.entries.firstOrNull { it.key == "xl/sharedstrings.xml" }?.value?.let { sharedBytes ->
            try {
                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = true
                val parser = factory.newPullParser()
                parser.setInput(ByteArrayInputStream(sharedBytes), "UTF-8")

                var eventType = parser.eventType
                var currentText = StringBuilder()
                var insideSi = false
                var insideT = false

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    val tagName = parser.name
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            if (tagName.equals("si", ignoreCase = true)) {
                                insideSi = true
                                currentText = StringBuilder()
                            } else if (insideSi && tagName.equals("t", ignoreCase = true)) {
                                insideT = true
                            }
                        }
                        XmlPullParser.TEXT -> {
                            if (insideSi && insideT) {
                                currentText.append(parser.text)
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (tagName.equals("t", ignoreCase = true)) {
                                insideT = false
                            } else if (tagName.equals("si", ignoreCase = true)) {
                                insideSi = false
                                sharedStrings.add(currentText.toString())
                            }
                        }
                    }
                    eventType = parser.next()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Step 2: Find sheet entries (sheet1.xml, sheet2.xml, etc.)
        val sheetKeys = entries.keys.filter { it.startsWith("xl/worksheets/sheet") }.sorted()
        if (sheetKeys.isEmpty()) return emptyList()

        val resultLines = mutableListOf<String>()

        for (sheetKey in sheetKeys) {
            val sheetBytes = entries[sheetKey] ?: continue
            try {
                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = true
                val parser = factory.newPullParser()
                parser.setInput(ByteArrayInputStream(sheetBytes), "UTF-8")

                var eventType = parser.eventType
                var rowCells = mutableMapOf<Int, String>()
                var currentCellRef: String? = null
                var currentCellType: String? = null
                var currentVal = StringBuilder()
                var isVal = false
                var isInlineStr = false

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    val tagName = parser.name
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            if (tagName.equals("row", ignoreCase = true)) {
                                rowCells = mutableMapOf()
                            } else if (tagName.equals("c", ignoreCase = true)) {
                                currentCellRef = getAttribute(parser, "r")
                                currentCellType = getAttribute(parser, "t")
                                currentVal = StringBuilder()
                                isVal = false
                                isInlineStr = false
                            } else if (tagName.equals("v", ignoreCase = true)) {
                                isVal = true
                            } else if (tagName.equals("t", ignoreCase = true)) {
                                isInlineStr = true
                            }
                        }
                        XmlPullParser.TEXT -> {
                            if (isVal || isInlineStr) {
                                currentVal.append(parser.text)
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (tagName.equals("v", ignoreCase = true)) {
                                isVal = false
                            } else if (tagName.equals("t", ignoreCase = true)) {
                                isInlineStr = false
                            } else if (tagName.equals("c", ignoreCase = true)) {
                                val colIdx = if (currentCellRef != null) colLetterToIndex(currentCellRef!!) else rowCells.size
                                val cellRaw = currentVal.toString().trim()
                                val formattedVal = when (currentCellType) {
                                    "s" -> {
                                        val idx = cellRaw.toIntOrNull()
                                        if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else ""
                                    }
                                    "b" -> if (cellRaw == "1") "true" else "false"
                                    else -> cleanNumericString(cellRaw)
                                }
                                rowCells[colIdx] = formattedVal
                            } else if (tagName.equals("row", ignoreCase = true)) {
                                if (rowCells.isNotEmpty()) {
                                    val maxCol = rowCells.keys.maxOrNull() ?: -1
                                    if (maxCol >= 0) {
                                        val lineParts = (0..maxCol).map { col -> rowCells[col] ?: "" }
                                        val line = lineParts.joinToString(";")
                                        if (line.replace(";", "").isNotBlank()) {
                                            resultLines.add(line)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    eventType = parser.next()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return resultLines
    }

    private fun getAttribute(parser: XmlPullParser, attrName: String): String? {
        for (i in 0 until parser.attributeCount) {
            if (parser.getAttributeName(i).equals(attrName, ignoreCase = true)) {
                return parser.getAttributeValue(i)
            }
        }
        return null
    }

    private fun colLetterToIndex(ref: String): Int {
        val colLetters = ref.takeWhile { it.isLetter() }.uppercase()
        if (colLetters.isEmpty()) return 0
        var col = 0
        for (ch in colLetters) {
            col = col * 26 + (ch - 'A' + 1)
        }
        return col - 1
    }

    private fun cleanNumericString(raw: String): String {
        if (raw.isBlank()) return raw
        var str = raw
        if (str.endsWith(".0")) {
            str = str.dropLast(2)
        } else if (str.contains("E", ignoreCase = true)) {
            str = try {
                val bd = BigDecimal(str)
                val plain = bd.toPlainString()
                if (plain.endsWith(".0")) plain.dropLast(2) else plain
            } catch (e: Exception) {
                raw
            }
        }
        return str
    }
}
