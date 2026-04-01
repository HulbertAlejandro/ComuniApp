package com.miempresa.comuniapp.core.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenu(
    value: String,
    label: String,
    supportingText: String? = null,
    list: List<String>,
    icon: ImageVector? = null,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true
) {

    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { expanded = !expanded }
    ) {

        OutlinedTextField(
            enabled = enabled,
            readOnly = true,
            value = value,
            onValueChange = { },
            label = {
                Text(
                    text = label
                )
            },
            supportingText = supportingText?.let {
                { Text(text = supportingText) }
            },
            leadingIcon = icon?.let {
                { Icon(imageVector = icon, contentDescription = null) }
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false }
        ) {
            list.forEach {
                DropdownMenuItem(
                    text = {
                        Text(text = it)
                    },
                    onClick = {
                        onValueChange(it)
                        expanded = false
                    }
                )
            }
        }

    }
}
