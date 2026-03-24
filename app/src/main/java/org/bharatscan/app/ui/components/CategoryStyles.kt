/*
 * Copyright 2025-2026 Ahan Sardar
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.bharatscan.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import org.bharatscan.app.ui.model.DocumentCategory

data class CategoryStyle(
    val icon: ImageVector,
    val color: Color
)

fun categoryStyleFor(categoryId: String?, label: String): CategoryStyle {
    val normalized = label.trim().lowercase()
    val tokens = normalized
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .toSet()

    val resolved = DocumentCategory.fromId(categoryId) ?: DocumentCategory.resolveFromCustomName(label)

    return when {
        resolved == DocumentCategory.ID_PROOFS ||
            tokens.any { it in setOf("id", "aadhaar", "aadhar", "pan", "passport", "license", "voter") } ->
            CategoryStyle(Icons.Default.CreditCard, Color(0xFF1E3A8A))
        resolved == DocumentCategory.EDUCATION ||
            tokens.any { it in setOf("school", "college", "university", "degree", "certificate", "marksheet", "education") } ->
            CategoryStyle(Icons.Default.School, Color(0xFF2E7D32))
        tokens.any { it in setOf("bill", "invoice", "tax", "receipt", "finance", "bank", "statement") } ->
            CategoryStyle(Icons.Default.ReceiptLong, Color(0xFF00838F))
        tokens.any { it in setOf("health", "medical", "insurance", "hospital", "clinic") } ->
            CategoryStyle(Icons.Default.HealthAndSafety, Color(0xFFD32F2F))
        tokens.any { it in setOf("work", "job", "salary", "payslip", "offer", "employment", "resume") } ->
            CategoryStyle(Icons.Default.Work, Color(0xFF6A1B9A))
        tokens.any { it in setOf("home", "house", "property", "rent", "lease", "mortgage") } ->
            CategoryStyle(Icons.Default.Home, Color(0xFF6D4C41))
        else -> CategoryStyle(Icons.Default.Description, Color(0xFF546E7A))
    }
}
