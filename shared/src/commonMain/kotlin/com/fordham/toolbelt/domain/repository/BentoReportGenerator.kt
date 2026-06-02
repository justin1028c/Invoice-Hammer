package com.fordham.toolbelt.domain.repository

import com.fordham.toolbelt.domain.model.BentoReportData

/**
 * Platform PDF generator for the Stats Bento business report.
 */
interface BentoReportGenerator {
    /**
     * @param outputPath Absolute path for the PDF file (parent directory must exist).
     * @return true when the file was written successfully.
     */
    fun generate(data: BentoReportData, outputPath: String): Boolean
}
