/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.developers.androidify.vertexai

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.android.developers.androidify.model.GeneratedPrompt
import com.android.developers.androidify.model.ImageValidationError
import com.android.developers.androidify.model.ValidatedDescription
import com.android.developers.androidify.model.ValidatedImage
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

interface FirebaseAiDataSource {
    suspend fun validatePromptHasEnoughInformation(inputPrompt: String): ValidatedDescription
    suspend fun validateImageHasEnoughInformation(image: Bitmap): ValidatedImage
    suspend fun generateDescriptivePromptFromImage(image: Bitmap): ValidatedDescription
    suspend fun generateImageFromPromptAndSkinTone(prompt: String, skinTone: String): Bitmap
    suspend fun generatePrompt(prompt: String): GeneratedPrompt
    suspend fun generateImageWithEdit(image: Bitmap, backgroundPrompt: String): Bitmap
}

/**
 * Local stub that replaces the former Google AI backend integration.
 *
 * The methods return lightweight, deterministic results so the rest of the
 * pipeline can continue to function without remote calls.
 */
@Singleton
class FirebaseAiDataSourceImpl @Inject constructor() : FirebaseAiDataSource {

    override suspend fun validatePromptHasEnoughInformation(
        inputPrompt: String,
    ): ValidatedDescription {
        val normalizedPrompt = inputPrompt.trim()
        val hasContent = normalizedPrompt.isNotEmpty()
        return ValidatedDescription(hasContent, normalizedPrompt.takeIf { hasContent })
    }

    override suspend fun validateImageHasEnoughInformation(image: Bitmap): ValidatedImage {
        val hasPixels = image.width > 0 && image.height > 0
        return ValidatedImage(hasPixels, errorMessage = if (hasPixels) null else ImageValidationError.NOT_PERSON)
    }

    override suspend fun generateDescriptivePromptFromImage(image: Bitmap): ValidatedDescription {
        val description =
            "Androidify-ready portrait ${image.width}x${image.height} with clear foreground subject."
        return ValidatedDescription(true, description)
    }

    override suspend fun generateImageFromPromptAndSkinTone(prompt: String, skinTone: String): Bitmap {
        val label = if (prompt.isBlank()) "Androidify bot" else prompt
        return createPlaceholderBitmap(label, skinTone)
    }

    override suspend fun generatePrompt(prompt: String): GeneratedPrompt {
        val base = prompt.ifBlank { "Androidify bot" }
        return GeneratedPrompt(true, listOf(base, "$base with playful style"))
    }

    override suspend fun generateImageWithEdit(image: Bitmap, backgroundPrompt: String): Bitmap {
        // Preserve the provided bot while overlaying a hint of the requested background as text.
        val editableBitmap = image.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(editableBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = min(image.width, image.height) / 12f
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
        }
        val text = backgroundPrompt.take(40)
        canvas.drawText(text, 24f, paint.textSize + 24f, paint)
        return editableBitmap
    }

    private fun createPlaceholderBitmap(prompt: String, accent: String): Bitmap {
        val size = 512
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#263238"))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#80CBC4")
            textSize = 32f
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.LTGRAY
            textSize = 24f
        }

        canvas.drawText("Androidify", 32f, 80f, paint)
        canvas.drawText(prompt.take(30), 32f, 140f, subtitlePaint)
        if (accent.isNotBlank()) {
            canvas.drawText("Tone: ${accent.take(20)}", 32f, 200f, subtitlePaint)
        }

        return bitmap
    }
}
