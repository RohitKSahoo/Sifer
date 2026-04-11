package com.rohit.sifer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rohit.sifer.ui.theme.*

object SiferColors {
    val Green = SiferGreen
    val Black = SiferBlack
    val White = SiferWhite
    val Yellow = SiferYellow
    val Grey = SiferGrey
    val MediumGrey = SiferMediumGrey
    val TextSecondary = Color(0xFF4A4A4A) 
    val BlueBadge = SiferBlueBadge
    val LightBlue = SiferLightBlue
    val Red = SiferRed
    
    val MeshGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFE0F2F1),
            Color(0xFFF1F8E9),
            Color(0xFFFFFDE7)
        )
    )
}

@Composable
fun GridBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val gridSpacing = 20.dp.toPx()
        val strokeWidth = 0.5.dp.toPx()
        val color = Color.Black.copy(alpha = 0.05f)

        // Draw vertical lines
        var x = 0f
        while (x < size.width) {
            drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth)
            x += gridSpacing
        }

        // Draw horizontal lines
        var y = 0f
        while (y < size.height) {
            drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth)
            y += gridSpacing
        }
    }
}

@Composable
fun NeoBrutalCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = SiferColors.White,
    borderColor: Color = SiferColors.Black,
    borderWidth: Dp = 2.dp,
    shadowOffset: Dp = 6.dp,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = modifier.padding(bottom = shadowOffset, end = shadowOffset)) {
        // Shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffset, y = shadowOffset)
                .background(borderColor)
        )
        // Main Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .border(borderWidth, borderColor)
                .padding(padding)
        ) {
            content()
        }
    }
}

@Composable
fun SiferBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = SiferColors.Green,
    textColor: Color = SiferColors.Black,
    dotColor: Color? = null
) {
    Row(
        modifier = modifier
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (dotColor != null) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = text.uppercase(),
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun SiferButton(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = SiferColors.Black,
    textColor: Color = SiferColors.White,
    icon: ImageVector? = null,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .background(backgroundColor)
            .border(2.dp, SiferColors.Black)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text.uppercase(),
                color = textColor,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
            if (icon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun SiferTopBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SiferColors.White)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .drawBehind {
                drawLine(
                    color = Color.Black,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 2.dp.toPx()
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "SIFER",
            color = SiferColors.Black,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            fontSize = 28.sp,
            letterSpacing = 2.sp
        )
    }
}

@Composable
fun SiferBottomNav(
    selectedItem: Int,
    onItemSelected: (Int) -> Unit
) {
    val items = listOf(
        NavItem("Home", Icons.Outlined.Home),
        NavItem("Add", Icons.Outlined.AddLocationAlt),
        NavItem("Settings", Icons.Outlined.Settings)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SiferColors.White)
            .navigationBarsPadding()
            .drawBehind {
                drawLine(
                    color = Color.Black,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 2.dp.toPx()
                )
            }
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = index == selectedItem
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onItemSelected(index) }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = if (isSelected) SiferColors.Black else SiferColors.TextSecondary,
                        modifier = Modifier.size(32.dp)
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(3.dp)
                                .background(SiferColors.Yellow)
                        )
                    }
                }
            }
        }
    }
}

data class NavItem(val label: String, val icon: ImageVector)

@Composable
fun SiferSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp, 24.dp)
            .background(if (checked) SiferColors.Green else SiferColors.White)
            .border(2.dp, SiferColors.Black)
            .clickable { onCheckedChange(!checked) }
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .background(SiferColors.Black)
        )
    }
}

@Composable
fun DashedCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val stroke = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(bottom = 6.dp, end = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRoundRect(color = Color.Black, style = stroke)
        }
        content()
    }
}

@Composable
fun SiferSectionHeader(title: String, rightText: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text(
                text = title.uppercase(),
                color = SiferColors.Black,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(4.dp)
                    .background(SiferColors.Green)
            )
        }
        if (rightText != null) {
            Text(
                text = rightText.uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SiferColors.TextSecondary
            )
        }
    }
}
