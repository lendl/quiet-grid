package com.quietgrid.app.ui.play

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.session.FinishKind
import com.quietgrid.app.ui.components.EndPuzzleDialog

const val PLAY_BOARD_TAG = "play_board"
const val PLAY_HINT_TAG = "play_hint"
const val PLAY_RULES_TAG = "play_rules"

private val HINT_BOARD_SPACING = 4.dp
private val SIDE_HINT_MIN_WIDTH = 160.dp
private val TABLET_HINT_AREA_HEIGHT = 176.dp
private const val WIDE_DOCK_FRACTION = 0.5f
private val TABLET_DRAG_DOCK_WIDTH = 240.dp

private typealias HintCardSlot = @Composable (maxHeight: Dp, expandedMaxHeight: Dp, modifier: Modifier) -> Unit

@Composable
fun GamePlayScaffold(
    gameId: GameId,
    timer: PlayTimer?,
    onBack: () -> Unit,
    onEndPuzzle: () -> Unit,
    modifier: Modifier = Modifier,
    difficulty: Difficulty? = null,
    hint: PlayHintAction? = null,
    hintMessage: PlayHintMessage? = null,
    info: List<InfoItem> = emptyList(),
    boardAspectRatio: Float = 1f,
    boardAspectRatioFor: ((PlaySizeClass) -> Float)? = null,
    wideDock: Boolean = false,
    phonePortraitOnly: Boolean = false,
    dockRole: DockRole = DockRole.Controls,
    dockHeight: (PlaySizeClass) -> Dp = { 0.dp },
    dock: (@Composable ColumnScope.(PlaySizeClass) -> Unit)? = null,
    finishing: FinishKind? = null,
    foldRows: Int? = null,
    foldCols: Int? = null,
    board: @Composable BoxScope.(PlaySizeClass) -> Unit,
) {
    var showEndDialog by remember { mutableStateOf(false) }
    var rulesOpen by remember { mutableStateOf(false) }
    BackHandler(enabled = rulesOpen) { rulesOpen = false }
    BackHandler(enabled = finishing != null) {}
    LaunchedEffect(finishing) {
        if (finishing != null) {
            rulesOpen = false
            showEndDialog = false
        }
    }
    val fold = rememberFoldState(finishing)
    val (rows, cols) = remember(foldRows, foldCols, boardAspectRatio) { foldGrid(foldRows, foldCols, boardAspectRatio) }
    val preferences = LocalGamePlayPreferences.current
    ApplyPlayOrientation(preferences.rotation, phonePortraitOnly)
    val openGameRules = LocalOpenGameRules.current

    val shownHint = hint?.takeIf { preferences.showHints }
    val shownTimer = timer?.takeUnless { it is PlayTimer.Elapsed && !preferences.showTimer }
    val hintActive = shownHint?.active == true
    var hintCardHidden by remember(hintActive) { mutableStateOf(false) }
    val hintCardVisible = hintActive && !hintCardHidden && hintMessage != null
    val chromeHint = shownHint?.copy(
        onClick = {
            rulesOpen = false
            if (hintActive && hintCardHidden) hintCardHidden = false else shownHint.onClick()
        },
    )
    val actions = PlayChromeActions(
        timer = shownTimer,
        hint = chromeHint,
        rulesOpen = rulesOpen,
        onBack = onBack,
        onToggleRules = {
            if (!rulesOpen && hintCardVisible) hintCardHidden = true
            rulesOpen = !rulesOpen
        },
        onEndPuzzle = { if (preferences.confirmEndPuzzle) showEndDialog = true else onEndPuzzle() },
    )
    val stripItems = playContextItems(gameId, difficulty) + info
    val contentDirection = LocalLayoutDirection.current

    BoxWithConstraints(modifier.fillMaxSize()) {
        val sizeClass = playSizeClass(maxWidth, maxHeight)
        val metrics = playMetrics(sizeClass)
        val ratio = boardAspectRatioFor?.invoke(sizeClass) ?: boardAspectRatio
        val mirrored = preferences.leftHanded && sizeClass in MIRRORABLE_SIZE_CLASSES
        val layoutDirection = if (mirrored) contentDirection.flipped() else contentDirection
        val boardSlot: @Composable BoxScope.() -> Unit = {
            InContentDirection {
                FinishFold(fold, finishing, rows, cols) { board(sizeClass) }
            }
        }
        val dockSlot: (@Composable ColumnScope.() -> Unit)? = if (dock != null) {
            { InContentDirection { dock(sizeClass) } }
        } else {
            null
        }
        val message = hintMessage
        val hintCard: HintCardSlot? = when {
            !hintCardVisible || message == null -> null
            mirrored -> { cardMaxHeight, cardExpandedMaxHeight, cardModifier ->
                Box(cardModifier.foldChrome(fold)) {
                    InContentDirection {
                        PlayHintCard(message, onClose = { hintCardHidden = true }, cardMaxHeight, cardExpandedMaxHeight)
                    }
                }
            }
            else -> { cardMaxHeight, cardExpandedMaxHeight, cardModifier ->
                PlayHintCard(message, onClose = { hintCardHidden = true }, cardMaxHeight, cardExpandedMaxHeight, cardModifier.foldChrome(fold))
            }
        }
        val dockAreaHeight = if (dock != null) dockHeight(sizeClass) else 0.dp
        CompositionLocalProvider(
            LocalPlayMetrics provides metrics,
            LocalPlayContentDirection provides contentDirection,
            LocalLayoutDirection provides layoutDirection,
        ) {
            when (sizeClass) {
                PlaySizeClass.PhoneLandscape -> PhoneLandscapeLayout(
                    metrics, actions, stripItems, if (wideDock) maxWidth * WIDE_DOCK_FRACTION else phoneLandscapePanelWidth(maxWidth), ratio,
                    reserveHintSpace = shownHint != null, dragSource = dockRole == DockRole.DragSource, fold, boardSlot, dockSlot, hintCard,
                )
                PlaySizeClass.TabletLandscape -> if (dockRole == DockRole.DragSource) {
                    TabletDragSourceLayout(metrics, actions, stripItems, fold, boardSlot, dockSlot)
                } else {
                    TabletLandscapeLayout(
                        metrics, actions, stripItems, tabletPanelWidth(maxWidth),
                        reserveHintArea = shownHint != null, fold, boardSlot, dockSlot, hintCard,
                    )
                }
                else -> UprightLayout(metrics, actions, stripItems, ratio, dockAreaHeight, fold, boardSlot, dockSlot, hintCard)
            }
        }
        if (finishing != null) {
            Box(Modifier.fillMaxSize().pointerInput(Unit) { consumeAllPointerEvents() })
        }
        if (rulesOpen) {
            RulesPeekOverlay(
                gameId = gameId,
                cardAlignment = when {
                    !sizeClass.isTablet -> Alignment.TopCenter
                    mirrored -> Alignment.TopStart
                    else -> Alignment.TopEnd
                },
                onDismiss = { rulesOpen = false },
                onFullRules = openGameRules?.let { open ->
                    {
                        rulesOpen = false
                        open(gameId)
                    }
                },
                modifier = if (sizeClass == PlaySizeClass.PhoneLandscape) {
                    if (mirrored) Modifier.padding(end = PLAY_RAIL_WIDTH) else Modifier.padding(start = PLAY_RAIL_WIDTH)
                } else {
                    Modifier.padding(top = metrics.topBarHeight)
                },
            )
        }
    }

    EndPuzzleDialog(
        visible = showEndDialog,
        onDismiss = { showEndDialog = false },
        onConfirm = {
            showEndDialog = false
            onEndPuzzle()
        },
    )
}

