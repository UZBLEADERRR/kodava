package uz.kodava.studio.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import uz.kodava.studio.ui.theme.Kodava
import java.io.File

/** Ilovaning umumiy karkasi: fon, xabar (snackbar) va band holat qatlami. */
@Composable
fun KodavaScreen(
    vm: AppViewModel,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbar.showSnackbar(it)
            vm.message = null
        }
    }

    Scaffold(
        containerColor = Kodava.Ink,
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(Kodava.glow)
            )
            content(padding)
            vm.busy?.let { BusyOverlay(it, vm.progress) }
        }
    }
}

/** Ichki ekranlar uchun sarlavha qatori. */
@Composable
fun TopRow(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            CircleButton(Icons.AutoMirrored.Filled.ArrowBack, "Orqaga", onBack)
            Spacer(Modifier.width(10.dp))
        } else {
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Kodava.TextMid, maxLines = 1)
            }
        }
        actions()
    }
}

@Composable
fun CircleButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Kodava.SurfaceHigh)
            .border(1.dp, Kodava.Line, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = Kodava.TextHigh, modifier = Modifier.size(20.dp))
    }
}

/** Asosiy amal tugmasi — gradient bilan. */
@Composable
fun GradientButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier
            .height(54.dp)
            .clip(RoundedCornerShape(27.dp))
            .background(if (enabled) Kodava.accent else Brush.horizontalGradient(listOf(Kodava.SurfaceHigh, Kodava.SurfaceHigh)))
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.6f)
            .padding(horizontal = 26.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(9.dp))
            }
            Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Ikkilamchi tugma — chegarali. */
@Composable
fun OutlineButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Kodava.SurfaceHigh)
            .border(1.dp, Kodava.Line, RoundedCornerShape(22.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.5f)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Kodava.TextHigh, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
            }
            Text(text, color = Kodava.TextHigh, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun KodavaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Kodava.Surface,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Kodava.Line)
    ) {
        Box(Modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)) {
            content()
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.titleSmall,
        color = Kodava.TextMid
    )
}

/** Ma'lumot yoki ogohlantirish paneli. */
@Composable
fun Banner(
    text: String,
    modifier: Modifier = Modifier,
    tone: Color = Kodava.Violet,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = tone.copy(alpha = 0.12f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, tone.copy(alpha = 0.35f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text, style = MaterialTheme.typography.bodySmall, color = Kodava.TextHigh)
            if (actionLabel != null && onAction != null) {
                Text(
                    actionLabel,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = tone,
                    modifier = Modifier.clickable(onClick = onAction)
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    emoji: String,
    title: String,
    text: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(emoji, style = MaterialTheme.typography.displaySmall)
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = Kodava.TextMid,
            textAlign = TextAlign.Center
        )
    }
}

/** Aktyor yuzi — doira ko'rinishida. */
@Composable
fun Avatar(file: File?, name: String, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Kodava.SurfaceHigh)
            .border(1.dp, Kodava.Line, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (file != null && file.exists()) {
            AsyncImage(
                model = file,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                name.trim().take(1).uppercase().ifBlank { "?" },
                color = Kodava.TextMid,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

/** Bajarilgan kadrlar ulushi. */
@Composable
fun ProgressLine(done: Int, total: Int, modifier: Modifier = Modifier) {
    val fraction = if (total == 0) 0f else done.toFloat() / total
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = Kodava.Mint,
            trackColor = Kodava.SurfaceHigh,
            gapSize = 0.dp,
            drawStopIndicator = {}
        )
        Text(
            "$done / $total kadr tayyor",
            style = MaterialTheme.typography.bodySmall,
            color = Kodava.TextMid
        )
    }
}

@Composable
private fun BusyOverlay(text: String, progress: Float) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xE60A0912)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Kodava.Surface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Kodava.Line),
            modifier = Modifier.padding(36.dp)
        ) {
            Column(
                Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(Modifier.size(40.dp), color = Kodava.Magenta)
                Text(text, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                if (progress > 0f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Kodava.Mint,
                        trackColor = Kodava.SurfaceHigh,
                        gapSize = 0.dp,
                        drawStopIndicator = {}
                    )
                }
            }
        }
    }
}
