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
package org.bharatscan.app.ui.model

import androidx.annotation.StringRes
import org.bharatscan.app.R

enum class DocumentCategory(val id: String, @param:StringRes val labelRes: Int) {
    ID_PROOFS("id_proofs", R.string.id_proofs),
    EDUCATION("education", R.string.education),
    OTHER("other", R.string.other),
    ;

    companion object {
        const val CUSTOM_PREFIX = "custom:"
        private val ID_PROOFS_KEYWORDS = setOf(
            "aadhaar",
            "aadhar",
            "uidai",
            "pan",
            "passport",
            "voter",
            "license",
            "driving",
            "identity",
            "id"
        )
        private val EDUCATION_KEYWORDS = setOf(
            "degree",
            "certificate",
            "education",
            "school",
            "college",
            "university",
            "transcript",
            "marksheet"
        )

        fun fromId(id: String?): DocumentCategory? {
            if (id.isNullOrBlank()) return null
            return values().firstOrNull { it.id.equals(id, ignoreCase = true) }
        }

        fun resolveFromCustomName(name: String): DocumentCategory? {
            val tokens = normalizeTokens(name)
            return when {
                tokens.any { ID_PROOFS_KEYWORDS.contains(it) } -> ID_PROOFS
                tokens.any { EDUCATION_KEYWORDS.contains(it) } -> EDUCATION
                else -> null
            }
        }

        fun matchesFilter(
            category: DocumentCategory,
            categoryId: String?,
            fileName: String,
        ): Boolean {
            if (!categoryId.isNullOrBlank()) {
                return categoryId.equals(category.id, ignoreCase = true)
            }
            val tokens = normalizeTokens(fileName)
            return when (category) {
                ID_PROOFS -> tokens.any { ID_PROOFS_KEYWORDS.contains(it) }
                EDUCATION -> tokens.any { EDUCATION_KEYWORDS.contains(it) }
                OTHER -> false
            }
        }

        fun encodeCustom(name: String): String {
            return CUSTOM_PREFIX + name.trim()
        }

        fun decodeCustom(value: String?): String? {
            if (value.isNullOrBlank()) return null
            return if (value.startsWith(CUSTOM_PREFIX, ignoreCase = true)) {
                value.removePrefix(CUSTOM_PREFIX).trim().ifBlank { null }
            } else null
        }

        private fun normalizeTokens(raw: String): Set<String> {
            val normalized = raw
                .trim()
                .lowercase()
                .replace(Regex("[^a-z0-9]+"), " ")
                .trim()
            if (normalized.isBlank()) return emptySet()
            return normalized.split(Regex("\\s+")).toSet()
        }
    }
}
