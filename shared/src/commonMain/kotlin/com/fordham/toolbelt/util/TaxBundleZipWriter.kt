package com.fordham.toolbelt.util

/**
 * Writes a ZIP archive using STORE (no compression) so it works on all KMP targets.
 */
class TaxBundleZipWriter(private val outputPath: String) {
    private val entries = mutableListOf<ZipEntryPayload>()

    fun addFileFromPath(entryPath: String, sourcePath: String) {
        val bytes = readAllBytes(sourcePath) ?: return
        if (bytes.isEmpty()) return
        entries.add(ZipEntryPayload(entryPath.replace('\\', '/'), bytes))
    }

    fun close() {
        if (entries.isEmpty()) {
            writeAllBytes(outputPath, buildEndOfCentralDirectory(0, 0L, 0L))
            return
        }
        val localParts = mutableListOf<ByteArray>()
        var offset = 0L
        val localOffsets = mutableListOf<Long>()

        entries.forEach { entry ->
            val local = buildLocalFileHeader(entry, offset)
            localOffsets.add(offset)
            localParts.add(local)
            offset += local.size.toLong()
            localParts.add(entry.bytes)
            offset += entry.bytes.size.toLong()
        }

        val centralDirectory = buildCentralDirectory(entries, localOffsets)
        val endRecord = buildEndOfCentralDirectory(
            entryCount = entries.size,
            centralDirectorySize = centralDirectory.size.toLong(),
            centralDirectoryOffset = offset
        )

        val totalSize = localParts.sumOf { it.size } + centralDirectory.size + endRecord.size
        val output = ByteArray(totalSize)
        var pos = 0
        localParts.forEach { part ->
            part.copyInto(output, destinationOffset = pos)
            pos += part.size
        }
        centralDirectory.copyInto(output, destinationOffset = pos)
        pos += centralDirectory.size
        endRecord.copyInto(output, destinationOffset = pos)

        writeAllBytes(outputPath, output)
    }
}

internal fun writeTaxBundleZip(
    outputPath: String,
    businessReportPath: String?,
    invoicePdfPaths: List<String>
) {
    TaxBundleZipWriter(outputPath).use { zip ->
        businessReportPath?.let { path ->
            zip.addFileFromPath("Business_Report.pdf", path)
        }
        val added = mutableSetOf<String>()
        invoicePdfPaths.forEach { pdfPath ->
            val name = pdfPath.substringAfterLast('/').substringAfterLast('\\')
            if (name.isBlank()) return@forEach
            val entryPath = "Invoices/$name"
            if (added.add(entryPath)) {
                zip.addFileFromPath(entryPath, pdfPath)
            }
        }
    }
}

private inline fun TaxBundleZipWriter.use(block: (TaxBundleZipWriter) -> Unit) {
    try {
        block(this)
    } finally {
        close()
    }
}

internal expect fun readAllBytes(path: String): ByteArray?

internal expect fun writeAllBytes(path: String, bytes: ByteArray)

private data class ZipEntryPayload(val path: String, val bytes: ByteArray)

private const val LOCAL_FILE_HEADER_SIGNATURE = 0x04034b50
private const val CENTRAL_DIRECTORY_SIGNATURE = 0x02014b50
private const val END_OF_CENTRAL_DIRECTORY_SIGNATURE = 0x06054b50

private fun buildLocalFileHeader(entry: ZipEntryPayload, offset: Long): ByteArray {
    val nameBytes = entry.path.encodeToByteArray()
    val crc = Crc32.compute(entry.bytes)
    val size = entry.bytes.size.toLong()
    val header = ByteArray(30 + nameBytes.size)
    header.writeUInt32LE(0, LOCAL_FILE_HEADER_SIGNATURE.toLong())
    header.writeUInt16LE(4, 10)  // Version needed (1.0)
    header.writeUInt16LE(6, 0)   // Flags
    header.writeUInt16LE(8, 0)   // Compression (0 = STORED)
    header.writeUInt16LE(10, 0)  // Mod time
    header.writeUInt16LE(12, 0)  // Mod date
    header.writeUInt32LE(14, crc) // CRC-32
    header.writeUInt32LE(18, size) // Compressed size
    header.writeUInt32LE(22, size) // Uncompressed size
    header.writeUInt16LE(26, nameBytes.size) // File name length
    header.writeUInt16LE(28, 0)  // Extra field length
    nameBytes.copyInto(header, destinationOffset = 30)
    return header
}

