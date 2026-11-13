package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.CourseSession
import com.example.domain.ScheduleConflict
import com.example.domain.Semester
import com.example.domain.SemesterStatus
import com.example.domain.SessionType
import com.example.domain.TimetableFormatters
import com.example.domain.conflict.TimetableConflictChecker
import com.example.ui.theme.BrandLogoGold
import com.example.ui.theme.BrandLogoNavy
import com.example.ui.theme.BrandLogoNavyLight
import com.example.ui.theme.DarkCrimson
import com.example.ui.theme.DarkPrimaryBlue
import com.example.ui.theme.DarkTeal
import com.example.ui.theme.LabAccent
import com.example.ui.theme.LectureAccent
import com.example.ui.theme.OfficeHoursAccent
import com.example.ui.theme.OxfordNavy
import com.example.ui.theme.OxfordNavyLight
import com.example.ui.theme.SeminarAccent
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun ThemeTogglePillSwitch(
    isDarkMode: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    onBlueHeader: Boolean = false
) {
    val trackColor by animateColorAsState(
        targetValue = when {
            onBlueHeader && isDarkMode -> Color.White.copy(alpha = 0.22f)
            onBlueHeader -> Color.White.copy(alpha = 0.14f)
            isDarkMode -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(250),
        label = "theme_switch_track"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (isDarkMode) 28.dp else 2.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "theme_switch_thumb"
    )
    val borderColor = if (onBlueHeader) {
        Color.White.copy(alpha = 0.28f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }
    val thumbColor = if (onBlueHeader) Color.White else MaterialTheme.colorScheme.primary
    val thumbIconColor = if (onBlueHeader) BrandLogoNavy else MaterialTheme.colorScheme.onPrimary

    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .clickable(
                role = Role.Switch,
                onClick = onToggle
            )
            .testTag("theme_mode_toggle_switch"),
        shape = CircleShape,
        color = trackColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = if (onBlueHeader) 0.dp else 2.dp
    ) {
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(34.dp)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.LightMode,
                    contentDescription = null,
                    tint = when {
                        onBlueHeader && !isDarkMode -> BrandLogoGold
                        onBlueHeader -> Color.White.copy(alpha = 0.55f)
                        !isDarkMode -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(16.dp)
                )
                Icon(
                    imageVector = Icons.Filled.DarkMode,
                    contentDescription = null,
                    tint = when {
                        onBlueHeader && isDarkMode -> DarkPrimaryBlue
                        onBlueHeader -> Color.White.copy(alpha = 0.55f)
                        isDarkMode -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(16.dp)
                )
            }
            Surface(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(26.dp),
                shape = CircleShape,
                color = thumbColor,
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                        contentDescription = if (isDarkMode) "Switch to light mode" else "Switch to dark mode",
                        tint = thumbIconColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EnterpriseBrandMark(
    modifier: Modifier = Modifier,
    sizeDp: Int = 40
) {
    val cornerRadius = (sizeDp * 0.29f).dp
    Surface(
        modifier = modifier.size(sizeDp.dp),
        shape = RoundedCornerShape(cornerRadius),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    BrandLogoGold.copy(alpha = 0.72f),
                    Color.White.copy(alpha = 0.32f)
                )
            )
        ),
        shadowElevation = 4.dp
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_art),
            contentDescription = stringResource(R.string.app_name),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun EnterpriseCleanHeader(
    title: String,
    subtitle: String,
    eyebrow: String = "FACULTY WORKSPACE",
    conflictCount: Int = 0,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    showExportPdf: Boolean = false,
    onExportPdfClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    applyStatusBarPadding: Boolean = false
) {
    val breathTransition = rememberInfiniteTransition(label = "blue_header_breath")
    val auroraDrift by breathTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blue_header_drift"
    )
    val dotPulse by breathTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blue_header_dot_pulse"
    )

    val baseBlueGradient = Brush.linearGradient(
        colors = listOf(
            BrandLogoNavy,
            OxfordNavy,
            BrandLogoNavyLight
        )
    )
    val bottomShape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
    val bottomShimmerLine = Brush.horizontalGradient(
        colors = listOf(
            Color.Transparent,
            Color.White.copy(alpha = 0.16f),
            BrandLogoGold.copy(alpha = 0.42f),
            Color.White.copy(alpha = 0.16f),
            Color.Transparent
        )
    )
    val statusDotColor = if (conflictCount > 0) DarkCrimson else DarkTeal

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = bottomShape,
        shadowElevation = 6.dp,
        color = BrandLogoNavy
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(baseBlueGradient)
                .testTag("enterprise_clean_header")
        ) {
            // Calm, non-chaotic breathing blue atmospheric sheen across the header canvas
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val primaryGlowCenter = Offset(
                    x = w * (0.22f + 0.56f * auroraDrift),
                    y = h * 0.28f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            DarkPrimaryBlue.copy(alpha = 0.22f),
                            OxfordNavyLight.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = primaryGlowCenter,
                        radius = w * 0.52f
                    ),
                    radius = w * 0.52f,
                    center = primaryGlowCenter
                )

                val warmAccentCenter = Offset(
                    x = w * (0.82f - 0.35f * auroraDrift),
                    y = h * 0.85f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            BrandLogoGold.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = warmAccentCenter,
                        radius = w * 0.34f
                    ),
                    radius = w * 0.34f,
                    center = warmAccentCenter
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (applyStatusBarPadding) Modifier.windowInsetsPadding(WindowInsets.statusBars)
                        else Modifier
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.16f * dotPulse),
                                                Color.Transparent
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                            )
                            EnterpriseBrandMark(sizeDp = 42)
                        }

                        AnimatedContent(
                            targetState = eyebrow to title,
                            transitionSpec = {
                                (fadeIn(tween(240)) + slideInVertically(tween(240)) { it / 5 })
                                    .togetherWith(
                                        fadeOut(tween(160)) + slideOutVertically(tween(160)) { -it / 5 }
                                    )
                            },
                            label = "blue_header_title_transition"
                        ) { (animatedEyebrow, animatedTitle) ->
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .alpha(dotPulse)
                                            .clip(CircleShape)
                                            .background(statusDotColor)
                                    )
                                    Text(
                                        text = animatedEyebrow.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            letterSpacing = 1.15.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = Color.White.copy(alpha = 0.84f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = animatedTitle,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        letterSpacing = (-0.3).sp
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.74f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (showExportPdf) {
                            Surface(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(
                                        role = Role.Button,
                                        onClick = onExportPdfClick
                                    )
                                    .testTag("export_pdf_button"),
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.26f))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.PictureAsPdf,
                                        contentDescription = "Export weekly timetable to PDF",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        ThemeTogglePillSwitch(
                            isDarkMode = isDarkMode,
                            onToggle = onToggleTheme,
                            onBlueHeader = true
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(bottomShimmerLine)
                )
            }
        }
    }
}

