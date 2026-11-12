package com.karyar.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karyar.app.ui.theme.AmberAccent
import com.karyar.app.ui.theme.CyanAccent
import com.karyar.app.ui.theme.EmeraldAccent
import com.karyar.app.ui.theme.IndigoAccent
import com.karyar.app.ui.theme.RoseAccent
import com.karyar.app.ui.theme.Slate100
import com.karyar.app.ui.theme.Slate200
import com.karyar.app.util.BackupUtil
import com.karyar.app.util.Formatters
import com.karyar.app.util.LocalBackupFileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun BackupDataDialog(
    onDismiss: () -> Unit,
    onSaveToDeviceFile: () -> Unit,
    onShareBackupFile: () -> Unit,
    onSaveInternalBackup: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Backup,
                    contentDescription = null,
                    tint = EmeraldAccent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "پشتیبان‌گیری از کل اطلاعات",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.testTag("backup_dialog_title")
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "از تمامی اطلاعات برنامه شامل پروژه‌ها، روزهای کاری، مشخصات کارگران، سوابق تردد و هزینه‌ها یک فایل پشتیبان کامل تهیه می‌شود.",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                Button(
                    onClick = {
                        onDismiss()
                        onSaveToDeviceFile()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("backup_save_to_device_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ذخیره فایل پشتیبان در حافظه گوشی",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = {
                        onDismiss()
                        onShareBackupFile()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("backup_share_file_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ساخت و اشتراک‌گذاری فایل پشتیبان",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onSaveInternalBackup()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("backup_save_internal_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = null,
                        tint = AmberAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ذخیره سریع در لیست پشتیبان‌های برنامه",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("backup_dialog_close_button")
            ) {
                Text("بستن")
            }
        }
    )
}

@Composable
fun RestoreBackupDialog(
    onDismiss: () -> Unit,
    onPickFileFromDevice: () -> Unit,
    onRestoreLocalFile: (File) -> Unit
) {
    val context = LocalContext.current
    var localBackups by remember { mutableStateOf<List<LocalBackupFileInfo>>(emptyList()) }
    var pendingConfirmFile by remember { mutableStateOf<LocalBackupFileInfo?>(null) }
    var pendingDeleteFile by remember { mutableStateOf<LocalBackupFileInfo?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        localBackups = withContext(Dispatchers.IO) {
            BackupUtil.listLocalBackups(context)
        }
    }

    if (pendingDeleteFile != null) {
        val target = pendingDeleteFile!!
        AlertDialog(
            onDismissRequest = { pendingDeleteFile = null },
            title = {
                Text(
                    text = "تأیید حذف فایل پشتیبان",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp
                )
            },
            text = {
                Text(
                    text = "آیا از حذف فایل پشتیبان «${target.fileName}» از حافظه برنامه اطمینان دارید؟ این عملیات قابل بازگشت نیست.",
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        target.file.delete()
                        pendingDeleteFile = null
                        refreshTrigger++
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseAccent),
                    modifier = Modifier.testTag("confirm_delete_local_backup_button")
                ) {
                    Text("بله، حذف شود", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteFile = null }) {
                    Text("انصراف")
                }
            }
        )
        return
    }

    if (pendingConfirmFile != null) {
        val target = pendingConfirmFile!!
        AlertDialog(
            onDismissRequest = { pendingConfirmFile = null },
            title = {
                Text(
                    text = "تأیید بازیابی پشتیبان",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp
                )
            },
            text = {
                Text(
                    text = "با بازیابی فایل «${target.fileName}»، تمامی اطلاعات فعلی برنامه با اطلاعات داخل این نسخه پشتیبان (${Formatters.toPersianDigits(target.foldersCount)} پروژه و ${Formatters.toPersianDigits(target.workersCount)} کارگر) جایگزین خواهد شد. آیا مطمئن هستید؟",
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fileToRestore = target.file
                        pendingConfirmFile = null
                        onDismiss()
                        onRestoreLocalFile(fileToRestore)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                    modifier = Modifier.testTag("confirm_restore_local_backup_button")
                ) {
                    Text("بله، بازیابی شود", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingConfirmFile = null }) {
                    Text("انصراف")
                }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = null,
                    tint = AmberAccent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "بازیابی پشتیبان اطلاعات",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.testTag("restore_dialog_title")
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = RoseAccent.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "توجه: با بازیابی نسخه پشتیبان، اطلاعات فعلی برنامه با اطلاعات فایل پشتیبان جایگزین می‌شود.",
                        fontSize = 11.5.sp,
                        color = RoseAccent,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(10.dp),
                        lineHeight = 18.sp
                    )
                }

                Button(
                    onClick = {
                        onDismiss()
                        onPickFileFromDevice()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("restore_pick_file_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "انتخاب فایل پشتیبان از حافظه گوشی",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "پشتیبان‌های ذخیره‌شده در برنامه:",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (localBackups.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Slate100, RoundedCornerShape(10.dp))
                            .border(1.dp, Slate200, RoundedCornerShape(10.dp))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "هنوز نسخه پشتیبانی در حافظه داخلی برنامه ذخیره نشده است.",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(localBackups, key = { it.fileName }) { item ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Slate100,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = null,
                                            tint = AmberAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "پشتیبان مورخ ${Formatters.toPersianDigits(item.createdAtJalali)}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${Formatters.toPersianDigits(item.foldersCount)} پروژه | ${Formatters.toPersianDigits(item.workersCount)} کارگر | ${Formatters.toPersianDigits(item.attendancesCount)} تردد",
                                                fontSize = 10.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Button(
                                            onClick = { pendingConfirmFile = item },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .height(34.dp)
                                                .testTag("restore_local_item_${item.fileName}")
                                        ) {
                                            Text("بازیابی", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        IconButton(
                                            onClick = {
                                                pendingDeleteFile = item
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .testTag("delete_local_backup_${item.fileName}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "حذف فایل پشتیبان",
                                                tint = RoseAccent,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("restore_dialog_close_button")
            ) {
                Text("بستن")
            }
        }
    )
}
