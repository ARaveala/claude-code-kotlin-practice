package com.practice.plantUser.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.practice.plantUser.model.ValidationMessages
import com.practice.plantUser.model.heightRejectionReason
import com.practice.plantUser.model.needsSizeConfirm
import com.practice.plantUser.model.widthDepthRejectionReason

/**
 * Size fields parsed and checked against model/SizingLimits.kt. Shared by the create dialog and the
 * edit panel. [widthCm]/[depthCm]/[heightCm] are null unless the field parses and passes the hard
 * limits; these exact Doubles feed both the ghost preview and confirm, so preview == applied.
 * Errors are null for a blank or half-typed field ("12." parses, "." doesn't): no message, just invalid.
 */
class SizeInput(
    val widthCm: Double?,
    val depthCm: Double?,
    val heightCm: Double?,
    val widthError: String?,
    val depthError: String?,
    val heightError: String?,
    val isValid: Boolean,
    /** The value to show in the soft warning (larger of width/depth), or null if no warning needed. */
    val softWarnValueCm: Double?
)

fun checkSizeInput(widthText: String, depthText: String, heightText: String): SizeInput {
    // toDoubleOrNull returns a boxed Double? (heap), three per call; callers remember by text.
    val widthParsed = widthText.toDoubleOrNull()
    val depthParsed = depthText.toDoubleOrNull()
    val heightParsed = heightText.toDoubleOrNull()

    val widthError = widthParsed?.let { widthDepthRejectionReason(it) }
    val depthError = depthParsed?.let { widthDepthRejectionReason(it) }
    val heightError = heightParsed?.let { heightRejectionReason(it) }

    val widthCm = if (widthError == null) widthParsed else null
    val depthCm = if (depthError == null) depthParsed else null
    val heightCm = if (heightError == null) heightParsed else null
    // Height is optional: blank is fine, but non-blank text that doesn't parse is not.
    val heightOk = heightText.isBlank() || heightCm != null

    val isValid = widthCm != null && depthCm != null && heightOk
    val softWarnValueCm =
        if (widthCm == null || depthCm == null || !heightOk) {
            null
        } else if (needsSizeConfirm(widthCm) || needsSizeConfirm(depthCm)) {
            maxOf(widthCm, depthCm)
        } else {
            null
        }
    return SizeInput(widthCm, depthCm, heightCm, widthError, depthError, heightError, isValid, softWarnValueCm)
}

/** Width, depth and optional height fields, each showing its rejection message under it. */
@Composable
fun SizeFields(
    widthText: String,
    depthText: String,
    heightText: String,
    onWidthChange: (String) -> Unit,
    onDepthChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    sizeInput: SizeInput
) {
    Column {
        Row {
            DimensionField(widthText, onWidthChange, "Width (cm)", sizeInput.widthError, Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            DimensionField(depthText, onDepthChange, "Depth (cm)", sizeInput.depthError, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        DimensionField(heightText, onHeightChange, "Height (cm, optional)", sizeInput.heightError, Modifier)
    }
}

@Composable
private fun DimensionField(
    text: String,
    onChange: (String) -> Unit,
    label: String,
    error: String?,
    modifier: Modifier
) {
    TextField(
        value = text,
        onValueChange = { input -> onChange(coerceDimensionInput(text, input)) },
        singleLine = true,
        label = { Text(label) },
        isError = error != null,
        supportingText = if (error != null) {
            { Text(error) }
        } else {
            null
        },
        modifier = modifier
    )
}

/** Soft warning text (fat finger check). The String is built once per value, not per recomposition. */
@Composable
fun SoftWarningMessage(valueCm: Double) {
    val message = remember(valueCm) { ValidationMessages.sizeSoftWarning(valueCm) }
    Text(message, color = MaterialTheme.colorScheme.tertiary)
}
