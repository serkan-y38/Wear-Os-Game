package com.yilmaz.snakegame.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.yilmaz.snakegame.presentation.theme.SnakeGameTheme
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            WearApp()
        }
    }
}

@Composable
fun WearApp() {
    SnakeGameTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            TimeText()
            SnakeGame()
        }
    }
}

@Composable
fun SnakeGame() {
    val gameSpeed by remember {
        mutableLongStateOf(50L)
    }
    val snakeSpeed by remember {
        mutableLongStateOf(300L)
    }

    val gridSize = 16

    var gameId by remember {
        mutableIntStateOf(0)
    }
    var isGameOver by remember {
        mutableStateOf(false)
    }
    var currentDirection by remember {
        mutableStateOf(Direction.RIGHT)
    }
    var prevDirection by remember {
        mutableStateOf(Direction.RIGHT)
    }
    var snake by remember {
        mutableStateOf(listOf(Cell(0, 0)))
    }
    var food by remember {
        mutableStateOf(generateFood(snake, gridSize))
    }
    var lastMoveTime by remember {
        mutableLongStateOf(0L)
    }

    LaunchedEffect(gameId) {
        while (!isGameOver) {
            delay(gameSpeed)

            if (System.currentTimeMillis() - lastMoveTime >= snakeSpeed) {
                snake = moveSnake(
                    snake,
                    currentDirection,
                    gridSize
                )
                lastMoveTime = System.currentTimeMillis()
            }

            if (snake.first() == food) {
                food = generateFood(snake, gridSize)
                snake = growSnake(snake, currentDirection, gridSize)
            }

            isGameOver = checkGameOver(snake)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isGameOver) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    modifier = Modifier.padding(8.dp),
                    onClick = {
                        snake = listOf(Cell(0, 0))
                        currentDirection = Direction.RIGHT
                        prevDirection = Direction.RIGHT
                        food = generateFood(snake, gridSize)
                        isGameOver = false
                        gameId++
                    }) {
                    Text(text = "Restart")
                }
            }
        } else {
            if (currentDirection != prevDirection) {
                prevDirection = currentDirection
                GameBoard(
                    snake,
                    food,
                    gridSize,
                    currentDirection,
                    onDirectionChange = { d -> currentDirection = d })
            } else {
                GameBoard(
                    snake,
                    food,
                    gridSize,
                    currentDirection,
                    onDirectionChange = { d -> currentDirection = d })
            }
        }
    }
}

fun checkGameOver(snake: List<Cell>): Boolean {
    return snake.first() in snake.drop(1)
}

@Composable
fun GameBoard(
    snake: List<Cell>,
    food: Cell,
    gridSize: Int,
    currentDirection: Direction,
    onDirectionChange: (Direction) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Grid(snake, food, gridSize)
        SwipeControl(onDirectionChange, currentDirection)
    }
}

@Composable
fun Grid(snake: List<Cell>, food: Cell, gridSize: Int) {
    val cellSize = 8.dp

    Column(modifier = Modifier.background(color = MaterialTheme.colors.background)) {
        for (i in 0 until gridSize) {
            Row {
                for (j in 0 until gridSize) {
                    Box(
                        modifier = Modifier
                            .size(cellSize)
                            .border(border = BorderStroke(0.2.dp, MaterialTheme.colors.background))
                            .background(
                                when (Cell(j, i)) {
                                    in snake -> MaterialTheme.colors.primary
                                    food -> Color.Red
                                    else -> MaterialTheme.colors.surface
                                }
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun SwipeControl(
    onDirectionChange: (Direction) -> Unit,
    currentDirection: Direction
) {
    var direction by remember {
        mutableIntStateOf(-1)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val (x, y) = dragAmount

                        if (abs(x) > abs(y))
                            when {
                                x > 0 -> direction = 0
                                x < 0 -> direction = 1
                            }
                        else
                            when {
                                y > 0 -> direction = 2
                                y < 0 -> direction = 3
                            }
                    },
                    onDragEnd = {
                        when (direction) {
                            0 -> if (currentDirection != Direction.LEFT) onDirectionChange(Direction.RIGHT)
                            1 -> if (currentDirection != Direction.RIGHT) onDirectionChange(Direction.LEFT)
                            2 -> if (currentDirection != Direction.UP) onDirectionChange(Direction.DOWN)
                            3 -> if (currentDirection != Direction.DOWN) onDirectionChange(Direction.UP)
                        }
                    }
                )
            }
    )
}

fun moveSnake(
    snake: List<Cell>,
    direction: Direction,
    gridSize: Int
): List<Cell> {
    val head = snake.first()

    val newHead = when (direction) {
        Direction.UP -> {
            if (head.y <= 0) Cell(head.x, gridSize - 1)
            else Cell(head.x, head.y - 1)
        }

        Direction.DOWN -> {
            if (head.y > gridSize - 1) Cell(head.x, 0)
            else Cell(head.x, head.y + 1)

        }

        Direction.LEFT -> {
            if (head.x <= 0) Cell(gridSize - 1, head.y)
            else Cell(head.x - 1, head.y)

        }

        Direction.RIGHT -> {
            if (head.x > gridSize - 1) Cell(0, head.y)
            else Cell(head.x + 1, head.y)

        }
    }
    val newSnake = snake.toMutableList()
    newSnake.add(0, newHead)
    newSnake.removeAt(newSnake.size - 1)
    return newSnake
}

fun generateFood(snake: List<Cell>, gridSize: Int): Cell {
    val emptyCells = (0 until gridSize).flatMap { x ->
        (0 until gridSize).map { y -> Cell(x, y) }
    }.filter { it !in snake }
    return emptyCells[Random.nextInt(emptyCells.size)]
}

fun growSnake(
    snake: List<Cell>,
    direction: Direction,
    gridSize: Int
): List<Cell> {
    val growth = when (direction) {
        Direction.UP -> Cell(snake.first().x, (snake.first().y - 1 + gridSize) % gridSize)
        Direction.DOWN -> Cell(snake.first().x, (snake.first().y + 1) % gridSize)
        Direction.LEFT -> Cell((snake.first().x - 1 + gridSize) % gridSize, snake.first().y)
        Direction.RIGHT -> Cell((snake.first().x + 1) % gridSize, snake.first().y)
    }
    return listOf(growth) + snake
}