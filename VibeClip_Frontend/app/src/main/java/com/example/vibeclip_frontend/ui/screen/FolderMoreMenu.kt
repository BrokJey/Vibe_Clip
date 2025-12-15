package com.example.vibeclip_frontend.ui.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun FolderMoreMenu(
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onRegenerate: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "Меню папки"
        )
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        DropdownMenuItem(
            text = { Text("Открыть ленту") },
            onClick = {
                expanded = false
                onOpen()
            }
        )
        DropdownMenuItem(
            text = { Text("Редактировать") },
            onClick = {
                expanded = false
                onEdit()
            }
        )
        DropdownMenuItem(
            text = { Text("Перегенерировать ленту") },
            onClick = {
                expanded = false
                onRegenerate()
            }
        )
        DropdownMenuItem(
            text = { Text("Архивировать") },
            onClick = {
                expanded = false
                onArchive()
            }
        )
        DropdownMenuItem(
            text = { Text("Удалить") },
            onClick = {
                expanded = false
                onDelete()
            }
        )
    }
}


