package com.client.xvideos.r.ui.profile

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun rememberVisibleRangePercentIgnoringFirstNForGrid(
    gridState: LazyGridState,
    itemsToIgnore: Int = 0,
    numberOfColumns: Int = 2, // Добавляем параметр количества колонок
): State<Pair<Float, Float>> {

    if (numberOfColumns == 0) {
        return remember { mutableStateOf(0f to 1f) }
    }

    val result = remember { mutableStateOf(0f to 1f) }

    LaunchedEffect(gridState, itemsToIgnore, numberOfColumns) {
        snapshotFlow { gridState.layoutInfo }
            .distinctUntilChanged()
            .collect { layoutInfo ->
                if (layoutInfo.visibleItemsInfo.isEmpty() || layoutInfo.totalItemsCount == 0) {
                    result.value = 0f to 1f
                    return@collect
                }

                val relevantTotalItemsCount = layoutInfo.totalItemsCount - itemsToIgnore
                if (relevantTotalItemsCount <= 0) {
                    result.value = 0f to 1f
                    return@collect
                }

                // Общее количество релевантных РЯДОВ
                val totalRelevantRows = (relevantTotalItemsCount + numberOfColumns - 1) / numberOfColumns

                // Первый видимый элемент, который НЕ игнорируется
                val firstRelevantVisibleItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index >= itemsToIgnore }
                // Последний видимый элемент (он всегда релевантен, если есть)
                val lastRelevantVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull() // lastOrNull { it.index >= itemsToIgnore } - не нужно, т.к. last всегда будет >= first

                if (firstRelevantVisibleItem == null || lastRelevantVisibleItem == null || totalRelevantRows == 0) {
                    // Это может случиться, если все видимые элементы находятся в "игнорируемых"
                    // или если список пуст после вычета itemsToIgnore
                    val allItemsIgnoredOrBelowThreshold = layoutInfo.visibleItemsInfo.all { it.index < itemsToIgnore }
                    if (allItemsIgnoredOrBelowThreshold && layoutInfo.totalItemsCount > itemsToIgnore) {
                        // Все видимые элементы игнорируются, но есть еще элементы ниже
                        result.value = 0f to 0f // Или другое подходящее значение, показывающее, что мы в самом верху
                    } else if (relevantTotalItemsCount > 0) {
                        // Если есть релевантные элементы, но они не видны (например, прокрутили далеко вниз за пределы списка)
                        // Этого не должно происходить в нормальном LazyGrid, но для надежности
                        result.value = 1f to 1f
                    } else {
                        result.value = 0f to 1f // fallback
                    }
                    return@collect
                }

                // --- Расчет StartPercent на основе рядов ---
                val firstRelevantItemEffectiveIndex = firstRelevantVisibleItem.index - itemsToIgnore
                val firstRelevantItemRowIndex = firstRelevantItemEffectiveIndex / numberOfColumns

                // Доля, на которую первый релевантный РЯД прокручен за верхнюю границу
                // Для простоты, можно считать, что если первый элемент ряда виден, то ряд виден с его начала
                // Более точный расчет потребовал бы анализа всех элементов первого видимого ряда.
                // Пока оставим упрощенный вариант: если первый релевантный элемент виден, то его "начало" = 0% этого элемента.
                // Но нам нужна доля, которая *уже* проскроллена *выше* видимой области для первого релевантного ряда.

                val viewportStartOffset = layoutInfo.viewportStartOffset // Обычно 0, если нет contentPadding сверху
                val firstItemTopEdge = firstRelevantVisibleItem.offset.y.toFloat()
                val firstItemHeight = firstRelevantVisibleItem.size.height.toFloat()

                // Сколько от высоты первого релевантного элемента скрыто сверху
                val fractionOfFirstItemHidden = if (firstItemHeight > 0) {
                    (- (firstItemTopEdge - viewportStartOffset) / firstItemHeight).coerceIn(0f, 1f)
                } else 0f


                // StartPercent - какая доля релевантных РЯДОВ находится выше видимой области
                // (индекс первого релевантного ряда + доля этого ряда, которая скрыта сверху) / общее кол-во релевант. рядов
                val startPercent = if (totalRelevantRows > 0) {
                    (firstRelevantItemRowIndex + fractionOfFirstItemHidden) / totalRelevantRows
                } else 0f


                // --- Расчет EndPercent на основе рядов ---
                val lastRelevantItemEffectiveIndex = lastRelevantVisibleItem.index - itemsToIgnore
                val lastRelevantItemRowIndex = lastRelevantItemEffectiveIndex / numberOfColumns


                // Доля, на которую последний релевантный РЯД виден
                // Более сложная часть: нужно определить, какая часть *ряда* видна.
                // Можно взять видимую часть lastRelevantVisibleItem как аппроксимацию.
                val viewportEndOffset = layoutInfo.viewportEndOffset.toFloat()
                val lastItemTopEdge = lastRelevantVisibleItem.offset.y.toFloat()
                val lastItemHeight = lastRelevantVisibleItem.size.height.toFloat()

                // Какая часть высоты последнего элемента видна в viewport
                val visibleHeightOfLastItem = if (lastItemHeight > 0) {
                    (viewportEndOffset - lastItemTopEdge).coerceAtMost(lastItemHeight).coerceAtLeast(0f)
                } else 0f
                val fractionOfLastItemVisible = if (lastItemHeight > 0) visibleHeightOfLastItem / lastItemHeight else 0f

                // EndPercent - какая доля релевантных РЯДОВ (включая частично видимый последний) видна до конца этого ряда
                // (индекс последнего релевантного ряда + видимая доля этого ряда) / общее кол-во релевант. рядов
                val endPercent = if (totalRelevantRows > 0) {
                    ((lastRelevantItemRowIndex + fractionOfLastItemVisible) / totalRelevantRows)
                        .coerceAtMost(1f) // Убедимся, что не превышает 100%
                } else 0f

                //Timber.d("GRID SCROLL: ItemsToIgnore: $itemsToIgnore, NumCols: $numberOfColumns")
                //Timber.d("GRID SCROLL: RelevantTotalItems: $relevantTotalItemsCount, TotalRelevantRows: $totalRelevantRows")
                //Timber.d("GRID SCROLL: FirstVisible: idx=${firstRelevantVisibleItem.index}, effIdx=$firstRelevantItemEffectiveIndex, rowIdx=$firstRelevantItemRowIndex, hiddenFrac=$fractionOfFirstItemHidden, startP=$startPercent")
                //Timber.d("GRID SCROLL: LastVisible: idx=${lastRelevantVisibleItem.index}, effIdx=$lastRelevantItemEffectiveIndex, rowIdx=$lastRelevantItemRowIndex, visibleFrac=$fractionOfLastItemVisible, endP=$endPercent")


                result.value = startPercent.coerceIn(0f, 1f) to endPercent.coerceIn(startPercent, 1f)
            }
    }
    return result
}

