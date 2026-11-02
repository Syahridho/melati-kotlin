package com.example.melati.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.melati.R
import com.example.melati.ui.theme.MelatiBorder
import com.example.melati.ui.theme.MelatiDestructive
import com.example.melati.ui.theme.MelatiForeground
import com.example.melati.ui.theme.MelatiHeaderTint
import com.example.melati.ui.theme.MelatiMuted
import com.example.melati.ui.theme.MelatiMutedForeground
import com.example.melati.ui.theme.MelatiPlaceholder
import com.example.melati.ui.theme.MelatiPrimary
import com.example.melati.ui.theme.MelatiPrimaryDisabled
import com.example.melati.ui.theme.MelatiPrimaryTint
import com.example.melati.ui.theme.MelatiRing
import com.example.melati.ui.theme.MelatiSurface

/*
 * Kit komponen bergaya shadcn/ui untuk Melati.
 *
 * Prinsipnya satu: TIDAK ADA elevasi/bayangan di mana pun. Pemisahan bidang
 * dibuat oleh latar putih di atas abu muda (#F8FAFC) plus garis 1dp #E4E4E7.
 * Semua komponen di sini memakai token dari ui/theme/Color.kt sehingga
 * penggantian palet cukup lewat satu file.
 */

private val CardShape = RoundedCornerShape(12.dp)
private val ControlShape = RoundedCornerShape(8.dp)
private val MinTouchTarget = 48.dp

// ===== Ikon =====

/**
 * Ikon "salin" (dua lembar bertumpuk) dibuat sebagai ImageVector di sini karena
 * material-icons-core tidak memuat ContentCopy. Bentuknya mengikuti ikon
 * content_copy Material, tanpa dependency atau font tambahan.
 */
val CopyIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Copy",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(
            pathData = PathParser().parsePathString(
                "M16,1H4C2.9,1 2,1.9 2,3v14h2V3h12V1z" +
                    "M19,5H8C6.9,5 6,5.9 6,7v14c0,1.1 0.9,2 2,2h11c1.1,0 2,-0.9 2,-2V7C21,5.9 20.1,5 19,5z" +
                    "M19,21H8V7h11V21z",
            ).toNodes(),
            fill = SolidColor(Color.Black),
        )
    }.build()
}

// ===== Kartu =====

/** Kartu putih, sudut 12dp, garis 1dp. Tanpa bayangan. */
@Composable
fun ShadcnCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = CardShape,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MelatiSurface,
        contentColor = MelatiForeground,
        border = BorderStroke(1.dp, MelatiBorder),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        content = { Column(content = content) },
    )
}

/** Isi kartu dengan padding standar 20dp. */
@Composable
fun ShadcnCardBody(
    modifier: Modifier = Modifier,
    spacing: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

/**
 * Pita judul kartu: latar biru sangat muda (#F0F7FF) dengan judul 18sp
 * semibold dan subjudul 14sp muted — seperti header kartu di web BPS.
 */
@Composable
fun ShadcnCardHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MelatiHeaderTint)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MelatiForeground,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MelatiMutedForeground,
            )
        }
    }
}

/** Judul layar 20sp semibold + subjudul 13sp muted. */
@Composable
fun ShadcnScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MelatiForeground,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MelatiMutedForeground,
            )
        }
    }
}

// ===== Label =====

/** Label seksi 14sp semibold di atas input, dengan pesan galat opsional. */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MelatiForeground,
        )
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.labelSmall,
                color = MelatiDestructive,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

// ===== Input =====

/**
 * Input teks bergaya shadcn: label 14sp semibold DI ATAS kotak (bukan label
 * mengambang), kotak putih bergaris 1dp dengan sudut 8dp, dan cincin biru
 * lembut saat fokus.
 */
@Composable
fun ShadcnTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String? = null,
    modifier: Modifier = Modifier,
    hint: String? = null,
    error: String? = null,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
) {
    var focused by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MelatiForeground,
                )
                if (hint != null) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.labelSmall,
                        color = MelatiMutedForeground,
                    )
                }
            }
        } else if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.labelSmall,
                color = MelatiMutedForeground,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }

        // Cincin fokus: bingkai 3dp biru 16% yang selalu ada (transparan saat
        // tidak fokus) supaya tata letak tidak bergeser.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 3.dp,
                    color = if (focused && enabled) MelatiRing.copy(alpha = 0.16f) else Color.Transparent,
                    shape = RoundedCornerShape(11.dp),
                )
                .padding(3.dp),
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused },
                enabled = enabled,
                isError = error != null,
                singleLine = singleLine,
                minLines = minLines,
                placeholder = if (placeholder != null) {
                    {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MelatiPlaceholder,
                        )
                    }
                } else {
                    null
                },
                textStyle = MaterialTheme.typography.bodyMedium,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                shape = ControlShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MelatiForeground,
                    unfocusedTextColor = MelatiForeground,
                    disabledTextColor = MelatiMutedForeground,
                    focusedContainerColor = MelatiSurface,
                    unfocusedContainerColor = MelatiSurface,
                    disabledContainerColor = MelatiMuted,
                    errorContainerColor = MelatiSurface,
                    cursorColor = MelatiPrimary,
                    focusedBorderColor = MelatiPrimary,
                    unfocusedBorderColor = MelatiBorder,
                    disabledBorderColor = MelatiBorder,
                    errorBorderColor = MelatiDestructive,
                ),
            )
        }

        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.labelSmall,
                color = MelatiDestructive,
                modifier = Modifier.padding(top = 6.dp, start = 3.dp),
            )
        }
    }
}