private suspend fun PointerInputScope.consumeAllPointerEvents() {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
        }
    }
}

private val MIRRORABLE_SIZE_CLASSES = setOf(PlaySizeClass.PhoneLandscape, PlaySizeClass.TabletLandscape)

private val LocalPlayContentDirection = staticCompositionLocalOf { LayoutDirection.Ltr }

private fun LayoutDirection.flipped(): LayoutDirection =
    if (this == LayoutDirection.Ltr) LayoutDirection.Rtl else LayoutDirection.Ltr

@Composable
private fun InContentDirection(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LocalPlayContentDirection.current, content = content)
}

@Composable
private fun UprightLayout(
    metrics: PlayMetrics,
    actions: PlayChromeActions,
    stripItems: List<InfoItem>,
    boardAspectRatio: Float,
    dockAreaHeight: Dp,
    fold: FoldState,
    board: @Composable BoxScope.() -> Unit,
    dock: (@Composable ColumnScope.() -> Unit)?,
    hintCard: HintCardSlot?,
) {
    val gap = metrics.contentPadding
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val headerHeight = metrics.topBarHeight + metrics.stripHeight
        val dockSpace = if (dock != null) dockAreaHeight + gap else 0.dp
        val bottomLimit = (maxHeight - gap - dockSpace).coerceAtLeast(headerHeight)
        val bodyWidth = (maxWidth - gap * 2).coerceAtLeast(0.dp)
        val boardHeight = uprightBoardHeight(bodyWidth, bottomLimit - headerHeight, boardAspectRatio, 0.dp)
        val boardTop = centeredBoardTop(maxHeight, boardHeight, headerHeight, bottomLimit)
        Column(Modifier.fillMaxWidth().foldChrome(fold)) {
            PlayTopBar(metrics, actions)
            InfoStrip(stripItems, metrics)
        }
        Box(
            Modifier.padding(start = gap, end = gap, top = boardTop).fillMaxWidth().height(boardHeight).testTag(PLAY_BOARD_TAG),
            contentAlignment = Alignment.Center,
            content = board,
        )
        if (dock != null) {
            Column(
                Modifier.align(Alignment.BottomCenter).padding(start = gap, end = gap, bottom = gap).fillMaxWidth().height(dockAreaHeight).foldDock(fold).clipToBounds(),
                content = dock,
            )
        }
        if (hintCard != null) {
            hintCard(
                boardTop - metrics.topBarHeight - HINT_BOARD_SPACING,
                bottomLimit - metrics.topBarHeight - HINT_BOARD_SPACING,
                Modifier.padding(top = metrics.topBarHeight, start = gap, end = gap),
            )
        }
    }
}