@Composable
fun rememberVisibleRangePercentIgnoringFirstNForLazyColumn(
    gridState: LazyListState,
    itemsToIgnore: Int = 0,
): State<Pair<Float, Float>> {
    val result = remember { mutableStateOf(0f to 1f) }

    LaunchedEffect(gridState, itemsToIgnore) {
        snapshotFlow { gridState.layoutInfo }
            .distinctUntilChanged()
            .collect { layoutInfo ->
                if (layoutInfo.visibleItemsInfo.isEmpty() || layoutInfo.totalItemsCount == 0) {
                    result.value = 0f to 1f
                    return@collect
                }

                val relevantTotalItemsCount = layoutInfo.totalItemsCount - itemsToIgnore
                if (relevantTotalItemsCount <= 0) {
                    result.value = 0f to 1f
                    return@collect
                }

                // Общее количество релевантных РЯДОВ
                val totalRelevantRows = relevantTotalItemsCount

                // Первый видимый элемент, который НЕ игнорируется
                val firstRelevantVisibleItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index >= itemsToIgnore }
                // Последний видимый элемент (он всегда релевантен, если есть)
                val lastRelevantVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull() // lastOrNull { it.index >= itemsToIgnore } - не нужно, т.к. last всегда будет >= first

                if (firstRelevantVisibleItem == null || lastRelevantVisibleItem == null || totalRelevantRows == 0) {
                    // Это может случиться, если все видимые элементы находятся в "игнорируемых"
                    // или если список пуст после вычета itemsToIgnore
                    val allItemsIgnoredOrBelowThreshold = layoutInfo.visibleItemsInfo.all { it.index < itemsToIgnore }
                    if (allItemsIgnoredOrBelowThreshold && layoutInfo.totalItemsCount > itemsToIgnore) {
                        // Все видимые элементы игнорируются, но есть еще элементы ниже
                        result.value = 0f to 0f // Или другое подходящее значение, показывающее, что мы в самом верху
                    } else if (relevantTotalItemsCount > 0) {
                        // Если есть релевантные элементы, но они не видны (например, прокрутили далеко вниз за пределы списка)
                        // Этого не должно происходить в нормальном LazyGrid, но для надежности
                        result.value = 1f to 1f
                    } else {
                        result.value = 0f to 1f // fallback
                    }
                    return@collect
                }

                // --- Расчет StartPercent на основе рядов ---
                val firstRelevantItemEffectiveIndex = firstRelevantVisibleItem.index - itemsToIgnore
                val firstRelevantItemRowIndex = firstRelevantItemEffectiveIndex

                // Доля, на которую первый релевантный РЯД прокручен за верхнюю границу
                // Для простоты, можно считать, что если первый элемент ряда виден, то ряд виден с его начала
                // Более точный расчет потребовал бы анализа всех элементов первого видимого ряда.
                // Пока оставим упрощенный вариант: если первый релевантный элемент виден, то его "начало" = 0% этого элемента.
                // Но нам нужна доля, которая *уже* проскроллена *выше* видимой области для первого релевантного ряда.

                val viewportStartOffset = layoutInfo.viewportStartOffset // Обычно 0, если нет contentPadding сверху
                val firstItemTopEdge = firstRelevantVisibleItem.offset.toFloat()
                val firstItemHeight = firstRelevantVisibleItem.size.toFloat()

                // Сколько от высоты первого релевантного элемента скрыто сверху
                val fractionOfFirstItemHidden = if (firstItemHeight > 0) {
                    (- (firstItemTopEdge - viewportStartOffset) / firstItemHeight).coerceIn(0f, 1f)
                } else 0f


                // StartPercent - какая доля релевантных РЯДОВ находится выше видимой области
                // (индекс первого релевантного ряда + доля этого ряда, которая скрыта сверху) / общее кол-во релевант. рядов
                val startPercent = if (totalRelevantRows > 0) {
                    (firstRelevantItemRowIndex + fractionOfFirstItemHidden) / totalRelevantRows
                } else 0f


                // --- Расчет EndPercent на основе рядов ---
                val lastRelevantItemEffectiveIndex = lastRelevantVisibleItem.index - itemsToIgnore
                val lastRelevantItemRowIndex = lastRelevantItemEffectiveIndex


                // Доля, на которую последний релевантный РЯД виден
                // Более сложная часть: нужно определить, какая часть *ряда* видна.
                // Можно взять видимую часть lastRelevantVisibleItem как аппроксимацию.
                val viewportEndOffset = layoutInfo.viewportEndOffset.toFloat()
                val lastItemTopEdge = lastRelevantVisibleItem.offset.toFloat()
                val lastItemHeight = lastRelevantVisibleItem.size.toFloat()

                // Какая часть высоты последнего элемента видна в viewport
                val visibleHeightOfLastItem = if (lastItemHeight > 0) {
                    (viewportEndOffset - lastItemTopEdge).coerceAtMost(lastItemHeight).coerceAtLeast(0f)
                } else 0f
                val fractionOfLastItemVisible = if (lastItemHeight > 0) visibleHeightOfLastItem / lastItemHeight else 0f

                // EndPercent - какая доля релевантных РЯДОВ (включая частично видимый последний) видна до конца этого ряда
                // (индекс последнего релевантного ряда + видимая доля этого ряда) / общее кол-во релевант. рядов
                val endPercent = if (totalRelevantRows > 0) {
                    ((lastRelevantItemRowIndex + fractionOfLastItemVisible) / totalRelevantRows)
                        .coerceAtMost(1f) // Убедимся, что не превышает 100%
                } else 0f

                //Timber.d("GRID SCROLL: ItemsToIgnore: $itemsToIgnore, NumCols: $numberOfColumns")
                //Timber.d("GRID SCROLL: RelevantTotalItems: $relevantTotalItemsCount, TotalRelevantRows: $totalRelevantRows")
                //Timber.d("GRID SCROLL: FirstVisible: idx=${firstRelevantVisibleItem.index}, effIdx=$firstRelevantItemEffectiveIndex, rowIdx=$firstRelevantItemRowIndex, hiddenFrac=$fractionOfFirstItemHidden, startP=$startPercent")
                //Timber.d("GRID SCROLL: LastVisible: idx=${lastRelevantVisibleItem.index}, effIdx=$lastRelevantItemEffectiveIndex, rowIdx=$lastRelevantItemRowIndex, visibleFrac=$fractionOfLastItemVisible, endP=$endPercent")


                result.value = startPercent.coerceIn(0f, 1f) to endPercent.coerceIn(startPercent, 1f)
            }
    }
    return result
}

