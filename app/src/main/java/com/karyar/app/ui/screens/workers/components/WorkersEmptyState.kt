package com.karyar.app.ui.screens.workers.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karyar.app.ui.components.HairlineCard
import com.karyar.app.ui.components.LoadingButton
import com.karyar.app.ui.theme.AmberAccent

@Composable
fun WorkersEmptyState(
    isDateFoldersEmpty: Boolean,
    dayOfWeek: String?,
    onCreateDateFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    HairlineCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        backgroundColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.People,
                contentDescription = null,
                tint = AmberAccent,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isDateFoldersEmpty)
                    "هنوز روز کاری ثبت نشده است. ابتدا یک روز کاری ایجاد نمایید و سپس کارگران را اضافه کنید."
                else if (dayOfWeek != null)
                    "کارگری برای $dayOfWeek یافت نشد"
                else
                    "کارگری یافت نشد",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.5.sp,
                textAlign = TextAlign.Center
            )
            if (isDateFoldersEmpty) {
                Spacer(modifier = Modifier.height(12.dp))
                LoadingButton(
                    text = "ایجاد روز کاری",
                    icon = Icons.Default.Add,
                    onClick = onCreateDateFolder,
                    containerColor = AmberAccent,
                    height = 36.dp,
                    fontSize = 11.5.sp
                )
            }
        }
    }
}
