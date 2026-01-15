package com.leafy.composetreeview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

// =============================================================================
// DATA MODEL
// =============================================================================

/**
 * A node in a hierarchical graph structure with optional styling.
 *
 * @param T The type of data held by this node.
 * @property data The data held by this node.
 * @property children The child nodes.
 * @property color Optional Compose Color for this node (used by the content slot).
 * @property backgroundRes Optional drawable resource ID for custom backgrounds.
 */
data class GTWNode<T>(
    val data: T,
    val children: List<GTWNode<T>> = emptyList(),
    val color: Color? = null,
    val backgroundRes: Int? = null
) {
    val hasChildren: Boolean get() = children.isNotEmpty()
}

/**
 * DSL builder for creating graph nodes with optional styling.
 */
class GTWNodeBuilder<T>(
    private val data: T,
    private var color: Color? = null,
    private var backgroundRes: Int? = null
) {
    private val children = mutableListOf<GTWNode<T>>()

    /**
     * Set the color for this node.
     */
    fun color(color: Color) {
        this.color = color
    }

    /**
     * Set a drawable background resource for this node.
     */
    fun backgroundRes(resId: Int) {
        this.backgroundRes = resId
    }

    /**
     * Add a child node.
     */
    fun gtwNode(
        data: T,
        color: Color? = null,
        backgroundRes: Int? = null,
        init: GTWNodeBuilder<T>.() -> Unit = {}
    ) {
        children.add(GTWNodeBuilder(data, color, backgroundRes).apply(init).build())
    }

    fun build(): GTWNode<T> = GTWNode(data, children.toList(), color, backgroundRes)
}

/**
 * Creates a [GTWNode] using DSL syntax with optional styling.
 */
fun <T> gtwNode(
    data: T,
    color: Color? = null,
    backgroundRes: Int? = null,
    init: GTWNodeBuilder<T>.() -> Unit = {}
): GTWNode<T> = GTWNodeBuilder(data, color, backgroundRes).apply(init).build()

// =============================================================================
// LAYOUT ALGORITHM
// =============================================================================

internal data class LayoutNode<T>(
    val node: GTWNode<T>,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val depth: Int
) {
    val centerX: Float get() = x
    val top: Float get() = y
    val bottom: Float get() = y + height
    val left: Float get() = x - width / 2
}

internal data class TreeLayoutConfig(
    val levelSpacing: Float,
    val nodeSpacing: Float,
    val defaultNodeWidth: Float,
    val defaultNodeHeight: Float
)

internal data class TreeBounds(val x: Float, val y: Float, val width: Float, val height: Float)

internal fun <T> List<LayoutNode<T>>.bounds(): TreeBounds {
    if (isEmpty()) return TreeBounds(0f, 0f, 0f, 0f)
    val minX = minOf { it.left }
    val maxX = maxOf { it.left + it.width }
    val minY = minOf { it.top }
    val maxY = maxOf { it.bottom }
    return TreeBounds(minX, minY, maxX - minX, maxY - minY)
}

internal class LayoutWorkNode<T>(
    val node: GTWNode<T>,
    val depth: Int,
    var x: Float = 0f,
    var width: Float = 0f,
    var height: Float = 0f,
    var mod: Float = 0f,
    val children: MutableList<LayoutWorkNode<T>> = mutableListOf()
)

internal class TreeLayoutAlgorithm<T>(private val config: TreeLayoutConfig) {
    private var nextX = 0f

    fun layout(root: GTWNode<T>, nodeSizes: Map<T, IntSize>): List<LayoutNode<T>> {
        nextX = 0f
        val workRoot = buildWorkTree(root, 0, nodeSizes)
        firstPass(workRoot)
        val result = mutableListOf<LayoutNode<T>>()
        secondPass(workRoot, 0f, result)
        val minX = result.minOfOrNull { it.left } ?: 0f
        val shiftX = -minX + config.nodeSpacing
        return result.map { it.copy(x = it.x + shiftX) }
    }

    private fun buildWorkTree(node: GTWNode<T>, depth: Int, nodeSizes: Map<T, IntSize>): LayoutWorkNode<T> {
        val size = nodeSizes[node.data]
        val workNode = LayoutWorkNode(
            node = node,
            depth = depth,
            width = size?.width?.toFloat() ?: config.defaultNodeWidth,
            height = size?.height?.toFloat() ?: config.defaultNodeHeight
        )
        node.children.forEach { child ->
            workNode.children.add(buildWorkTree(child, depth + 1, nodeSizes))
        }
        return workNode
    }

    private fun firstPass(node: LayoutWorkNode<T>) {
        node.children.forEach { firstPass(it) }
        if (node.children.isEmpty()) {
            node.x = nextX
            nextX += node.width + config.nodeSpacing
        } else if (node.children.size == 1) {
            node.x = node.children[0].x
        } else {
            node.x = (node.children.first().x + node.children.last().x) / 2
        }
    }

    private fun secondPass(node: LayoutWorkNode<T>, modSum: Float, result: MutableList<LayoutNode<T>>) {
        val finalX = node.x + modSum
        val finalY = node.depth * (config.defaultNodeHeight + config.levelSpacing)
        result.add(LayoutNode(node.node, finalX, finalY, node.width, node.height, node.depth))
        node.children.forEach { child -> secondPass(child, modSum + node.mod, result) }
    }
}

// =============================================================================
// ZOOM & PAN
// =============================================================================

/**
 * State for zoom and pan transformations.
 */
class ZoomPanState {
    var scale by mutableFloatStateOf(1f)
    var offsetX by mutableFloatStateOf(0f)
    var offsetY by mutableFloatStateOf(0f)

