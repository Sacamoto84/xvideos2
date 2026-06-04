package com.client.xvideos.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun ComposableLifecycle(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onEvent: (LifecycleOwner, Lifecycle.Event) -> Unit,
) {
    DisposableEffect(lifecycleOwner) {
        // 1. Create a LifecycleEventObserver to handle lifecycle events.
        val observer = LifecycleEventObserver { source, event ->
            // 2. Call the provided onEvent callback with the source and event.
            onEvent(source, event)
        }

        // 3. Add the observer to the lifecycle of the provided LifecycleOwner.
        lifecycleOwner.lifecycle.addObserver(observer)

        // 4. Remove the observer when the composable is disposed.
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}