// ===== Tombol =====

/** Tombol utama: biru #005FB6, tinggi 48dp, sudut 8dp, teks putih 14sp. */
@Composable
fun ShadcnButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = MinTouchTarget),
        enabled = enabled && !loading,
        shape = ControlShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MelatiPrimary,
            contentColor = Color.White,
            disabledContainerColor = MelatiPrimaryDisabled,
            disabledContentColor = Color.White,
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
            disabledElevation = 0.dp,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Color.White,
            )
            Spacer(Modifier.width(10.dp))
        } else if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Tombol garis: latar putih, garis 1dp #E4E4E7, teks #09090B. */
@Composable
fun ShadcnOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = MinTouchTarget),
        enabled = enabled,
        shape = ControlShape,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MelatiSurface,
            contentColor = MelatiForeground,
            disabledContainerColor = MelatiMuted,
            disabledContentColor = MelatiPlaceholder,
        ),
        border = BorderStroke(1.dp, MelatiBorder),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
            disabledElevation = 0.dp,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Tombol teks tanpa garis untuk aksi ringan (mis. "Hapus semua"). */
@Composable
fun ShadcnTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MelatiPrimary,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = MinTouchTarget),
        shape = ControlShape,
        colors = ButtonDefaults.textButtonColors(contentColor = color),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

// ===== Pilihan =====

/**
 * Satu kartu pilihan (radio) bergaya shadcn: kotak putih bergaris, dan saat
 * terpilih bergaris biru dengan titik radio biru penuh.
 */
@Composable
fun ShadcnRadioCard(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ControlShape)
            .background(MelatiSurface)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) MelatiPrimary else MelatiBorder,
                shape = ControlShape,
            )
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
                // Tanpa ripple: status terpilih sudah ditandai garis biru +
                // titik penuh, dan overlay fokus Material akan tertinggal
                // sebagai latar abu setelah kartu disentuh.
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            )
            .heightIn(min = MinTouchTarget)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioDot(selected = selected)
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = MelatiForeground,
        )
    }
}

/** Grup kartu pilihan; dibungkus agar pembaca layar mengenali satu kelompok. */
@Composable
fun ShadcnRadioGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun RadioDot(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(MelatiSurface)
            .border(
                width = 1.5.dp,
                color = if (selected) MelatiPrimary else Color(0xFFD4D4D8),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MelatiPrimary),
            )
        }
    }
}

// ===== Pemilih berkas bergaris putus-putus =====

/** Tombol pemilih lampiran dengan garis putus-putus, seperti dropzone di web. */
@Composable
fun DashedPicker(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val borderColor = if (enabled) MelatiBorder else MelatiMuted
    val contentColor = if (enabled) MelatiMutedForeground else MelatiPlaceholder
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ControlShape)
            .background(MelatiSurface)
            .drawBehind {
                drawRoundRect(
                    color = borderColor,
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(7.dp.toPx(), 5.dp.toPx()),
                            0f,
                        ),
                    ),
                )
            }
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
        )
    }
}

/** Chip lampiran: latar muted, sudut 8dp, nama berkas + ukuran + tombol hapus. */
@Composable
fun ShadcnFileChip(
    name: String,
    meta: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ControlShape)
            .background(MelatiMuted)
            .heightIn(min = MinTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = MelatiForeground,
                maxLines = 1,
            )
            Text(
                text = meta,
                style = MaterialTheme.typography.labelSmall,
                color = MelatiMutedForeground,
            )
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(MinTouchTarget)) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.cd_hapus_lampiran),
                tint = MelatiMutedForeground,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ===== Panel status =====

/**
 * Kartu status untuk keadaan kosong / gagal: ikon dalam lingkaran muted,
 * judul 18sp semibold, keterangan 14sp muted, dan satu aksi biru.
 */
@Composable
fun ShadcnStateCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Info,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    ShadcnCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MelatiMuted),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MelatiMutedForeground,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MelatiForeground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MelatiMutedForeground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            if (actionLabel != null && onAction != null) {
                ShadcnButton(
                    text = actionLabel,
                    onClick = onAction,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

// ===== Dialog =====

/**
 * Kerangka dialog: kartu putih sudut 12dp dengan padding 24dp. Lebar mengikuti
 * layar (dengan margin 24dp), bukan lebar bawaan platform.
 */
@Composable
fun ShadcnDialog(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(CardShape)
                .background(MelatiSurface)
                .padding(24.dp),
            content = content,
        )
    }
}

/** Dialog konfirmasi: judul, keterangan, lalu dua tombol sejajar. */
@Composable
fun ShadcnAlertDialog(
    title: String,
    message: String? = null,
    confirmLabel: String,
    onConfirm: () -> Unit,
    dismissLabel: String,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    extraContent: (@Composable () -> Unit)? = null,
) {
    ShadcnDialog(onDismissRequest = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MelatiForeground,
            )
            extraContent?.invoke()
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MelatiMutedForeground,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ShadcnOutlinedButton(
                text = dismissLabel,
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = MinTouchTarget),
                shape = ControlShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (destructive) MelatiDestructive else MelatiPrimary,
                    contentColor = Color.White,
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp,
                    disabledElevation = 0.dp,
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(text = confirmLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Kotak abu berisi satu nilai penting, mis. nomor tiket yang baru dibuat. */
@Composable
fun ShadcnValueBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(ControlShape)
            .background(MelatiMuted)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MelatiMutedForeground,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MelatiForeground,
        )
    }
}