private fun buildCentralDirectory(
    entries: List<ZipEntryPayload>,
    localOffsets: List<Long>
): ByteArray {
    val parts = entries.mapIndexed { index, entry ->
        val nameBytes = entry.path.encodeToByteArray()
        val crc = Crc32.compute(entry.bytes)
        val size = entry.bytes.size.toLong()
        val header = ByteArray(46 + nameBytes.size)
        header.writeUInt32LE(0, CENTRAL_DIRECTORY_SIGNATURE.toLong())
        header.writeUInt16LE(4, 20)  // Version made by (2.0)
        header.writeUInt16LE(6, 10)  // Version needed (1.0)
        header.writeUInt16LE(8, 0)   // Flags
        header.writeUInt16LE(10, 0)  // Compression (0 = STORED)
        header.writeUInt16LE(12, 0)  // Mod time
        header.writeUInt16LE(14, 0)  // Mod date
        header.writeUInt32LE(16, crc) // CRC-32
        header.writeUInt32LE(20, size) // Compressed size
        header.writeUInt32LE(24, size) // Uncompressed size
        header.writeUInt16LE(28, nameBytes.size) // File name length
        header.writeUInt16LE(30, 0)  // Extra field length
        header.writeUInt16LE(32, 0)  // Comment length
        header.writeUInt16LE(34, 0)  // Disk number start
        header.writeUInt16LE(36, 0)  // Internal attributes
        header.writeUInt32LE(38, 0)  // External attributes
        header.writeUInt32LE(42, localOffsets[index]) // Relative offset of local header
        nameBytes.copyInto(header, destinationOffset = 46)
        header
    }
    val totalSize = parts.sumOf { it.size }
    return ByteArray(totalSize).also { out ->
        var pos = 0
        parts.forEach { part ->
            part.copyInto(out, destinationOffset = pos)
            pos += part.size
        }
    }
}

private fun buildEndOfCentralDirectory(
    entryCount: Int,
    centralDirectorySize: Long,
    centralDirectoryOffset: Long
): ByteArray {
    val record = ByteArray(22)
    record.writeUInt32LE(0, END_OF_CENTRAL_DIRECTORY_SIGNATURE.toLong())
    record.writeUInt16LE(4, 0)
    record.writeUInt16LE(6, 0)
    record.writeUInt16LE(8, entryCount)
    record.writeUInt16LE(10, entryCount)
    record.writeUInt32LE(12, centralDirectorySize)
    record.writeUInt32LE(16, centralDirectoryOffset)
    record.writeUInt16LE(20, 0)
    return record
}

private fun ByteArray.writeUInt16LE(offset: Int, value: Int) {
    this[offset] = (value and 0xFF).toByte()
    this[offset + 1] = ((value shr 8) and 0xFF).toByte()
}

private fun ByteArray.writeUInt32LE(offset: Int, value: Long) {
    this[offset] = (value and 0xFF).toByte()
    this[offset + 1] = ((value shr 8) and 0xFF).toByte()
    this[offset + 2] = ((value shr 16) and 0xFF).toByte()
    this[offset + 3] = ((value shr 24) and 0xFF).toByte()
}

private object Crc32 {
    private val table = IntArray(256) { i ->
        var crc = i
        repeat(8) {
            crc = if (crc and 1 != 0) (crc ushr 1) xor 0xEDB88320.toInt() else crc ushr 1
        }
        crc
    }

    fun compute(data: ByteArray): Long {
        var crc = 0xFFFFFFFF.toInt()
        for (byte in data) {
            val index = (crc xor byte.toInt()) and 0xFF
            crc = (crc ushr 8) xor table[index]
        }
        return (crc xor 0xFFFFFFFF.toInt()).toLong() and 0xFFFFFFFFL
    }
}
