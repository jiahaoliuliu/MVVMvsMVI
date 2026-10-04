# Clean Architecture: MVVM vs. MVI

## Introduction

For this example, we organize the app into three layers following Clean Architecture:

- **Data:** Implements repository contracts and manages access to data sources.
- **Domain:** Contains the entities, repository contracts, and use cases that express the application's business logic. This layer is independent of Android and the UI framework.
- **Presentation:** Handles user interactions and renders UI state.

MVVM (Model–View–ViewModel) and MVI (Model–View–Intent) are two common patterns for organizing the presentation layer. Both are compatible with Clean Architecture, but they organize user actions and UI state transitions differently.

To explore these differences, I built a simple app that implements the same screen using both patterns. The two implementations share the same Compose UI, UI state, domain layer, and data layer. This lets us focus on how each implementation handles user actions and updates the screen.

## General structure

The app starts with a selection screen where the user can choose the MVVM or MVI implementation. The source code is available on [GitHub](https://github.com/jiahaoliuliu/MVVMvsMVI).

*[Insert the architecture selection screenshot here.]*

Both screens display a task list loaded from a fake repository that simulates a remote data source. The user can tap a task to mark it as complete or incomplete. Completion choices are persisted locally using Preferences DataStore, so both screens read the same saved values when they load their tasks.

The user can also tap the Refresh button to reload the list. To demonstrate error handling, every third load attempt made by a repository instance fails deliberately.

*[Insert a task screen screenshot here.]*

### Data and domain layers

The data layer contains `FakeTaskRepository`, which implements `TaskRepository` and uses `TaskCompletionStore` to read and persist completion choices in DataStore.

The domain layer defines the repository contract:

```kotlin
interface TaskRepository {
    suspend fun loadTasks(): List<Task>
    suspend fun toggleTask(id: String): Task?
}
```

`LoadTasksUseCase` calls `loadTasks()` to retrieve the tasks with their saved completion values. `ToggleTaskUseCase` calls `toggleTask(id)` to switch a task between complete and incomplete and return the updated task. The repository returns `null` if the ID does not identify a task.

A task is represented by a simple data class containing an ID, a title, and a completion flag:

```kotlin
data class Task(
    val id: String,
    val title: TaskTitle,
    val completed: Boolean = false,
)
```

### Shared presentation code

The presentation layer contains a separate package for each pattern. Both implementations use the same `TasksScreen` composable:

```kotlin
@Composable
fun TasksScreen(
    state: TasksState,
    onRefresh: () -> Unit,
    onToggle: (String) -> Unit,
    onBack: () -> Unit,
)
```

The composable receives the current `TasksState` and callbacks for user actions. It renders the state and invokes the appropriate callback when the user interacts with the screen.

`TasksState` contains the task list and flags describing loading, toggling, and errors:

```kotlin
data class TasksState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val isToggling: Boolean = false,
    val hasToggleError: Boolean = false,
)
```

Both ViewModels keep a private mutable state flow and expose a read-only `StateFlow` to the UI:

```kotlin
private val mutableState = MutableStateFlow(TasksState())
val state = mutableState.asStateFlow()
```

## Model–View–ViewModel: Actions are methods

In the MVVM implementation, the ViewModel exposes methods for the actions supported by the screen:

```kotlin
class MvvmViewModel(
    private val loadTaskUseCase: LoadTasksUseCase,
    private val toggleTaskUseCase: ToggleTaskUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TasksState())
    val state = mutableState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() { /* ... */ }

    fun toggle(id: String) { /* ... */ }
}
```

The activity passes these methods to `TasksScreen` as callbacks:

```kotlin
TasksScreen(
    state = viewModel.state.collectAsStateWithLifecycle().value,
    onRefresh = viewModel::refresh,
    onToggle = viewModel::toggle,
    onBack = onBackPressedDispatcher::onBackPressed,
)
```

For example, when the user toggles a task, the ViewModel executes the following method:

```kotlin
fun toggle(id: String) {
    if (state.value.isLoading || state.value.isToggling) return

    mutableState.update {
        it.copy(isToggling = true, hasToggleError = false)
    }

    viewModelScope.launch {
        try {
            val toggled = toggleTaskUseCase(id)
            mutableState.update { state ->
                state.copy(
                    isToggling = false,
                    tasks = state.tasks.map {
                        if (toggled != null && it.id == toggled.id) toggled else it
                    },
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            mutableState.update {
                it.copy(isToggling = false, hasToggleError = true)
            }
        }
    }
}
```

The method:

1. Checks whether loading or another toggle is already in progress.
2. Sets `isToggling` to `true` and clears the previous toggle error.
3. Calls `ToggleTaskUseCase` inside a coroutine.
4. Replaces the matching task in the UI state with the confirmed task returned by the use case.
5. Clears the progress flag. If the operation fails, it preserves the task list and exposes a toggle error. Coroutine cancellation is rethrown rather than treated as an operation failure.

The asynchronous work and its UI state transitions are coordinated within the same action method.

*[Insert the numbered MVVM sequence diagram here.]*

## Model–View–Intent: Actions and outcomes are explicit values

In the MVI implementation, an intent describes what the user requests. The supported intents are defined in a sealed interface:

```kotlin
sealed interface TasksIntent {
    data object Refresh : TasksIntent
    data class Toggle(val id: String) : TasksIntent
}
```

The ViewModel receives them through a single entry point:

```kotlin
fun accept(intent: TasksIntent) { /* ... */ }
```

The activity translates the UI callbacks into intents:

```kotlin
TasksScreen(
    state = model.state.collectAsStateWithLifecycle().value,
    onRefresh = { model.accept(TasksIntent.Refresh) },
    onToggle = { model.accept(TasksIntent.Toggle(it)) },
    onBack = onBackPressedDispatcher::onBackPressed,
)
```

A result describes an operation's progress or outcome:

```kotlin
sealed interface TasksResult {
    data object Loading : TasksResult
    data class Loaded(val tasks: List<Task>) : TasksResult
    data object Failed : TasksResult
    data object Toggling : TasksResult
    data class Toggled(val updatedTask: Task?) : TasksResult
    data object ToggleFailed : TasksResult
}
```

The ViewModel coordinates the following steps:

1. Checks whether another operation is already in progress.
2. Uses a progress result to produce and publish a loading or toggling state.
3. Enqueues and processes the intent, calling the appropriate use case.
4. Uses the operation's result to produce and publish the next UI state.

For steps 2 and 4, the ViewModel delegates state transitions to `TasksReducer`:

```kotlin
fun reduce(state: TasksState, result: TasksResult): TasksState
```

The reducer is a pure function: it takes the current state and a result and returns the next state. It does not call use cases or write to DataStore. The ViewModel coordinates operation execution, while the reducer defines how the UI state changes in response to progress and outcomes.

### Following a toggle through MVI

*[Insert the numbered MVI sequence diagram here.]*

1. The user taps a task's checkbox or row.
2. `TasksScreen` invokes its callback, and the activity calls `accept(TasksIntent.Toggle(id))` on the ViewModel.
3. The ViewModel checks the loading and toggling guard. The action proceeds if neither operation is running.
4. The ViewModel passes the current state and `TasksResult.Toggling` to the reducer.
5. The reducer returns a new state with `isToggling = true` and the previous toggle error cleared.
6. The ViewModel publishes this state through `StateFlow`. `TasksScreen` observes it and disables further interactions while the operation is running.
7. The original `TasksIntent.Toggle(id)` is enqueued in a `Channel`.
8. The intent-processing loop receives the intent and calls the ViewModel's internal `toggle(id)` method.
9. That method invokes `ToggleTaskUseCase`.
10. The use case calls `TaskRepository.toggleTask(id)`.
11. The repository uses `TaskCompletionStore` to atomically toggle and persist the completion value in DataStore.
12. DataStore completes the update, and the store returns the confirmed completion value.
13. The repository returns an updated `Task` containing that value.
14. `ToggleTaskUseCase` returns the updated task to the ViewModel.
15. The ViewModel passes the current state and `TasksResult.Toggled(updatedTask)` to the reducer.
16. The reducer returns a new state with the matching task replaced and `isToggling = false`.
17. The ViewModel publishes the new state through `StateFlow`.
18. `TasksScreen` observes the state, displays the confirmed checkbox value, and enables interactions again.

The distinction between `Toggling` and `Toggled` matters. `Toggling` indicates that the operation is in progress. `Toggled` carries its confirmed outcome. By the time the reducer receives `Toggled`, the repository has already changed and persisted the completion value; the reducer only incorporates the returned task into the UI state.

## MVVM vs. MVI

In this sample, MVVM and MVI share the same Compose UI, domain use cases, repository, and DataStore persistence. Both expose immutable UI state snapshots through `StateFlow`, which Compose observes to render the screen.

Their main differences are:

- **Handling user actions:** In MVVM, the UI calls specific ViewModel methods, such as `refresh()` or `toggle(id)`. In MVI, the UI sends typed intents, such as `TasksIntent.Refresh` or `TasksIntent.Toggle(id)`, through a single `accept()` method.
- **Processing actions:** In MVVM, each ViewModel method coordinates its operation and updates the UI state. In this MVI implementation, a `Channel` delivers intents to a processing loop, which calls the appropriate operation.
- **Updating UI state:** In MVVM, state changes happen directly inside the ViewModel's action methods. In MVI, the ViewModel passes results to `TasksReducer`, which combines the current state with a result to produce the next state.
- **Representing progress and outcomes:** MVVM updates fields such as `isToggling` and `hasToggleError` directly. MVI represents these transitions with explicit types, such as `TasksResult.Toggling`, `TasksResult.Toggled(updatedTask)`, and `TasksResult.ToggleFailed`.
- **Toggling a task:** Both implementations call the same `ToggleTaskUseCase`, and the repository persists the change in DataStore. Once the updated task returns, MVVM applies it directly to the UI state, while MVI passes it to the reducer through `TasksResult.Toggled`. The reducer only applies the confirmed task to the UI state.
- **Testing:** Both implementations need tests for loading, toggling, failures, and overlapping actions. MVI also allows the reducer's state transitions to be tested independently, without coroutines or a repository.
- **Code complexity:** MVVM uses fewer types and keeps each action's flow together, making this small example easy to follow. MVI introduces intents, results, a processing loop, and a reducer, providing a consistent structure as the number of actions and transitions grows.

These differences describe the implementations in this sample. MVVM can also use typed actions and a reducer, and MVI does not require a `Channel` or a particular library. Both implementations use unidirectional data flow. Their handling of overlapping actions is an explicit design choice rather than a guarantee provided by the architecture name.

## Conclusion

MVVM and MVI offer different ways to organize the presentation layer within Clean Architecture. In this example, the UI and business behavior stay the same: both implementations load the same tasks, call the same use cases, and persist completion choices in the same DataStore. What changes is how a user action becomes a new UI state.

MVVM keeps that flow together in a ViewModel method, making a small feature straightforward to read and maintain. MVI makes the flow explicit through intents, results, and a reducer, adding structure and allowing state transitions to be tested in isolation. That structure can be useful when a screen has many actions and transitions, but it also introduces more code and concepts.

Neither pattern is automatically the better choice. Choose the structure that helps your team understand the feature, handle failures, and verify its behavior. Whichever approach you use, keep business operations in the domain and data layers and make the presentation layer's state transitions clear.