@Composable
private fun PhoneLandscapeLayout(
    metrics: PlayMetrics,
    actions: PlayChromeActions,
    stripItems: List<InfoItem>,
    panelWidth: Dp,
    boardAspectRatio: Float,
    reserveHintSpace: Boolean,
    dragSource: Boolean,
    fold: FoldState,
    board: @Composable BoxScope.() -> Unit,
    dock: (@Composable ColumnScope.() -> Unit)?,
    hintCard: HintCardSlot?,
) {
    val gap = metrics.contentPadding
    val hasDock = dock != null
    val hasSideColumn = !hasDock && (stripItems.isNotEmpty() || reserveHintSpace)
    Row(Modifier.fillMaxSize().padding(end = gap)) {
        Box(Modifier.foldChrome(fold)) { InContentDirection { PlayRail(metrics, actions) } }
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
            val panelSpace = if (hasDock) panelWidth else 0.dp
            val boardAreaWidth = (maxWidth - panelSpace).coerceAtLeast(0.dp)
            val boardWidth = min(boardAreaWidth - gap * 2, (maxHeight - gap * 2) * boardAspectRatio).coerceAtLeast(0.dp)
            val freeWidth = (boardAreaWidth - gap * 2 - boardWidth).coerceAtLeast(0.dp)
            val needsSideRoom = if (hasDock) reserveHintSpace else hasSideColumn
            val centered = dragSource || !needsSideRoom || freeWidth / 2 >= SIDE_HINT_MIN_WIDTH
            val boardStart = gap + if (centered) freeWidth / 2 else 0.dp
            val besideBoard = boardStart + boardWidth + gap
            Box(
                Modifier.padding(start = boardStart, top = gap, bottom = gap).width(boardWidth).fillMaxHeight().testTag(PLAY_BOARD_TAG),
                contentAlignment = Alignment.Center,
                content = board,
            )
            if (hasDock) {
                val panelStart = if (dragSource) besideBoard else maxWidth - panelSpace
                Column(Modifier.padding(start = panelStart.coerceAtLeast(0.dp)).width(panelWidth).fillMaxHeight().padding(vertical = gap)) {
                    InContentDirection { InfoStrip(stripItems, metrics, Modifier.foldChrome(fold), stacked = true) }
                    Column(Modifier.fillMaxWidth().weight(1f).padding(top = gap).foldDock(fold).clipToBounds(), content = dock)
                }
                if (hintCard != null) {
                    val hintEnd = if (boardAreaWidth - besideBoard >= SIDE_HINT_MIN_WIDTH) panelSpace + gap else 0.dp
                    val hintHeight = maxHeight - gap * 2
                    hintCard(hintHeight, hintHeight, Modifier.padding(start = besideBoard, end = hintEnd, top = gap))
                }
            } else if (hasSideColumn) {
                Column(Modifier.padding(start = besideBoard, top = gap, bottom = gap).fillMaxSize()) {
                    if (stripItems.isNotEmpty()) InContentDirection { InfoStrip(stripItems, metrics, Modifier.foldChrome(fold), stacked = true) }
                    if (hintCard != null) {
                        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(top = if (stripItems.isNotEmpty()) gap else 0.dp)) {
                            hintCard(maxHeight, maxHeight, Modifier)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabletDragSourceLayout(
    metrics: PlayMetrics,
    actions: PlayChromeActions,
    stripItems: List<InfoItem>,
    fold: FoldState,
    board: @Composable BoxScope.() -> Unit,
    dock: (@Composable ColumnScope.() -> Unit)?,
) {
    val gap = metrics.contentPadding
    Column(Modifier.fillMaxSize()) {
        InContentDirection {
            Column(Modifier.fillMaxWidth().foldChrome(fold)) {
                PlayTopBar(metrics, actions)
                InfoStrip(stripItems, metrics)
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(start = gap, end = gap, bottom = gap)) {
            val boardSize = min(maxHeight, maxWidth - TABLET_DRAG_DOCK_WIDTH - gap).coerceAtLeast(0.dp)
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(boardSize).testTag(PLAY_BOARD_TAG), contentAlignment = Alignment.Center, content = board)
                if (dock != null) {
                    Column(Modifier.width(TABLET_DRAG_DOCK_WIDTH).fillMaxHeight().foldDock(fold).clipToBounds(), content = dock)
                }
            }
        }
    }
}

@Composable
private fun TabletLandscapeLayout(
    metrics: PlayMetrics,
    actions: PlayChromeActions,
    stripItems: List<InfoItem>,
    panelWidth: Dp,
    reserveHintArea: Boolean,
    fold: FoldState,
    board: @Composable BoxScope.() -> Unit,
    dock: (@Composable ColumnScope.() -> Unit)?,
    hintCard: HintCardSlot?,
) {
    val gap = metrics.contentPadding
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.foldChrome(fold)) { InContentDirection { PlayTopBar(metrics, actions) } }
        Row(
            Modifier.weight(1f).fillMaxWidth().padding(start = gap, end = gap, bottom = gap),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            Box(
                Modifier.weight(1f).fillMaxHeight().testTag(PLAY_BOARD_TAG),
                contentAlignment = Alignment.Center,
                content = board,
            )
            Column(Modifier.width(panelWidth).fillMaxHeight()) {
                InContentDirection { InfoStrip(stripItems, metrics, Modifier.foldChrome(fold)) }
                if (reserveHintArea) {
                    Box(Modifier.fillMaxWidth().height(TABLET_HINT_AREA_HEIGHT).padding(top = 8.dp)) {
                        val cardHeight = TABLET_HINT_AREA_HEIGHT - 8.dp
                        hintCard?.invoke(cardHeight, cardHeight, Modifier)
                    }
                }
                if (dock != null) {
                    Column(Modifier.fillMaxWidth().weight(1f).padding(top = 16.dp).foldDock(fold).clipToBounds(), content = dock)
                }
            }
        }
    }
}
