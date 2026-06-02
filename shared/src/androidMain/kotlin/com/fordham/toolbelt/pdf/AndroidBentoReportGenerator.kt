package com.fordham.toolbelt.pdf

import com.fordham.toolbelt.domain.model.BentoReportData
import com.fordham.toolbelt.domain.repository.BentoReportGenerator
import java.io.File

class AndroidBentoReportGenerator(
    private val engine: BentoReportEngine
) : BentoReportGenerator {
    override fun generate(data: BentoReportData, outputPath: String): Boolean {
        val file = engine.generateBentoPdf(data, File(outputPath)) ?: return false
        return file.exists() && file.length() > 0L
    }
}
