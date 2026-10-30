package com.familycare.app.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.familycare.app.ui.theme.*

/**
 * Thẻ chứa thông tin chuẩn Design System Health & Wellness (DESIGN.md)
 * Rounded 12px (md), Border #E2E5E9, Elevation 1px (XS).
 */
@Composable
fun FamilyCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    containerColor: Color = HealthSurface,
    borderColor: Color = HealthBorder,
    elevation: Dp = 1.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        content = content
    )
}

/**
 * Nút hành động chính (Primary Button):
 * Chiều cao chuẩn 48dp, Bo góc 8dp (sm), Màu Teal #0D7C66, Chữ trắng rõ ràng.
 */
@Composable
fun HealthPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    containerColor: Color = HealthTealPrimary,
    contentColor: Color = Color.White
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.4f),
            disabledContentColor = contentColor.copy(alpha = 0.7f)
        ),
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .heightIn(min = 48.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Nút hành động phụ (Secondary Outlined Button):
 * Chiều cao 48dp, Bo góc 8dp (sm), Viền #CBD0D6, Chữ #1A202C.
 */
@Composable
fun HealthSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, HealthBorderStrong),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = HealthTextPrimary
        ),
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .heightIn(min = 48.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = HealthTextPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Tiêu đề mục chuẩn mực với touch target thoải mái cho người cao tuổi
 */
@Composable
fun SectionHeading(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = HealthTextPrimary
        )

        if (!actionLabel.isNullOrBlank() && onActionClick != null) {
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = actionLabel,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HealthTealPrimary
                )
            }
        }
    }
}

/**
 * Badge trạng thái sức khỏe đạt chuẩn tương phản cao
 */
enum class HealthBadgeType {
    POSITIVE,   // Ổn định, Bình thường (Xanh lá #2F855A)
    WARNING,    // Cần lưu ý, Chưa đo gần đây (Hổ phách #C05621)
    URGENT,     // Nguy cơ, Vượt ngưỡng (Đỏ #C53030)
    NEUTRAL     // Chưa có dữ liệu (Xám #4A5568)
}

@Composable
fun HealthStatusBadge(
    text: String,
    type: HealthBadgeType,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val (bgColor, textColor, borderColor) = when (type) {
        HealthBadgeType.POSITIVE -> Triple(HealthSuccessLight, HealthSuccess, HealthSuccessBorder)
        HealthBadgeType.WARNING -> Triple(HealthWarningLight, HealthWarning, HealthWarningBorder)
        HealthBadgeType.URGENT -> Triple(HealthErrorLight, HealthError, HealthErrorBorder)
        HealthBadgeType.NEUTRAL -> Triple(HealthSurfaceWarm, HealthTextSecondary, HealthBorder)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                color = textColor,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
