package com.fordham.toolbelt.presentation.payment.qr

import com.fordham.toolbelt.domain.payment.qr.PaymentCheckoutUrl
import kotlin.math.abs

data class QrBitMatrix(val size: Int, val matrix: BooleanArray) {
    fun isDark(row: Int, col: Int): Boolean = matrix[row * size + col]
}

object QrCodeMatrixEngine {
    private val GF256_EXP = IntArray(512)
    private val GF256_LOG = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            GF256_EXP[i] = x
            GF256_LOG[x] = i
            x = x shl 1
            if (x >= 256) {
                x = x xor 285 // Generator polynomial x^8 + x^4 + x^3 + x^2 + 1
            }
        }
        for (i in 255 until 512) {
            GF256_EXP[i] = GF256_EXP[i - 255]
        }
    }

    private fun gfMul(a: Int, b: Int): Int {
        if (a == 0 || b == 0) return 0
        return GF256_EXP[GF256_LOG[a] + GF256_LOG[b]]
    }

    private class RSGenerator(val degree: Int) {
        val poly = IntArray(degree + 1)

        init {
            poly[0] = 1
            for (i in 0 until degree) {
                val root = GF256_EXP[i]
                for (j in i + 1 downTo 1) {
                    poly[j] = poly[j - 1] xor gfMul(poly[j], root)
                }
                poly[0] = gfMul(poly[0], root)
            }
        }
    }

    private fun generateEcc(data: ByteArray, eccCount: Int): ByteArray {
        val generator = RSGenerator(eccCount)
        val info = IntArray(data.size + eccCount)
        for (i in data.indices) {
            info[i] = data[i].toInt() and 0xFF
        }

        for (i in data.indices) {
            val coef = info[i]
            if (coef != 0) {
                for (j in 0 until eccCount) {
                    info[i + 1 + j] = info[i + 1 + j] xor gfMul(generator.poly[eccCount - 1 - j], coef)
                }
            }
        }

        val ecc = ByteArray(eccCount)
        for (i in 0 until eccCount) {
            ecc[i] = info[data.size + i].toByte()
        }
        return ecc
    }

    // Capacity table for Version 1 to 22 (Error Correction Level M)
    // Format: Version, Size, Data Codewords, ECC Codewords, Blocks
    private data class VersionInfo(
        val version: Int,
        val size: Int,
        val dataCodewords: Int,
        val eccCodewords: Int,
        val blocks: Int
    )

    private val versionTable = listOf(
        VersionInfo(1, 21, 16, 10, 1),
        VersionInfo(2, 25, 28, 16, 1),
        VersionInfo(3, 29, 44, 26, 1),
        VersionInfo(4, 33, 64, 36, 2),
        VersionInfo(5, 37, 86, 48, 2),
        VersionInfo(6, 41, 108, 64, 4),
        VersionInfo(7, 45, 124, 72, 4),
        VersionInfo(8, 49, 154, 88, 4),
        VersionInfo(9, 53, 182, 110, 5),
        VersionInfo(10, 57, 216, 130, 5),
        VersionInfo(11, 61, 254, 150, 5),
        VersionInfo(12, 65, 290, 176, 8),
        VersionInfo(13, 69, 330, 208, 8),
        VersionInfo(14, 73, 368, 234, 9),
        VersionInfo(15, 77, 416, 260, 10),
        VersionInfo(16, 81, 482, 280, 10),
        VersionInfo(17, 85, 530, 308, 11),
        VersionInfo(18, 89, 584, 336, 12),
        VersionInfo(19, 93, 644, 364, 13),
        VersionInfo(20, 97, 704, 392, 14),
        VersionInfo(21, 101, 750, 420, 15),
        VersionInfo(22, 105, 812, 448, 16)
    )

    private fun selectVersion(dataLength: Int): VersionInfo {
        // Mode indicator (4 bits) + Character count (8 or 16 bits) + Terminator (4 bits)
        // Let's find first version that can fit dataLength + overhead
        val overheadBits = 4 + 8 // Byte mode + char count (up to version 9)
        val overheadBytes = (overheadBits + 7) / 8
        val requiredBytes = dataLength + overheadBytes

        for (v in versionTable) {
            if (v.dataCodewords >= requiredBytes) {
                return v
            }
        }
        return versionTable.last()
    }

    private val alignmentPatternPositions = mapOf(
        1 to intArrayOf(),
        2 to intArrayOf(6, 18),
        3 to intArrayOf(6, 22),
        4 to intArrayOf(6, 26),
        5 to intArrayOf(6, 30),
        6 to intArrayOf(6, 34),
        7 to intArrayOf(6, 22, 38),
        8 to intArrayOf(6, 24, 42),
        9 to intArrayOf(6, 26, 46),
        10 to intArrayOf(6, 28, 50),
        11 to intArrayOf(6, 30, 54),
        12 to intArrayOf(6, 32, 58),
        13 to intArrayOf(6, 34, 62),
        14 to intArrayOf(6, 26, 46, 66),
        15 to intArrayOf(6, 26, 48, 70),
        16 to intArrayOf(6, 26, 50, 74),
        17 to intArrayOf(6, 30, 54, 78),
        18 to intArrayOf(6, 30, 56, 82),
        19 to intArrayOf(6, 30, 58, 86),
        20 to intArrayOf(6, 34, 62, 90),
        21 to intArrayOf(6, 28, 50, 72, 94),
        22 to intArrayOf(6, 26, 50, 74, 98)
    )

    fun encodeUrl(checkoutUrl: PaymentCheckoutUrl): QrBitMatrix {
        val rawBytes = checkoutUrl.value.encodeToByteArray()
        val version = selectVersion(rawBytes.size)
        val size = version.size

        val matrix = BooleanArray(size * size)
        val reserved = BooleanArray(size * size)

        fun setModule(r: Int, c: Int, valB: Boolean, reserve: Boolean = true) {
            matrix[r * size + c] = valB
            if (reserve) reserved[r * size + c] = true
        }

        // 1. Draw Finder Patterns
        fun drawFinderPattern(row: Int, col: Int) {
            for (r in -1..7) {
                for (c in -1..7) {
                    val pr = row + r
                    val pc = col + c
                    if (pr in 0 until size && pc in 0 until size) {
                        val isBorder = r == -1 || r == 7 || c == -1 || c == 7
                        val isOuter = r == 0 || r == 6 || c == 0 || c == 6
                        val isInner = r in 2..4 && c in 2..4
                        setModule(pr, pc, !isBorder && (isOuter || isInner), reserve = true)
                    }
                }
            }
        }
        drawFinderPattern(0, 0)
        drawFinderPattern(size - 7, 0)
        drawFinderPattern(0, size - 7)

        // 2. Draw Alignment Patterns
        val alignPos = alignmentPatternPositions[version.version] ?: intArrayOf()
        for (r in alignPos) {
            for (c in alignPos) {
                // Avoid overlapping finder patterns
                if ((r < 8 && c < 8) || (r < 8 && c >= size - 8) || (r >= size - 8 && c < 8)) continue
                for (dr in -2..2) {
                    for (dc in -2..2) {
                        val isOuter = abs(dr) == 2 || abs(dc) == 2
                        val isCenter = dr == 0 && dc == 0
                        setModule(r + dr, c + dc, isOuter || isCenter, reserve = true)
                    }
                }
            }
        }

        // 3. Draw Timing Patterns
        for (i in 8 until size - 8) {
            val bit = (i % 2 == 0)
            setModule(6, i, bit, reserve = true)
            setModule(i, 6, bit, reserve = true)
        }

        // 4. Dark Module
        setModule(4 * version.version + 9, 8, true, reserve = true)

        // 5. Build Bit Buffer (8-bit Byte Mode)
        val dataCodewords = version.dataCodewords
        val bitBuffer = ArrayList<Boolean>()

        // Mode Indicator: Byte Mode (0100)
        bitBuffer.add(false); bitBuffer.add(true); bitBuffer.add(false); bitBuffer.add(false)

        // Character Count Indicator (8 bits for Version < 10)
        val charCountSize = if (version.version < 10) 8 else 16
        for (i in (charCountSize - 1) downTo 0) {
            bitBuffer.add(((rawBytes.size shl (31 - i)) ushr 31) != 0)
        }

        // Data Bytes
        for (b in rawBytes) {
            for (i in 7 downTo 0) {
                bitBuffer.add(((b.toInt() shl (31 - i)) ushr 31) != 0)
            }
        }

        // Terminator: 4 zero bits (or up to capacity)
        val maxDataBits = dataCodewords * 8
        for (i in 0 until 4) {
            if (bitBuffer.size < maxDataBits) bitBuffer.add(false)
        }

        // Round up to multiple of 8
        while (bitBuffer.size % 8 != 0 && bitBuffer.size < maxDataBits) {
            bitBuffer.add(false)
        }

        // Pad with Alternating Bytes (0xEC, 0x11)
        val padBytes = byteArrayOf(0xEC.toByte(), 0x11.toByte())
        var padIndex = 0
        while (bitBuffer.size < maxDataBits) {
            val b = padBytes[padIndex % 2]
            for (i in 7 downTo 0) {
                bitBuffer.add(((b.toInt() shl (31 - i)) ushr 31) != 0)
            }
            padIndex++
        }

        // Convert bitBuffer to Codewords
        val dataBytes = ByteArray(dataCodewords)
        for (i in dataBytes.indices) {
            var b = 0
            for (j in 0 until 8) {
                if (bitBuffer[i * 8 + j]) {
                    b = b or (1 shl (7 - j))
                }
            }
            dataBytes[i] = b.toByte()
        }

        // 6. Divide into Blocks and Calculate Error Correction
        val numBlocks = version.blocks
        val eccBytesPerBlock = version.eccCodewords / numBlocks
        val shortBlockDataSize = dataCodewords / numBlocks
        val numLongBlocks = dataCodewords % numBlocks

        val blockData = Array(numBlocks) { ByteArray(0) }
        val blockEcc = Array(numBlocks) { ByteArray(0) }

        var byteOffset = 0
        for (i in 0 until numBlocks) {
            val blockSize = shortBlockDataSize + (if (i >= numBlocks - numLongBlocks) 1 else 0)
            val subData = ByteArray(blockSize)
            dataBytes.copyInto(destination = subData, destinationOffset = 0, startIndex = byteOffset, endIndex = byteOffset + blockSize)
            byteOffset += blockSize

            blockData[i] = subData
            blockEcc[i] = generateEcc(subData, eccBytesPerBlock)
        }

        // Interleave Data and ECC Codewords
        val interleaved = ArrayList<Byte>()
        val maxBlockDataSize = shortBlockDataSize + 1
        for (j in 0 until maxBlockDataSize) {
            for (i in 0 until numBlocks) {
                if (j < blockData[i].size) {
                    interleaved.add(blockData[i][j])
                }
            }
        }
        for (j in 0 until eccBytesPerBlock) {
            for (i in 0 until numBlocks) {
                interleaved.add(blockEcc[i][j])
            }
        }

        // Convert interleaved to final bit stream
        val finalBits = ArrayList<Boolean>()
        for (b in interleaved) {
            for (i in 7 downTo 0) {
                finalBits.add(((b.toInt() shl (31 - i)) ushr 31) != 0)
            }
        }

        // 7. Place Data bits in Matrix
        var dataBitIndex = 0
        var right = size - 1
        var upwards = true
        while (right > 0) {
            if (right == 6) right-- // Skip vertical timing pattern
            for (r in 0 until size) {
                val row = if (upwards) size - 1 - r else r
                for (colOffset in 0..1) {
                    val col = right - colOffset
                    if (!reserved[row * size + col]) {
                        val bit = if (dataBitIndex < finalBits.size) finalBits[dataBitIndex++] else false
                        setModule(row, col, bit, reserve = false)
                    }
                }
            }
            upwards = !upwards
            right -= 2
        }

        // 8. Add Format Information (XOR mask pattern 0 = (row+col)%2==0)
        // Error Correction level M is 00 in binary. XOR standard mask = 000. Combined format string is: 101010000010010
        val formatInfo = 0x5412 // 101010000010010
        for (i in 0..14) {
            val bit = ((formatInfo shr (14 - i)) and 1) != 0

            // Top-left and bottom-left / top-right mappings
            if (i < 6) {
                setModule(i, 8, bit, reserve = false)
            } else if (i < 8) {
                setModule(i + 1, 8, bit, reserve = false)
            } else if (i == 8) {
                setModule(8, 7, bit, reserve = false)
            } else {
                setModule(8, 14 - i, bit, reserve = false)
            }

            // High regions
            if (i < 8) {
                setModule(8, size - 1 - i, bit, reserve = false)
            } else {
                setModule(size - 15 + i, 8, bit, reserve = false)
            }
        }

        // Apply Mask Pattern 0: (row + col) % 2 == 0 to non-reserved modules
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (!reserved[r * size + c]) {
                    if ((r + c) % 2 == 0) {
                        matrix[r * size + c] = !matrix[r * size + c]
                    }
                }
            }
        }

        return QrBitMatrix(size, matrix)
    }
}
