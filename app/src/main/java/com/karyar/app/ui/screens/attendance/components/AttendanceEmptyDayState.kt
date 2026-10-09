package com.karyar.app.ui.screens.attendance.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karyar.app.ui.components.HairlineCard
import com.karyar.app.ui.components.IconicsBox
import com.karyar.app.ui.components.IconicsSize
import com.karyar.app.ui.components.LoadingButton
import com.karyar.app.ui.theme.AmberAccent

@Composable
fun AttendanceEmptyDayState(
    onCreateFirstDay: () -> Unit,
    modifier: Modifier = Modifier
) {
    HairlineCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconicsBox(
                icon = Icons.Default.CalendarMonth,
                color = AmberAccent,
                size = IconicsSize.HERO
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "هیچ روز کاری فعالی وجود ندارد",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "برای ثبت ورود و خروج، محاسبه ساعات کاری و حضور و غیاب پرسنل، لطفاً ابتدا روز کاری مورد نظر خود را ایجاد کنید (می‌توانید از هر تاریخی شروع نمایید).",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            LoadingButton(
                text = "ایجاد اولین روز کاری",
                icon = Icons.Default.Add,
                onClick = onCreateFirstDay,
                containerColor = AmberAccent,
                height = 40.dp
            )
        }
    }
}
