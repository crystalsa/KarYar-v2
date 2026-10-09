package com.karyar.app.ui.screens.folders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.karyar.app.data.local.entity.WorkplaceFolderEntity
import com.karyar.app.ui.components.HairlineCard
import com.karyar.app.ui.components.IconicsBox
import com.karyar.app.ui.components.IconicsSize
import com.karyar.app.ui.components.LoadingButton
import com.karyar.app.ui.components.LoadingOutlinedButton
import com.karyar.app.ui.theme.AmberAccent
import com.karyar.app.ui.theme.CyanAccent
import com.karyar.app.ui.theme.EmeraldAccent

private val FOLDER_COLORS = listOf(
    0xFFD97706L, // Amber
    0xFF0284C7L, // Blue
    0xFF059669L, // Emerald
    0xFF7C3AEDL, // Violet
    0xFFE11D48L  // Rose
)

@Composable
fun AddEditFolderDialog(
    initialFolder: WorkplaceFolderEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, foreman: String, employer: String, notes: String, color: Long) -> Unit
) {
    var name by remember { mutableStateOf(initialFolder?.name ?: "") }
    var foremanName by remember { mutableStateOf(initialFolder?.foremanName ?: "") }
    var employerName by remember { mutableStateOf(initialFolder?.employerName ?: "") }
    var notes by remember { mutableStateOf(initialFolder?.notes ?: "") }
    var selectedColor by remember { mutableLongStateOf(initialFolder?.colorTag ?: FOLDER_COLORS[0]) }

    Dialog(onDismissRequest = onDismiss) {
        HairlineCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            backgroundColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with Android-Iconics styled container
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconicsBox(
                            icon = Icons.Default.Folder,
                            color = Color(selectedColor),
                            size = IconicsSize.SMALL
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (initialFolder == null) "پوشه جدید" else "ویرایش پوشه",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Name (Workplace Name)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام کارگاه / پروژه", fontSize = 12.sp) },
                    placeholder = { Text("مثلاً برج سپهر", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Business, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("folder_name_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Foreman Name
                OutlinedTextField(
                    value = foremanName,
                    onValueChange = { foremanName = it },
                    label = { Text("نام سرکارگر", fontSize = 12.sp) },
                    placeholder = { Text("مثلاً حاج اصغر کریمی", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Engineering, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("foreman_name_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Employer Name
                OutlinedTextField(
                    value = employerName,
                    onValueChange = { employerName = it },
                    label = { Text("نام کارفرما", fontSize = 12.sp) },
                    placeholder = { Text("مثلاً مهندس سعیدی", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("توضیحات", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Folder Color
                Text(
                    text = "رنگ نشان پوشه:",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FOLDER_COLORS.forEach { colorVal ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(colorVal))
                                .clickable { selectedColor = colorVal },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == colorVal) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions using LoadingButton
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LoadingOutlinedButton(
                        text = "انصراف",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        height = 40.dp
                    )
                    LoadingButton(
                        text = if (initialFolder == null) "ایجاد" else "ذخیره",
                        icon = Icons.Default.Folder,
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(name.trim(), foremanName.trim(), employerName.trim(), notes.trim(), selectedColor)
                            }
                        },
                        modifier = Modifier.weight(1.3f),
                        containerColor = AmberAccent,
                        height = 40.dp,
                        enabled = name.isNotBlank(),
                        testTag = "submit_folder_button"
                    )
                }
            }
        }
    }
}