@Composable
fun SectionEditorialHeader(
    eyebrow: String,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(2.5.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = eyebrow.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 1.15.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        letterSpacing = (-0.35).sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(12.dp))
                trailingContent()
            }
        }
    }
}

@Composable
fun LiquidLogoSignInOverlay(
    facultyLabel: String,
    onFillComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Prevent accidental back press while SSO authentication transition completes
    BackHandler(enabled = true) {}

    val fillProgress = remember { Animatable(0f) }
    val pulseScale = remember { Animatable(1f) }
    var hasNotifiedComplete by remember { mutableStateOf(false) }

    val waveTransition = rememberInfiniteTransition(label = "liquid_wave_transition")
    val wavePhase by waveTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1050, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "liquid_wave_phase"
    )

    LaunchedEffect(facultyLabel) {
        hasNotifiedComplete = false
        fillProgress.snapTo(0f)
        fillProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1650, easing = FastOutSlowInEasing)
        )
        pulseScale.animateTo(
            targetValue = 1.045f,
            animationSpec = tween(durationMillis = 130, easing = FastOutSlowInEasing)
        )
        pulseScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 130, easing = FastOutSlowInEasing)
        )
        delay(80)
        if (!hasNotifiedComplete) {
            hasNotifiedComplete = true
            onFillComplete()
        }
    }

    val progressPercent = (fillProgress.value * 100f).roundToInt().coerceIn(0, 100)
    val statusCaption = when {
        fillProgress.value < 0.36f -> "Verifying Institutional SSO"
        fillProgress.value < 0.78f -> "Syncing Teaching Timetable"
        else -> "Entering Faculty Workspace"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .testTag("liquid_logo_signin_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Pure white background emblem filled bottom-to-top with the exact app logo deep navy blue (#0C243C)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.scale(pulseScale.value)
            ) {
                Box(
                    modifier = Modifier
                        .size(168.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    BrandLogoNavy.copy(alpha = 0.12f + 0.16f * fillProgress.value),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                Surface(
                    modifier = Modifier.size(136.dp),
                    shape = RoundedCornerShape(34.dp),
                    color = Color.White,
                    shadowElevation = 14.dp,
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = BrandLogoNavy.copy(alpha = 0.14f + 0.35f * fillProgress.value)
                    )
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val p = fillProgress.value.coerceIn(0f, 1f)

                            // 1. Etched faint emblem silhouette on empty white background
                            drawLogoMonogram(
                                primaryColor = BrandLogoNavy.copy(alpha = 0.16f),
                                goldColor = BrandLogoGold.copy(alpha = 0.25f)
                            )

                            // 2. Rising dual-wave liquid in the exact app logo blue (#0C243C)
                            if (p > 0.005f) {
                                val baseWaterY = h * (1f - p)
                                val amplitude = (h * 0.042f) * (1f - p * 0.85f)

                                val backWave = Path().apply {
                                    moveTo(0f, h)
                                    lineTo(0f, baseWaterY)
                                    val steps = 28
                                    for (i in 0..steps) {
                                        val x = w * (i / steps.toFloat())
                                        val angle = (x / w) * (2f * PI.toFloat()) - wavePhase + 1.2f
                                        val y = (baseWaterY + sin(angle) * amplitude * 0.85f)
                                            .coerceIn(-4f, h + 4f)
                                        lineTo(x, y)
                                    }
                                    lineTo(w, h)
                                    close()
                                }
                                drawPath(
                                    path = backWave,
                                    color = BrandLogoNavyLight.copy(alpha = 0.58f)
                                )

                                val frontWave = Path().apply {
                                    moveTo(0f, h)
                                    lineTo(0f, baseWaterY)
                                    val steps = 32
                                    for (i in 0..steps) {
                                        val x = w * (i / steps.toFloat())
                                        val angle = (x / w) * (2.2f * PI.toFloat()) + wavePhase
                                        val y = (baseWaterY + sin(angle) * amplitude)
                                            .coerceIn(-4f, h + 4f)
                                        lineTo(x, y)
                                    }
                                    lineTo(w, h)
                                    close()
                                }
                                drawPath(
                                    path = frontWave,
                                    color = BrandLogoNavy
                                )

                                // 3. Crisp white & champagne-gold logo monogram revealed inside the rising blue liquid
                                clipPath(frontWave) {
                                    drawLogoMonogram(
                                        primaryColor = Color.White,
                                        goldColor = BrandLogoGold
                                    )
                                }
                            }
                        }

                        // Subtle crossfade to full raster emblem once liquid finishes filling
                        if (fillProgress.value > 0.96f) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher_art),
                                contentDescription = stringResource(R.string.app_name),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(((fillProgress.value - 0.96f) / 0.04f).coerceIn(0f, 1f))
                            )
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Text(
                        text = "$progressPercent%  ·  $facultyLabel",
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Text(
                    text = statusCaption,
                    style = MaterialTheme.typography.titleMedium.copy(
                        letterSpacing = (-0.2).sp
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

private fun DrawScope.drawLogoMonogram(
    primaryColor: Color,
    goldColor: Color
) {
    val w = size.width
    val h = size.height
    val left = w * 0.285f
    val right = w * 0.715f
    val top = h * 0.25f
    val bottom = h * 0.75f
    val width = right - left
    val height = bottom - top

    val colGap = width * 0.06f
    val colW = (width - colGap * 2f) / 3f
    val c1X = left
    val c2X = left + colW + colGap
    val c3X = left + (colW + colGap) * 2f

    val rowH = height * 0.138f
    val rowGap = height * 0.032f
    val r3Y = bottom - rowH
    val r2Y = r3Y - rowGap - rowH
    val r1Y = r2Y - rowGap - rowH * 1.18f
    val r1H = rowH * 1.18f

    // Bottom row: 3 crisp timetable tiles
    drawRoundRect(
        color = primaryColor,
        topLeft = Offset(c1X, r3Y),
        size = Size(colW, rowH),
        cornerRadius = CornerRadius(2f, 2f)
    )
    drawRoundRect(
        color = primaryColor,
        topLeft = Offset(c2X, r3Y),
        size = Size(colW, rowH),
        cornerRadius = CornerRadius(2f, 2f)
    )
    drawRoundRect(
        color = primaryColor,
        topLeft = Offset(c3X, r3Y),
        size = Size(colW, rowH),
        cornerRadius = CornerRadius(2f, 2f)
    )

    // Middle row: 3 timetable tiles
    drawRoundRect(
        color = primaryColor,
        topLeft = Offset(c1X, r2Y),
        size = Size(colW, rowH),
        cornerRadius = CornerRadius(2f, 2f)
    )
    drawRoundRect(
        color = primaryColor,
        topLeft = Offset(c2X, r2Y),
        size = Size(colW, rowH),
        cornerRadius = CornerRadius(2f, 2f)
    )
    drawRoundRect(
        color = primaryColor,
        topLeft = Offset(c3X, r2Y),
        size = Size(colW, rowH),
        cornerRadius = CornerRadius(2f, 2f)
    )

    // Highlighted gold scheduling bridge across columns 2 & 3
    val notchW = colGap * 0.65f
    val goldPath = Path().apply {
        moveTo(c2X, r1Y)
        lineTo(c2X + colW - notchW * 0.15f, r1Y)
        lineTo(c2X + colW - notchW * 0.15f, r1Y + r1H * 0.42f)
        lineTo(c3X + notchW * 0.15f, r1Y + r1H * 0.42f)
        lineTo(c3X + notchW * 0.15f, r1Y)
        lineTo(right, r1Y)
        lineTo(right, r1Y + r1H)
        lineTo(c2X, r1Y + r1H)
        close()
    }
    drawPath(path = goldPath, color = goldColor, style = Fill)

    // Sweeping architectural arch + top vertical pillar
    val strokeW = width * 0.062f
    val archYBase = r1Y + r1H - strokeW * 0.48f
    val archPath = Path().apply {
        moveTo(c1X + strokeW * 0.45f, archYBase)
        cubicTo(
            c1X + width * 0.13f, top + height * 0.16f,
            c1X + width * 0.48f, top + strokeW * 0.4f,
            right - strokeW * 0.48f, top + strokeW * 0.4f
        )
        lineTo(right - strokeW * 0.48f, r1Y - rowGap * 1.15f)
    }
    drawPath(
        path = archPath,
        color = primaryColor,
        style = Stroke(
            width = strokeW,
            cap = StrokeCap.Square,
            join = StrokeJoin.Miter
        )
    )

    // Inner secondary arch rib + horizontal arch base
    val innerRibPath = Path().apply {
        moveTo(c1X + strokeW * 0.45f, archYBase)
        lineTo(c1X + colW * 0.96f, archYBase)
        cubicTo(
            c1X + width * 0.40f, top + height * 0.22f,
            c1X + width * 0.60f, top + strokeW * 0.8f,
            right - strokeW * 1.4f, top + strokeW * 0.45f
        )
    }
    drawPath(
        path = innerRibPath,
        color = primaryColor,
        style = Stroke(
            width = strokeW * 0.82f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

data class AnimatedNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0
)

@Composable
fun ModernAnimatedNavBar(
    items: List<AnimatedNavItem>,
    currentRoute: String,
    onSelectRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val selected = currentRoute == item.route
                    val pillColor by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primary
                        else Color.Transparent,
                        animationSpec = tween(durationMillis = 220),
                        label = "nav_pill_color"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(durationMillis = 220),
                        label = "nav_content_color"
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.08f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "nav_icon_scale"
                    )
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(20.dp))
                            .background(pillColor)
                            .clickable(
                                role = Role.Tab,
                                onClick = { onSelectRoute(item.route) }
                            )
                            .padding(
                                horizontal = if (selected) 16.dp else 12.dp,
                                vertical = 10.dp
                            )
                            .testTag("nav_${item.route}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BadgedBox(
                                badge = {
                                    if (item.badgeCount > 0) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError
                                        ) {
                                            Text(item.badgeCount.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                    tint = contentColor,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .scale(iconScale)
                                )
                            }

                            AnimatedVisibility(
                                visible = selected,
                                enter = fadeIn(tween(180)) + expandHorizontally(
                                    spring(dampingRatio = Spring.DampingRatioNoBouncy)
                                ),
                                exit = fadeOut(tween(120)) + shrinkHorizontally()
                            ) {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = contentColor,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun sessionAccentColor(type: SessionType): Color = when (type) {
    SessionType.LECTURE -> LectureAccent
    SessionType.LAB -> LabAccent
    SessionType.SEMINAR -> SeminarAccent
    SessionType.OFFICE_HOURS -> OfficeHoursAccent
}

@Composable
fun SessionTypeBadge(sessionType: String, modifier: Modifier = Modifier) {
    val type = SessionType.fromString(sessionType)
    val accent = sessionAccentColor(type)
    val icon = when (type) {
        SessionType.LECTURE -> Icons.Filled.School
        SessionType.LAB -> Icons.Filled.Science
        SessionType.SEMINAR -> Icons.Filled.Groups
        SessionType.OFFICE_HOURS -> Icons.Filled.Schedule
    }
    Surface(
        modifier = modifier,
        color = accent.copy(alpha = 0.14f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = type.label,
                tint = accent,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = type.shortLabel,
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SemesterSelectorStrip(
    semesters: List<Semester>,
    selectedSemesterId: String?,
    onSelectSemester: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        semesters.forEach { semester ->
            val selected = semester.id == selectedSemesterId
            val status = SemesterStatus.fromString(semester.status)
            val statusColor = when (status) {
                SemesterStatus.ACTIVE -> MaterialTheme.colorScheme.secondary
                SemesterStatus.DRAFT -> MaterialTheme.colorScheme.tertiary
                SemesterStatus.HISTORICAL -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            FilterChip(
                selected = selected,
                onClick = { onSelectSemester(semester.id) },
                modifier = Modifier.testTag("semester_chip_${semester.id}"),
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = "${semester.name} · ${status.label}",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
fun ConflictStatusBanner(
    conflicts: List<ScheduleConflict>,
    balanceScore: Int,
    weeklyHours: Double,
    targetHours: Int,
    onAutoResolveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val severeCount = conflicts.count { it.type.isSevere }
    val hasConflicts = conflicts.isNotEmpty()

    val containerColor = when {
        severeCount > 0 -> MaterialTheme.colorScheme.errorContainer
        hasConflicts -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when {
        severeCount > 0 -> MaterialTheme.colorScheme.onErrorContainer
        hasConflicts -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (hasConflicts) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                    contentDescription = if (hasConflicts) "Scheduling conflicts detected" else "Schedule conflict-free",
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (hasConflicts) {
                            "${conflicts.size} Scheduling ${if (conflicts.size == 1) "Issue" else "Issues"} Detected"
                        } else {
                            "Conflict-Free Schedule"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = contentColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (hasConflicts) {
                            conflicts.first().title + " · ${String.format("%.1f", weeklyHours)}h/${targetHours}h"
                        } else {
                            "Balance $balanceScore/100 · ${String.format("%.1f", weeklyHours)}h / ${targetHours}h weekly target"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.9f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (hasConflicts) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onAutoResolveClick,
                    modifier = Modifier.testTag("auto_resolve_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoFixHigh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_auto_resolve),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
fun SampleAiNoticeBanner(
    customNotice: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = "Sample AI mode notice",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = stringResource(R.string.ai_sample_notice_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = customNotice ?: stringResource(R.string.ai_sample_notice_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyScheduleStateCard(
    onPrepareSemesterClick: () -> Unit,
    onAddManualClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_schedule),
                contentDescription = "Empty teaching timetable illustration",
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(150.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
            Text(
                text = stringResource(R.string.empty_schedule_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.empty_schedule_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onAddManualClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("empty_add_session_button")
                ) {
                    Text(stringResource(R.string.action_add_session))
                }
                Button(
                    onClick = onPrepareSemesterClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("empty_auto_prepare_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoFixHigh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Auto-Prepare")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SessionEditorBottomSheet(
    initialSession: CourseSession?,
    existingFacultySessions: List<CourseSession> = emptyList(),
    allSemesterSessions: List<CourseSession> = emptyList(),
    semesterId: String = "",
    facultyId: String = "",
    onDismiss: () -> Unit,
    onSave: (
        existingId: String?,
        courseCode: String,
        courseTitle: String,
        section: String,
        sessionType: String,
        dayOfWeek: Int,
        startMinuteOfDay: Int,
        durationMinutes: Int,
        roomCode: String,
        building: String,
        enrolledStudents: Int,
        isLocked: Boolean
    ) -> Unit,
    onDelete: ((CourseSession) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var courseCode by remember(initialSession) { mutableStateOf(initialSession?.courseCode ?: "CS-320") }
    var courseTitle by remember(initialSession) { mutableStateOf(initialSession?.courseTitle ?: "Machine Learning Foundations") }
    var section by remember(initialSession) { mutableStateOf(initialSession?.section ?: "Sec A") }
    var sessionType by remember(initialSession) { mutableStateOf(initialSession?.sessionType ?: "LECTURE") }
    var dayOfWeek by remember(initialSession) { mutableIntStateOf(initialSession?.dayOfWeek ?: 1) }
    var startMinute by remember(initialSession) { mutableIntStateOf(initialSession?.startMinuteOfDay ?: 9 * 60 + 30) }
    var durationMinutes by remember(initialSession) { mutableIntStateOf(initialSession?.durationMinutes ?: 90) }
    var roomCode by remember(initialSession) { mutableStateOf(initialSession?.roomCode ?: "Turing 204") }
    var building by remember(initialSession) { mutableStateOf(initialSession?.building ?: "Engineering West") }
    var enrolledText by remember(initialSession) { mutableStateOf((initialSession?.enrolledStudents ?: 45).toString()) }
    var isLocked by remember(initialSession) { mutableStateOf(initialSession?.isLocked ?: false) }

    val resolvedSemesterId = initialSession?.semesterId ?: semesterId
    val resolvedFacultyId = initialSession?.facultyId ?: facultyId
    val liveConflictCheck = remember(
        initialSession?.id,
        resolvedFacultyId,
        resolvedSemesterId,
        courseCode,
        dayOfWeek,
        startMinute,
        durationMinutes,
        roomCode,
        building,
        existingFacultySessions,
        allSemesterSessions
    ) {
        val candidate = CourseSession(
            id = initialSession?.id ?: "__draft_candidate__",
            facultyId = resolvedFacultyId,
            semesterId = resolvedSemesterId,
            courseCode = courseCode.trim().uppercase().ifBlank { "COURSE" },
            courseTitle = courseTitle.trim(),
            section = section.trim(),
            sessionType = sessionType,
            dayOfWeek = dayOfWeek,
            startMinuteOfDay = startMinute,
            durationMinutes = durationMinutes,
            roomCode = roomCode.trim(),
            building = building.trim(),
            enrolledStudents = enrolledText.toIntOrNull() ?: 40,
            capacity = 60,
            isLocked = isLocked,
            sourceNote = "",
            updatedAtMillis = 0L
        )
        TimetableConflictChecker.checkCandidateSession(
            candidate = candidate,
            facultySessions = existingFacultySessions,
            allSemesterSessions = allSemesterSessions
        )
    }

    val timeSlots = remember {
        listOf(8 * 60 + 30, 9 * 60 + 30, 10 * 60 + 30, 11 * 60 + 30, 13 * 60, 14 * 60, 15 * 60 + 30)
    }
    val roomPresets = remember {
        listOf(
            "Turing 204" to "Engineering West",
            "Turing 301" to "Engineering West",
            "Ada Lab 102" to "Engineering West",
            "Ada Lab 104" to "Science Commons",
            "Euler 101" to "Science Commons",
            "Hub 215" to "Humanities Hall"
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialSession == null) "Schedule New Class" else "Edit Teaching Session",
                    style = MaterialTheme.typography.headlineSmall
                )
                if (initialSession != null && onDelete != null) {
                    IconButton(
                        onClick = { onDelete(initialSession) },
                        modifier = Modifier.testTag("delete_session_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete session",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = courseCode,
                    onValueChange = { courseCode = it },
                    label = { Text("Course Code") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_course_code")
                )
                OutlinedTextField(
                    value = section,
                    onValueChange = { section = it },
                    label = { Text("Section") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(0.8f)
                        .testTag("input_section")
                )
            }

            OutlinedTextField(
                value = courseTitle,
                onValueChange = { courseTitle = it },
                label = { Text("Course Title") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_course_title")
            )

            Text("Session Type", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SessionType.entries.forEach { type ->
                    FilterChip(
                        selected = sessionType == type.name,
                        onClick = { sessionType = type.name },
                        label = { Text(type.label) }
                    )
                }
            }

            Text("Day of Week", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (1..5).forEach { day ->
                    FilterChip(
                        selected = dayOfWeek == day,
                        onClick = { dayOfWeek = day },
                        modifier = Modifier.weight(1f),
                        label = { Text(TimetableFormatters.dayShort(day)) }
                    )
                }
            }

            Text("Start Time", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                timeSlots.forEach { slot ->
                    FilterChip(
                        selected = startMinute == slot,
                        onClick = { startMinute = slot },
                        label = { Text(TimetableFormatters.formatMinuteOfDay(slot)) }
                    )
                }
            }

            Text("Duration", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(60, 90, 120).forEach { dur ->
                    FilterChip(
                        selected = durationMinutes == dur,
                        onClick = { durationMinutes = dur },
                        label = { Text("${dur}m") }
                    )
                }
            }

            Text("Campus Room & Building", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                roomPresets.forEach { (presetRoom, presetBldg) ->
                    FilterChip(
                        selected = roomCode == presetRoom,
                        onClick = {
                            roomCode = presetRoom
                            building = presetBldg
                        },
                        label = { Text("$presetRoom · ${presetBldg.substringBefore(" ")}") }
                    )
                }
            }

            OutlinedTextField(
                value = enrolledText,
                onValueChange = { enrolledText = it.filter { ch -> ch.isDigit() } },
                label = { Text("Enrolled Students") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text("Lock Session Slot", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Prevent Auto-Resolve from moving this time block",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isLocked,
                        onCheckedChange = { isLocked = it },
                        modifier = Modifier.testTag("lock_session_switch")
                    )
                }
            }

            if (liveConflictCheck.hasConflict) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_conflict_warning_card")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Conflict warning",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = liveConflictCheck.warningTitle,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = liveConflictCheck.warningMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onSave(
                            initialSession?.id,
                            courseCode,
                            courseTitle,
                            section,
                            sessionType,
                            dayOfWeek,
                            startMinute,
                            durationMinutes,
                            roomCode,
                            building,
                            enrolledText.toIntOrNull() ?: 40,
                            isLocked
                        )
                    },
                    enabled = courseCode.isNotBlank() && courseTitle.isNotBlank(),
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("save_session_button")
                ) {
                    Text(if (initialSession == null) "Add to Timetable" else "Save Changes")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfExportBottomSheet(
    semesterName: String,
    sessionCount: Int,
    suggestedFileName: String,
    isExporting: Boolean,
    onDismiss: () -> Unit,
    onSaveToDevice: () -> Unit,
    onSharePdf: () -> Unit,
    onPrintPdf: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "Export Weekly Timetable PDF",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$semesterName · $sessionCount classes · $suggestedFileName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Button(
                onClick = onSaveToDevice,
                enabled = !isExporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("pdf_save_device_button")
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save PDF to Device")
            }

            OutlinedButton(
                onClick = onSharePdf,
                enabled = !isExporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("pdf_share_button")
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share PDF")
            }

            OutlinedButton(
                onClick = onPrintPdf,
                enabled = !isExporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("pdf_print_button")
            ) {
                Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Print Weekly Schedule")
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Close")
            }
        }
    }
}
