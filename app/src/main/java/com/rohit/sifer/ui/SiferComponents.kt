package com.rohit.sifer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

object SiferColors {
    val Green = Color(0xFF46D19B)
    val Black = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)
    val LightGrey = Color(0xFFF2F2F2)
    val MediumGrey = Color(0xFFE0E0E0)
    val TextSecondary = Color(0xFF666666)
    val BlueBadge = Color(0xFFB5CCFF)
    val Yellow = Color(0xFFFDCB01)
    val LightBlue = Color(0xFFD0E0FF)
    val Red = Color(0xFFFF5252)
    
    val MeshGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFE0F2F1),
            Color(0xFFF1F8E9),
            Color(0xFFFFFDE7)
        )
    )
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
                fontSize = 12.sp
            )
            if (icon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun SiferTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SiferColors.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Menu, contentDescription = null, modifier = Modifier.size(24.dp))
        Text(
            text = "SIFER",
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            fontSize = 20.sp,
            letterSpacing = 1.sp
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .border(2.dp, SiferColors.Black, CircleShape)
        ) {
            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun SiferBottomNav(
    selectedItem: Int,
    onItemSelected: (Int) -> Unit
) {
    val items = listOf(
        NavItem("Home", Icons.Default.Home),
        NavItem("Campus", Icons.Default.Place),
        NavItem("Study", Icons.Default.Book),
        NavItem("Sifer", Icons.Default.Fingerprint)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SiferColors.White)
            .drawBehind {
                drawLine(
                    color = Color.Black,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 2.dp.toPx()
                )
            }
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = index == selectedItem
            Column(
                modifier = Modifier
                    .clickable { onItemSelected(index) }
                    .padding(8.dp)
                    .then(
                        if (isSelected) Modifier
                            .background(SiferColors.Green)
                            .border(2.dp, SiferColors.Black)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                        else Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = SiferColors.Black
                )
                Text(
                    text = item.label.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
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
            .size(44.dp, 22.dp)
            .background(if (checked) SiferColors.Green else SiferColors.White)
            .border(2.dp, SiferColors.Black)
            .clickable { onCheckedChange(!checked) }
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
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
                fontWeight = FontWeight.Black,
                fontSize = 16.sp
            )
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(3.dp)
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
