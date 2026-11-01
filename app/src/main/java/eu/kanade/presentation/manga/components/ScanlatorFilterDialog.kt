package eu.kanade.presentation.manga.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import eu.kanade.domain.chapter.model.CustomScanlator
import eu.kanade.domain.chapter.model.customScanlatorKeywords
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.CheckBoxOutlineBlank
import mihon.icons.materialsymbols.rounded.Close
import mihon.icons.materialsymbols.roundedfilled.DisabledByDefault
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.TextButton
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

/**
 * @param customScanlators the chapter name keywords of the locally created scanlators of the manga.
 * @param onConfirm receives the excluded scanlators (custom ones encoded with [CustomScanlator.encode]) and the
 * chapter name keywords of the locally created scanlators.
 */
@Composable
fun ScanlatorFilterDialog(
    availableScanlators: Set<String>,
    excludedScanlators: Set<String>,
    customScanlators: Set<String>,
    onDismissRequest: () -> Unit,
    onConfirm: (excluded: Set<String>, custom: Set<String>) -> Unit,
) {
    val sortedAvailableScanlators = remember(availableScanlators) {
        availableScanlators.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
    }
    val mutableExcludedScanlators = remember(excludedScanlators) { excludedScanlators.toMutableStateList() }
    val mutableCustomScanlators = remember(customScanlators, excludedScanlators) {
        // An excluded rule always stays in the list, even if the stored keywords were lost
        (customScanlators + excludedScanlators.customScanlatorKeywords())
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
            .toMutableStateList()
    }
    var showAddDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(MR.strings.exclude_scanlators)) },
        text = {
            Box {
                val state = rememberLazyListState()
                LazyColumn(state = state) {
                    if (sortedAvailableScanlators.isEmpty() && mutableCustomScanlators.isEmpty()) {
                        item {
                            Text(text = stringResource(MR.strings.no_scanlators_found))
                        }
                    }
                    sortedAvailableScanlators.forEach { scanlator ->
                        item(key = "scanlator-$scanlator") {
                            val isExcluded = mutableExcludedScanlators.contains(scanlator)
                            ScanlatorRow(
                                text = scanlator,
                                isExcluded = isExcluded,
                                onClick = {
                                    if (isExcluded) {
                                        mutableExcludedScanlators.remove(scanlator)
                                    } else {
                                        mutableExcludedScanlators.add(scanlator)
                                    }
                                },
                            )
                        }
                    }
                    mutableCustomScanlators.forEach { keyword ->
                        item(key = "custom-$keyword") {
                            val encoded = CustomScanlator.encode(keyword)
                            val isExcluded = mutableExcludedScanlators.contains(encoded)
                            ScanlatorRow(
                                text = "Chapter name contains \"$keyword\"",
                                isExcluded = isExcluded,
                                onClick = {
                                    if (isExcluded) {
                                        mutableExcludedScanlators.remove(encoded)
                                    } else {
                                        mutableExcludedScanlators.add(encoded)
                                    }
                                },
                                onRemove = {
                                    mutableExcludedScanlators.remove(encoded)
                                    mutableCustomScanlators.remove(keyword)
                                },
                            )
                        }
                    }
                }
                if (state.canScrollBackward) HorizontalDivider(modifier = Modifier.align(Alignment.TopCenter))
                if (state.canScrollForward) HorizontalDivider(modifier = Modifier.align(Alignment.BottomCenter))
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = true,
        ),
        confirmButton = {
            FlowRow {
                TextButton(onClick = { showAddDialog = true }) {
                    Text(text = stringResource(MR.strings.action_add))
                }
                if (sortedAvailableScanlators.isNotEmpty() || mutableCustomScanlators.isNotEmpty()) {
                    if (mutableExcludedScanlators.isEmpty()) {
                        TextButton(
                            onClick = {
                                mutableExcludedScanlators.addAll(availableScanlators)
                                mutableExcludedScanlators.addAll(
                                    mutableCustomScanlators.map(CustomScanlator::encode),
                                )
                            },
                        ) {
                            Text(text = stringResource(MR.strings.action_select_all))
                        }
                    } else {
                        TextButton(onClick = mutableExcludedScanlators::clear) {
                            Text(text = stringResource(MR.strings.action_reset))
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
                TextButton(
                    onClick = {
                        onConfirm(mutableExcludedScanlators.toSet(), mutableCustomScanlators.toSet())
                        onDismissRequest()
                    },
                ) {
                    Text(text = stringResource(MR.strings.action_ok))
                }
            }
        },
    )

    if (showAddDialog) {
        AddCustomScanlatorDialog(
            existingKeywords = mutableCustomScanlators,
            onDismissRequest = { showAddDialog = false },
            onAdd = { keyword ->
                mutableCustomScanlators.add(keyword)
                // Created to be excluded, the checkbox can be cleared afterwards
                mutableExcludedScanlators.add(CustomScanlator.encode(keyword))
            },
        )
    }
}

@Composable
private fun ScanlatorRow(
    text: String,
    isExcluded: Boolean,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .clip(MaterialTheme.shapes.small)
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.padding.small),
    ) {
        Icon(
            imageVector = if (isExcluded) {
                MaterialSymbols.RoundedFilled.DisabledByDefault
            } else {
                MaterialSymbols.Rounded.CheckBoxOutlineBlank
            },
            tint = if (isExcluded) {
                MaterialTheme.colorScheme.primary
            } else {
                LocalContentColor.current
            },
            contentDescription = null,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(start = 24.dp),
        )
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Close,
                    contentDescription = stringResource(MR.strings.action_delete),
                )
            }
        }
    }
}

@Composable
private fun AddCustomScanlatorDialog(
    existingKeywords: List<String>,
    onDismissRequest: () -> Unit,
    onAdd: (String) -> Unit,
) {
    var keyword by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val trimmed = keyword.trim()
    val alreadyExists = existingKeywords.any { it.equals(trimmed, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = "Add chapter name scanlator") },
        text = {
            OutlinedTextField(
                modifier = Modifier.focusRequester(focusRequester),
                value = keyword,
                onValueChange = { keyword = it },
                label = { Text(text = "Chapter name contains") },
                supportingText = {
                    Text(
                        text = if (alreadyExists) {
                            "Already added"
                        } else {
                            "Not case sensitive, e.g. raw"
                        },
                    )
                },
                isError = alreadyExists,
                singleLine = true,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        confirmButton = {
            TextButton(
                enabled = trimmed.isNotEmpty() && !alreadyExists,
                onClick = {
                    onAdd(trimmed)
                    onDismissRequest()
                },
            ) {
                Text(text = stringResource(MR.strings.action_add))
            }
        },
    )

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }
}