    fun reset() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }
}

@Composable
fun rememberZoomPanState(): ZoomPanState = remember { ZoomPanState() }

@Composable
private fun ZoomPanContainer(
    state: ZoomPanState,
    minScale: Float = 0.1f,
    maxScale: Float = 3f,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val newScale = (state.scale * zoom).coerceIn(minScale, maxScale)
                    val scaleDiff = newScale - state.scale
                    state.offsetX -= (centroid.x - state.offsetX) * scaleDiff / state.scale
                    state.offsetY -= (centroid.y - state.offsetY) * scaleDiff / state.scale
                    state.scale = newScale
                    state.offsetX += pan.x
                    state.offsetY += pan.y
                }
            }
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                scaleX = state.scale
                scaleY = state.scale
                translationX = state.offsetX
                translationY = state.offsetY
                transformOrigin = TransformOrigin(0f, 0f)
            }
        ) {
            content()
        }
    }
}

// =============================================================================
// CONNECTION DRAWING
// =============================================================================

enum class LineStyle { Bezier, Stepped, Straight }

private fun <T> DrawScope.drawConnection(
    parent: LayoutNode<T>,
    child: LayoutNode<T>,
    color: Color,
    strokeWidth: Float,
    style: LineStyle
) {
    val startX = parent.centerX
    val startY = parent.bottom
    val endX = child.centerX
    val endY = child.top

    when (style) {
        LineStyle.Bezier -> {
            val path = Path().apply {
                moveTo(startX, startY)
                val controlY = (startY + endY) / 2
                cubicTo(startX, controlY, endX, controlY, endX, endY)
            }
            drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        LineStyle.Stepped -> {
            val midY = (startY + endY) / 2
            val path = Path().apply {
                moveTo(startX, startY)
                lineTo(startX, midY)
                lineTo(endX, midY)
                lineTo(endX, endY)
            }
            drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        LineStyle.Straight -> {
            drawLine(color, Offset(startX, startY), Offset(endX, endY), strokeWidth, StrokeCap.Round)
        }
    }
}

// =============================================================================
// MAIN COMPOSABLE
// =============================================================================

/**
 * Configuration for the graph appearance.
 */
data class GraphConfig(
    val levelSpacing: Dp = 80.dp,
    val nodeSpacing: Dp = 40.dp,
    val lineColor: Color = Color(0xFF1976D2),
    val lineWidth: Dp = 2.dp,
    val lineStyle: LineStyle = LineStyle.Bezier
)

/**
 * A 2D hierarchical tree graph with zoom and pan support.
 *
 * @param T The data type for nodes.
 * @param root The root node of the tree.
 * @param modifier Modifier for the container.
 * @param config Visual configuration.
 * @param zoomPanState State for zoom/pan gestures.
 * @param nodeContent Composable to render each node. Receives the node data,
 *        optional color, and optional background resource ID for styling.
 */
@Composable
fun <T> GraphTreeView(
    root: GTWNode<T>,
    modifier: Modifier = Modifier,
    config: GraphConfig = GraphConfig(),
    zoomPanState: ZoomPanState = rememberZoomPanState(),
    nodeContent: @Composable (node: T, color: Color?, backgroundRes: Int?) -> Unit
) {
    val density = LocalDensity.current
    val nodeSizes = remember { mutableStateMapOf<T, IntSize>() }
    var layoutNodes by remember { mutableStateOf<List<LayoutNode<T>>>(emptyList()) }
    var treeBounds by remember { mutableStateOf(TreeBounds(0f, 0f, 0f, 0f)) }

    LaunchedEffect(root, nodeSizes.size) {
        val layoutConfig = with(density) {
            TreeLayoutConfig(
                levelSpacing = config.levelSpacing.toPx(),
                nodeSpacing = config.nodeSpacing.toPx(),
                defaultNodeWidth = 120.dp.toPx(),
                defaultNodeHeight = 60.dp.toPx()
            )
        }
        layoutNodes = TreeLayoutAlgorithm<T>(layoutConfig).layout(root, nodeSizes.toMap())
        treeBounds = layoutNodes.bounds()
    }

    ZoomPanContainer(state = zoomPanState, modifier = modifier) {
        Box(
            modifier = Modifier.size(
                width = with(density) { (treeBounds.width + treeBounds.x + 50f).toDp() },
                height = with(density) { (treeBounds.height + treeBounds.y + 50f).toDp() }
            )
        ) {
            val lineWidthPx = with(density) { config.lineWidth.toPx() }

            Canvas(modifier = Modifier.fillMaxSize()) {
                layoutNodes.forEach { parentLayout ->
                    if (parentLayout.node.hasChildren) {
                        val childLayouts = layoutNodes.filter { child ->
                            parentLayout.node.children.any { it.data == child.node.data }
                        }
                        childLayouts.forEach { childLayout ->
                            drawConnection(parentLayout, childLayout, config.lineColor, lineWidthPx, config.lineStyle)
                        }
                    }
                }
            }

            layoutNodes.forEach { layoutNode ->
                Box(
                    modifier = Modifier
                        .offset(
                            x = with(density) { layoutNode.left.toDp() },
                            y = with(density) { layoutNode.top.toDp() }
                        )
                        .onGloballyPositioned { coords ->
                            val size = IntSize(coords.size.width, coords.size.height)
                            if (nodeSizes[layoutNode.node.data] != size) {
                                nodeSizes[layoutNode.node.data] = size
                            }
                        }
                ) {
                    nodeContent(
                        layoutNode.node.data,
                        layoutNode.node.color,
                        layoutNode.node.backgroundRes
                    )
                }
            }
        }
    }
}
