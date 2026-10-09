package com.karyar.app.ui.screens.folders

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karyar.app.data.local.entity.WorkplaceFolderEntity
import com.karyar.app.ui.WorkerViewModel
import com.karyar.app.ui.components.HairlineCard
import com.karyar.app.ui.components.IconicsBox
import com.karyar.app.ui.components.IconicsSize
import com.karyar.app.ui.components.LoadingButton
import com.karyar.app.ui.components.LoadingOutlinedButton
import androidx.compose.material.icons.filled.Delete
import com.karyar.app.ui.theme.AmberAccent
import com.karyar.app.ui.theme.CyanAccent
import com.karyar.app.ui.theme.EmeraldAccent
import com.karyar.app.ui.theme.RoseAccent
import com.karyar.app.ui.theme.Slate100

@Composable
fun FoldersScreen(
    viewModel: WorkerViewModel,
    onFolderSelected: (WorkplaceFolderEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val folders by viewModel.allFolders.collectAsState()

    var isCreatingFolder by remember { mutableStateOf(false) }
    var editingFolder by remember { mutableStateOf<WorkplaceFolderEntity?>(null) }
    var deletingFolder by remember { mutableStateOf<WorkplaceFolderEntity?>(null) }
    var showConfirmClearAll by remember { mutableStateOf(false) }
    var showSampleConfirm by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp)
        ) {
            // Empty State
            if (folders.isEmpty()) {
                item {
                    HairlineCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        shape = RoundedCornerShape(18.dp),
                        backgroundColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            IconicsBox(
                                icon = Icons.Default.Folder,
                                color = AmberAccent,
                                size = IconicsSize.HERO
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "هیچ پروژه‌ای وجود ندارد",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "یک پروژه جدید بسازید یا داده نمونه بارگذاری کنید.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                LoadingButton(
                                    text = "ساخت اولین پروژه",
                                    icon = Icons.Default.Add,
                                    onClick = { isCreatingFolder = true },
                                    containerColor = AmberAccent,
                                    modifier = Modifier.weight(1f),
                                    height = 42.dp,
                                    testTag = "create_first_folder_button"
                                )

                                LoadingButton(
                                    text = "بارگذاری داده نمونه",
                                    icon = Icons.Default.PlaylistAdd,
                                    onClick = { showSampleConfirm = true },
                                    containerColor = EmeraldAccent,
                                    modifier = Modifier.weight(1f),
                                    height = 42.dp,
                                    testTag = "load_sample_data_button"
                                )
                            }
                        }
                    }
                }
            }

            // Folders List
            items(folders, key = { it.id }) { folder ->
                FolderCardItem(
                    folder = folder,
                    onOpen = { onFolderSelected(folder) },
                    onEdit = { editingFolder = folder },
                    onDuplicate = { viewModel.duplicateFolder(folder) },
                    onDelete = { deletingFolder = folder }
                )
            }
        }

        // Floating Action Button to create a folder
        FloatingActionButton(
            onClick = { isCreatingFolder = true },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
                .testTag("add_folder_fab"),
            containerColor = AmberAccent,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "ایجاد پوشه محل کار")
        }
    }

    // Create Folder Dialog
    if (isCreatingFolder) {
        AddEditFolderDialog(
            initialFolder = null,
            onDismiss = { isCreatingFolder = false },
            onConfirm = { name, foreman, employer, notes, color ->
                viewModel.createFolder(name, foreman, employer, notes, color)
                isCreatingFolder = false
            }
        )
    }

    // Edit Folder Dialog
    if (editingFolder != null) {
        AddEditFolderDialog(
            initialFolder = editingFolder,
            onDismiss = { editingFolder = null },
            onConfirm = { name, foreman, employer, notes, color ->
                viewModel.updateFolder(
                    editingFolder!!.copy(
                        name = name,
                        foremanName = foreman,
                        employerName = employer,
                        notes = notes,
                        colorTag = color
                    )
                )
                editingFolder = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (deletingFolder != null) {
        AlertDialog(
            onDismissRequest = { deletingFolder = null },
            title = { Text("حذف پوشه محل کار", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Text(
                    "آیا از حذف پوشه «${deletingFolder?.name}» و تمام کارگران، ترددها و فاکتورهای حساب و کتاب این پوشه اطمینان دارید؟ این عملیات غیرقابل بازگشت است.",
                    fontSize = 12.5.sp
                )
            },
            confirmButton = {
                LoadingButton(
                    text = "حذف کامل پوشه",
                    icon = Icons.Default.Delete,
                    onClick = {
                        deletingFolder?.let { viewModel.deleteFolder(it) }
                        deletingFolder = null
                    },
                    containerColor = RoseAccent,
                    height = 38.dp,
                    fontSize = 12.sp
                )
            },
            dismissButton = {
                LoadingOutlinedButton(
                    text = "انصراف",
                    onClick = { deletingFolder = null },
                    height = 38.dp,
                    fontSize = 12.sp
                )
            }
        )
    }

    // Clear All Data Confirmation Dialog
    if (showConfirmClearAll) {
        AlertDialog(
            onDismissRequest = { showConfirmClearAll = false },
            title = { Text("حذف تمام اطلاعات", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = { Text("آیا مطمئن هستید که می‌خواهید تمام پوشه‌ها، کارگران و ترددها را حذف کرده و برنامه را از صفر خالی کنید؟", fontSize = 12.5.sp) },
            confirmButton = {
                LoadingButton(
                    text = "حذف همه چیز",
                    icon = Icons.Default.Delete,
                    onClick = {
                        viewModel.clearAllData()
                        showConfirmClearAll = false
                    },
                    containerColor = RoseAccent,
                    height = 38.dp,
                    fontSize = 12.sp
                )
            },
            dismissButton = {
                LoadingOutlinedButton(
                    text = "انصراف",
                    onClick = { showConfirmClearAll = false },
                    height = 38.dp,
                    fontSize = 12.sp
                )
            }
        )
    }

    // Load Sample Data Confirmation Dialog
    if (showSampleConfirm) {
        AlertDialog(
            onDismissRequest = { showSampleConfirm = false },
            title = { Text("بارگذاری داده‌های نمونه", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = { Text("همه داده‌های فعلی پاک می‌شود. آیا مطمئن هستید که می‌خواهید داده‌های نمونه را بارگذاری کنید؟", fontSize = 12.5.sp) },
            confirmButton = {
                LoadingButton(
                    text = "بارگذاری داده نمونه",
                    icon = Icons.Default.PlaylistAdd,
                    onClick = {
                        viewModel.loadSampleData(clearFirst = true)
                        showSampleConfirm = false
                    },
                    containerColor = EmeraldAccent,
                    height = 38.dp,
                    fontSize = 12.sp,
                    testTag = "confirm_load_sample_data_button"
                )
            },
            dismissButton = {
                LoadingOutlinedButton(
                    text = "انصراف",
                    onClick = { showSampleConfirm = false },
                    height = 38.dp,
                    fontSize = 12.sp
                )
            }
        )
    }
}

@Composable
private fun FolderCardItem(
    folder: WorkplaceFolderEntity,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val folderColor = Color(folder.colorTag)

    HairlineCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .testTag("folder_card_${folder.id}"),
        shape = RoundedCornerShape(14.dp),
        backgroundColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top Row: Folder Icon + Workplace Name + Foreman Name UNDER Workplace
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconicsBox(
                        icon = Icons.Default.Folder,
                        color = folderColor,
                        size = IconicsSize.MEDIUM
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        // Workplace Name
                        Text(
                            text = folder.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        // FOREMAN NAME DIRECTLY UNDER WORKPLACE NAME (Mandatory user requirement)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconicsBox(
                                icon = Icons.Default.Engineering,
                                color = AmberAccent,
                                size = IconicsSize.TINY
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "سرکارگر: ${if (folder.foremanName.isNotBlank()) folder.foremanName else "تعیین نشده"}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AmberAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Action icons: Duplicate, Edit, Delete
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDuplicate, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "کپی پوشه", tint = CyanAccent, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "ویرایش و تغییر نام", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = RoseAccent, modifier = Modifier.size(17.dp))
                    }
                }
            }

            if (folder.employerName.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "کارفرما: ${folder.employerName}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (folder.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = folder.notes,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Enter folder button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Slate100)
                    .clickable(onClick = onOpen)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ورود به حساب و کتاب این کارگاه",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    Icons.Default.ArrowBack, // RTL arrow points into content
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
