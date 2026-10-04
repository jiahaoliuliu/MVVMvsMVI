package com.jiahaoliuliu.mvvmvsmvi.presentation.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jiahaoliuliu.mvvmvsmvi.R
import com.jiahaoliuliu.mvvmvsmvi.domain.entity.Task
import com.jiahaoliuliu.mvvmvsmvi.domain.entity.TaskTitle

/** Both architectures render this exact production composable. */
@Composable
fun TasksScreen(
    state: TasksState,
    onRefresh: () -> Unit,
    onToggle: (String) -> Unit,
    onBack: () -> Unit,
) {
    val togglingLabel = stringResource(R.string.toggling)
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .safeDrawingPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.back)
                    )
                }
                Text(
                    stringResource(R.string.tasks_title),
                    style = MaterialTheme.typography.headlineLarge
                )
            }
            Text(
                stringResource(R.string.demo_hint),
                modifier = Modifier.testTag("tasks_subtitle"),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("loading_area"),
                    contentAlignment = Alignment.Center,
                ) {
                    val loading = stringResource(R.string.loading)
                    Column(
                        modifier = Modifier.testTag("loading_content"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularProgressIndicator(Modifier.semantics { stateDescription = loading })
                        Text(loading)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("tasks_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                stringResource(
                                    R.string.completed_count,
                                    state.tasks.count { it.completed },
                                    state.tasks.size
                                )
                            )
                            if (state.hasToggleError) {
                                Text(
                                    stringResource(R.string.toggle_error),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            if (state.hasError) {
                                Text(
                                    stringResource(R.string.load_error),
                                    color = MaterialTheme.colorScheme.error
                                )
                                Button(
                                    onClick = onRefresh,
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text(stringResource(R.string.retry)) }
                            }
                        }
                    }
                    items(state.tasks, key = { it.id }) { task ->
                        Card {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .toggleable(
                                        value = task.completed,
                                        enabled = !state.isLoading && !state.isToggling,
                                        role = Role.Checkbox,
                                        onValueChange = { onToggle(task.id) },
                                    )
                                    .semantics {
                                        if (state.isToggling) stateDescription = togglingLabel
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = task.completed,
                                    onCheckedChange = null,
                                    enabled = !state.isLoading && !state.isToggling,
                                    colors = if (state.isLoading) CheckboxDefaults.colors() else CheckboxDefaults.colors(
                                        disabledCheckedColor = MaterialTheme.colorScheme.primary,
                                        disabledUncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledIndeterminateColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(task.title.resourceId()), Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            Button(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading && !state.isToggling,
                colors = if (state.isLoading) ButtonDefaults.buttonColors() else ButtonDefaults.buttonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) { Text(stringResource(R.string.refresh)) }
        }
    }
}

private fun TaskTitle.resourceId(): Int = when (this) {
    TaskTitle.COMPOSE -> R.string.task_compose
    TaskTitle.ARCHITECTURES -> R.string.task_compare
    TaskTitle.TESTS -> R.string.task_tests
}

@Preview(showBackground = true)
@Composable
private fun TasksPreview() {
    MaterialTheme {
        TasksScreen(TasksState(tasks = listOf(Task("compose", TaskTitle.COMPOSE))), {}, {}, {})
    }
}