@Composable
fun rememberVisibleRangePercentIgnoringFirstNForLazyStaggeredGrid(
    staggeredGridState: LazyStaggeredGridState,
    itemsToIgnore: Int = 0,
): State<Pair<Float, Float>> {
    val result = remember { mutableStateOf(0f to 1f) }

    LaunchedEffect(staggeredGridState, itemsToIgnore) {
        snapshotFlow { staggeredGridState.layoutInfo }
            .distinctUntilChanged()
            .collect { layoutInfo ->
                if (layoutInfo.visibleItemsInfo.isEmpty() || layoutInfo.totalItemsCount == 0) {
                    result.value = 0f to 1f
                    return@collect
                }

                val relevantTotalItemsCount = layoutInfo.totalItemsCount - itemsToIgnore
                if (relevantTotalItemsCount <= 0) {
                    result.value = 0f to 1f
                    return@collect
                }

                // Первый видимый элемент, который НЕ игнорируется
                val firstRelevantVisibleItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index >= itemsToIgnore }

                // Последний видимый элемент
                val lastRelevantVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()

                if (firstRelevantVisibleItem == null || lastRelevantVisibleItem == null) {
                    val allItemsIgnoredOrBelowThreshold = layoutInfo.visibleItemsInfo.all { it.index < itemsToIgnore }
                    if (allItemsIgnoredOrBelowThreshold && layoutInfo.totalItemsCount > itemsToIgnore) {
                        result.value = 0f to 0f
                    } else if (relevantTotalItemsCount > 0) {
                        result.value = 1f to 1f
                    } else {
                        result.value = 0f to 1f
                    }
                    return@collect
                }

                // --- Расчет StartPercent ---
                val firstRelevantItemEffectiveIndex = firstRelevantVisibleItem.index - itemsToIgnore

                // Для StaggeredGrid нужно учесть offset элемента относительно viewport
                val viewportStartOffset = layoutInfo.viewportStartOffset
                val firstItemTopEdge = firstRelevantVisibleItem.offset.y.toFloat()
                val firstItemHeight = firstRelevantVisibleItem.size.height.toFloat()

                // Сколько от высоты первого релевантного элемента скрыто сверху
                val fractionOfFirstItemHidden = if (firstItemHeight > 0) {
                    (-(firstItemTopEdge - viewportStartOffset) / firstItemHeight).coerceIn(0f, 1f)
                } else 0f

                // StartPercent - какая доля релевантных элементов находится выше видимой области
                val startPercent = if (relevantTotalItemsCount > 0) {
                    (firstRelevantItemEffectiveIndex + fractionOfFirstItemHidden) / relevantTotalItemsCount
                } else 0f

                // --- Расчет EndPercent ---
                val lastRelevantItemEffectiveIndex = lastRelevantVisibleItem.index - itemsToIgnore

                val viewportEndOffset = layoutInfo.viewportEndOffset.toFloat()
                val lastItemTopEdge = lastRelevantVisibleItem.offset.y.toFloat()
                val lastItemHeight = lastRelevantVisibleItem.size.height.toFloat()

                // Какая часть высоты последнего элемента видна в viewport
                val visibleHeightOfLastItem = if (lastItemHeight > 0) {
                    (viewportEndOffset - lastItemTopEdge).coerceAtMost(lastItemHeight).coerceAtLeast(0f)
                } else 0f

                val fractionOfLastItemVisible = if (lastItemHeight > 0) {
                    visibleHeightOfLastItem / lastItemHeight
                } else 0f

                // EndPercent - какая доля релевантных элементов видна
                val endPercent = if (relevantTotalItemsCount > 0) {
                    ((lastRelevantItemEffectiveIndex + fractionOfLastItemVisible) / relevantTotalItemsCount)
                        .coerceAtMost(1f)
                } else 0f

                result.value = startPercent.coerceIn(0f, 1f) to endPercent.coerceIn(startPercent, 1f)
            }
    }

    return result
}

