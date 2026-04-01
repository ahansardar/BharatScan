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
package org.bharatscan.app.ui

enum class TutorialStep {
    HOME,
    SCAN,
    EDIT,
    EXPORT,
    SEARCH,
}

fun TutorialStep.next(): TutorialStep? = when (this) {
    TutorialStep.HOME -> TutorialStep.SCAN
    TutorialStep.SCAN -> TutorialStep.EDIT
    TutorialStep.EDIT -> TutorialStep.EXPORT
    TutorialStep.EXPORT -> TutorialStep.SEARCH
    TutorialStep.SEARCH -> null
}

fun TutorialStep.previous(): TutorialStep? = when (this) {
    TutorialStep.HOME -> null
    TutorialStep.SCAN -> TutorialStep.HOME
    TutorialStep.EDIT -> TutorialStep.SCAN
    TutorialStep.EXPORT -> TutorialStep.EDIT
    TutorialStep.SEARCH -> TutorialStep.EXPORT
}
