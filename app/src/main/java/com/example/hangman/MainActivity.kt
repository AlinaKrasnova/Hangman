package com.example.hangman

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hangman.ui.theme.HangmanTheme
import androidx.compose.ui.tooling.preview.Preview
import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextAlign

// Galvena programmas aktivitate
class MainActivity : ComponentActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        // Lietotaja interfeis
        setContent {
            HangmanTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        // Background visai spelei
                        painter = painterResource(id = R.drawable.background),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = androidx.compose.ui.graphics.Color.Transparent
                    ) {
                        HangmanApp(assetManager = assets)
                    }
                }
            }
        }
    }
}



// Funkcija, lai darboties ar words failu un nejauši izvelet vardu
suspend fun loadRandomWord(assetManager: AssetManager): String
{
    return withContext(Dispatchers.IO) {
        assetManager.open("words.txt").bufferedReader().useLines { lines ->
            lines.shuffled().first()
        }
    }
}

// Pirmais speles ekrans, kur lietotajs ievada vardu
@Composable
fun NameEntryScreen(onContinueClicked: (String) -> Unit)
{
    var playerName by remember { mutableStateOf("") }
    var showErrorSnackbar by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = playerName,
            onValueChange = { playerName = it },
            label = { Text("Enter your name") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Button aktivizejas tikai, ja lietotajs ievada vardu
        Button(
            onClick = {
                    onContinueClicked(playerName)
            },
            enabled = playerName.isNotBlank()
        ) {
            Text("Continue")
        }
    }
}

// Main menu ekrans ar režimu pogam, instrukciju un iespeju pabeigt speli
@Composable
fun HangmanApp(assetManager: AssetManager)
{
    var currentScreen by remember { mutableStateOf("NameEntry") }
    var playerName by remember { mutableStateOf("") }
    var correctWord by remember { mutableStateOf("") }
    var opponentName by remember { mutableStateOf("") }
    var currentPlayerIsGuesser by remember { mutableStateOf(true) }
    var playerScore = remember { mutableStateOf(0) }
    var opponentScore = remember { mutableStateOf(0) }

    fun resetPvPState()
    {
        opponentName = ""
        correctWord = ""
        playerScore.value = 0
        opponentScore.value = 0
        currentPlayerIsGuesser = true
    }

    when (currentScreen) {
        "NameEntry" -> NameEntryScreen(onContinueClicked = { name ->
            playerName = name
            currentScreen = "MainMenu"
        })
        "MainMenu" -> {
            resetPvPState()  // Lai atjaunojas vertibas
            MainMenu(
                playerName = playerName,
                onStartPvC = { currentScreen = "PvCGame" },
                onStartPvP = { currentScreen = "PvPGameSetup" },
                onShowInstructions = { currentScreen = "Instructions" },
                onExit = { currentScreen = "ExitConfirmation" }
            )
        }
        "PvCGame" -> PvCGameScreen(
            onBackToMainMenu = { currentScreen = "MainMenu" },
            assetManager = assetManager,
            playerName = playerName
        )
        "PvPGameSetup" -> if (opponentName.isBlank()) {
            PvPGameSetupScreen({ name, word ->
                opponentName = name
                correctWord = word
                currentScreen = "PvPGame"
            }, onBackToMainMenu = { currentScreen = "MainMenu" })
        } else {
            PvPWordSetupScreen({ word ->
                correctWord = word
                currentScreen = "PvPGame"
            }, if (currentPlayerIsGuesser) opponentName else playerName, onBackToMainMenu = { currentScreen = "MainMenu" })
        }
        "PvPGame" -> PvPGameScreen(
            playerName = playerName,
            opponentName = opponentName,
            secretWord = correctWord,
            playerScore = playerScore,
            opponentScore = opponentScore,
            isCurrentPlayerGuesser = currentPlayerIsGuesser,
            onBackToMainMenu = { currentScreen = "MainMenu" },
            onNextRound = {
                currentPlayerIsGuesser = !currentPlayerIsGuesser
                currentScreen = "PvPGameSetup"
            }
        )
        "Instructions" -> InstructionsScreen(onBack = { currentScreen = "MainMenu" })
        "ExitConfirmation" -> ExitConfirmation(
            onConfirmExit = { android.os.Process.killProcess(android.os.Process.myPid()) },
            onDismiss = { currentScreen = "MainMenu" }
        )
    }
}

@Composable
fun InstructionsScreen(onBack: () -> Unit)
{
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = onBack, modifier = Modifier.padding(bottom = 8.dp)) {
            Text(text = "BACK TO MAIN MENU")
        }
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("Welcome to Hangman! Here's how to play:\n\n")
                }
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("START PvC (Player vs. Computer):\n")
                }
                append("Guess the word selected by the computer within 10 attempts before the hangman is fully drawn. " +
                        "If you guess correctly, you gain 100 points and the computer loses 10 points. If incorrect, " +
                        "the computer gains 100 points and you lose 10 points. Points do not drop below zero. " +
                        "Press \"FINISH GAME\" to end and see who won.\n\n")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("START PvP (Player vs. Player):\n")
                }
                append("Players take turns guessing words chosen by each other, each having 10 attempts to guess correctly. " +
                        "A correct guess adds 100 points to the guesser and subtracts 10 from the word setter. " +
                        "An incorrect guess awards the word setter 100 points, while the guesser loses 10 points. " +
                        "Points do not drop below zero. Press \"FINISH GAME\" to conclude and determine the winner.\n\n")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("EXIT:\n")
                }
                append("Select to exit the game mode.\n\n")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("Good luck and enjoy the game!")
                }
            },
            fontSize = 16.sp
        )
    }
}

@Composable
fun PvCGameScreen(
    onBackToMainMenu: () -> Unit,
    assetManager: AssetManager,
    playerName: String
) {
    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    var playerScore by remember { mutableStateOf(0) }
    var computerScore by remember { mutableStateOf(0) }
    var gallowsImageIndex by remember { mutableStateOf(0) }
    var guessedLetters by remember { mutableStateOf(setOf<Char>()) }
    var gameFinished by remember { mutableStateOf(false) }
    var correctWord by remember { mutableStateOf("") }
    var endGame by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(key1 = "load_word") {
        coroutineScope.launch {
            correctWord = loadRandomWord(assetManager).toUpperCase()
        }
    }

    if (!endGame)
    {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            ScoreDisplay(playerName, playerScore, computerScore)
            GallowsImage(index = gallowsImageIndex)
            WordDisplay(correctWord = correctWord, guessedLetters = guessedLetters)

            if (!gameFinished)
            {
                AlphabetButtons(alphabet = alphabet, guessedLetters = guessedLetters, onLetterClicked = { letter ->
                    guessedLetters += letter

                    if (correctWord.contains(letter))
                    {
                        if (correctWord.all { it in guessedLetters })
                        {
                            playerScore += 100  // Par uzvaru speletajs dabuja +100
                            gameFinished = true
                        }
                    } else
                    {
                        gallowsImageIndex++
                        if (gallowsImageIndex >= 10)
                        {
                            gameFinished = true
                            computerScore += 100  // Par uzvaru dators dabuja +100
                            playerScore = maxOf(0, playerScore - 10)  // Par zaudejumu -10
                        }
                    }
                })
            }

            if (gameFinished)
            {
                Text(
                    text = if (correctWord.all { guessedLetters.contains(it) })
                        "You WIN, my congratulations!!!" else {
                        "The hidden word was: $correctWord\nYou LOSE, please try again."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onBackToMainMenu) {
                    Text("BACK TO MAIN MENU")
                }
                if (gameFinished)
                {
                    Button(onClick = {
                        gallowsImageIndex = 0
                        guessedLetters = setOf()
                        gameFinished = false
                        coroutineScope.launch {
                            correctWord = loadRandomWord(assetManager).toUpperCase()
                        }
                    }) {
                        Text("NEXT ROUND")
                    }
                }
            }

            Button(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), onClick = { endGame = true }) {
                Text("FINISH THE GAME")
            }
        }
    } else
    {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            ScoreDisplay(playerName, playerScore, computerScore)
            val resultText = if (playerScore > computerScore) {
                "The player $playerName won the game"
            } else if (computerScore > playerScore)
            {
                "The computer won the game"
            } else
            {
                "Draw, friendship wins"
            }
            Text(resultText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Button(onClick = onBackToMainMenu) {
                Text("BACK TO MAIN MENU")
            }
        }
    }
}



@Composable
fun PvPGameSetupScreen(onGameStart: (opponentName: String, secretWord: String) -> Unit, onBackToMainMenu: () -> Unit)
{
    var opponentName by remember { mutableStateOf("") }
    var secretWord by remember { mutableStateOf("") }
    var showErrorSnackbar by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = opponentName,
            onValueChange = { opponentName = it },
            label = { Text("Enter opponent's name") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = secretWord,
            onValueChange = {
                if (it.all { char -> char.isLetter() }) {
                    secretWord = it
                }
            },
            label = { Text("Enter a word to guess") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (opponentName.isBlank() || secretWord.isBlank()) {
                    showErrorSnackbar = true
                } else {
                    onGameStart(opponentName, secretWord.toUpperCase())
                }
            },
            enabled = opponentName.isNotBlank() && secretWord.isNotBlank()
        ) {
            Text("START GAME")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            opponentName = ""
            secretWord = ""
            onBackToMainMenu()
        }) {
            Text("BACK TO MAIN MENU")
        }
        if (showErrorSnackbar)
        {
            Snackbar(
                action = {
                    Button(onClick = { showErrorSnackbar = false }) {
                        Text("OK")
                    }
                }
            ) {
                Text("Both fields must be filled to continue.")
            }
        }
    }
}



@Composable
fun PvPWordSetupScreen(onGameStart: (String) -> Unit, currentPlayer: String, onBackToMainMenu: () -> Unit)
{
    var secretWord by remember { mutableStateOf("") }
    var showErrorSnackbar by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "It's $currentPlayer's turn to choose a word",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        TextField(
            value = secretWord,
            onValueChange = {
                if (it.all { char -> char.isLetter() })
                {
                    secretWord = it
                }
            },
            label = { Text("Enter a word to guess") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (secretWord.isBlank())
                {
                    showErrorSnackbar = true
                } else
                {
                    onGameStart(secretWord.toUpperCase())
                }
            },
            enabled = secretWord.isNotBlank()
        ) {
            Text("START GAME")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            secretWord = ""
            onBackToMainMenu()
        }) {
            Text("BACK TO MAIN MENU")
        }
        if (showErrorSnackbar)
        {
            Snackbar(
                action = {
                    Button(onClick = { showErrorSnackbar = false }) {
                        Text("OK")
                    }
                }
            ) {
                Text("Please enter a word to continue.")
            }
        }
    }
}


@Composable
fun PvPGameScreen(
    playerName: String,
    opponentName: String,
    secretWord: String,
    playerScore: MutableState<Int>,
    opponentScore: MutableState<Int>,
    isCurrentPlayerGuesser: Boolean,
    onBackToMainMenu: () -> Unit,
    onNextRound: () -> Unit
) {
    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    var gallowsImageIndex by remember { mutableStateOf(0) }
    var guessedLetters by remember { mutableStateOf(setOf<Char>()) }
    var gameFinished by remember { mutableStateOf(false) }
    var endGame by remember { mutableStateOf(false) }

    if (!endGame)
    {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            ScoreDisplayPvP(playerName, playerScore.value, opponentName, opponentScore.value)
            GallowsImage(index = gallowsImageIndex)
            WordDisplay(secretWord, guessedLetters)

            if (!gameFinished)
            {
                AlphabetButtons(alphabet, guessedLetters) { letter ->
                    guessedLetters += letter
                    if (secretWord.contains(letter))
                    {
                        if (secretWord.all { guessedLetters.contains(it) })
                        {
                            if (isCurrentPlayerGuesser)
                            {
                                playerScore.value += 100
                                opponentScore.value = (opponentScore.value - 10).coerceAtLeast(0)
                            } else
                            {
                                opponentScore.value += 100
                                playerScore.value = (playerScore.value - 10).coerceAtLeast(0)
                            }
                            gameFinished = true
                        }
                    } else
                    {
                        gallowsImageIndex++
                        if (gallowsImageIndex >= 10)
                        {
                            gameFinished = true
                            if (!isCurrentPlayerGuesser)
                            {
                                playerScore.value += 100
                            } else
                            {
                                opponentScore.value += 100
                            }
                        }
                    }
                }
            }

            if (gameFinished)
            {
                Text(
                    text = if (secretWord.all { guessedLetters.contains(it) })
                        "You WIN, my congratulations!!!" else {
                        "The hidden word was: $secretWord\nYou LOSE, please try again."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onBackToMainMenu) {
                    Text("BACK TO MAIN MENU")
                }
                if (gameFinished)
                {
                    Button(onClick =
                    {
                        gallowsImageIndex = 0
                        guessedLetters = setOf()
                        gameFinished = false
                        onNextRound()
                    }) {
                        Text("NEXT ROUND")
                    }
                }
            }
            Button(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), onClick = { endGame = true })
            {
                Text("FINISH THE GAME")
            }
        }
    } else
    {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            ScoreDisplayPvP(playerName, playerScore.value, opponentName, opponentScore.value)
            val resultText = if (playerScore.value > opponentScore.value) {
                "The player $playerName won the game"
            } else if (opponentScore.value > playerScore.value) {
                "The player $opponentName won the game"
            } else {
                "Draw, friendship wins"
            }
            Text(resultText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Button(onClick = onBackToMainMenu)
            {
                Text("BACK TO MAIN MENU")
            }
        }
    }
}


@Composable
fun ScoreDisplay(playerName: String, playerScore: Int, computerScore: Int)
{
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$playerName's Score: $playerScore",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Computer's Score: $computerScore",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ScoreDisplayPvP(playerName: String, playerScore: Int, opponentName: String, opponentScore: Int)
{
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$playerName's Score: $playerScore",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "$opponentName's Score: $opponentScore",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}



@Composable
fun GallowsImage(index: Int)
{
    val imageName = when (index)
    {
        0 -> R.drawable.hangman1
        1 -> R.drawable.hangman2
        2 -> R.drawable.hangman3
        3 -> R.drawable.hangman4
        4 -> R.drawable.hangman5
        5 -> R.drawable.hangman6
        6 -> R.drawable.hangman7
        7 -> R.drawable.hangman8
        8 -> R.drawable.hangman9
        9 -> R.drawable.hangman10
        else -> R.drawable.hangman11 // Pedeja bilde, kad ir parsniegts kļudu limits
    }
    Image(
        painter = painterResource(id = imageName),
        contentDescription = "Gallows",
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .padding(horizontal = 16.dp)
    )
}

@Composable
fun WordDisplay(correctWord: String, guessedLetters: Set<Char>)
{
    val displayWord = buildString {
        for (char in correctWord) {
            append(if (guessedLetters.contains(char)) "$char " else "_ ")
        }
    }.trim()

    Text(
        text = displayWord,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .padding(vertical = 16.dp)
    )
}

@Composable
fun AlphabetButtons(alphabet: String, guessedLetters: Set<Char>, onLetterClicked: (Char) -> Unit)
{
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        contentPadding = PaddingValues(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        items(alphabet.length) { index ->
            val letter = alphabet[index]
            if (letter !in guessedLetters)
            {
                Button(
                    onClick = { onLetterClicked(letter) },
                    modifier = Modifier
                        .width(26.dp)
                        .height(40.dp)
                        .padding(2.dp)
                ) {
                    Text(text = letter.toString())
                }
            }
        }
    }
}

@Composable
fun GradientButton(
    onClick: () -> Unit,
    buttonText: String,
    gradientColors: List<Color>,
    cornerRadius: Dp = 12.dp
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 8.dp),
        contentPadding = PaddingValues(),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(cornerRadius)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(colors = gradientColors),
                    shape = RoundedCornerShape(cornerRadius)
                )
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = buttonText,
                fontSize = 20.sp,
                color = Color.White
            )
        }
    }
}

@Composable
fun MainMenu(
    playerName: String,
    onStartPvC: () -> Unit,
    onStartPvP: () -> Unit,
    onShowInstructions: () -> Unit,
    onExit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val greeting = "Welcome, $playerName, to the \nHangman game!"
        Text(greeting, fontWeight = FontWeight.Bold, fontSize = 28.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth() )
        Spacer(Modifier.height(16.dp))
        val gamemode ="Please choose game mode"
        Text(gamemode,fontSize = 20.sp)
        Spacer(Modifier.height(16.dp))

        GradientButton(
            onClick = onStartPvC,
            buttonText = "START PvC",
            gradientColors = listOf(Color(0xFF7B5EAD), Color(0xFF222F79))
        )
        Spacer(Modifier.height(8.dp))
        GradientButton(
            onClick = onStartPvP,
            buttonText = "START PvP",
            gradientColors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0))
        )
        Spacer(Modifier.height(8.dp))
        GradientButton(
            onClick = onShowInstructions,
            buttonText = "INSTRUCTIONS",
            gradientColors = listOf(Color(0xFF7B5EAD), Color(0xFF222F79))
        )
        Spacer(Modifier.height(8.dp))
        GradientButton(
            onClick = onExit,
            buttonText = "EXIT",
            gradientColors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0))
        )
    }
}

@Composable
fun ExitConfirmation(onConfirmExit: () -> Unit, onDismiss: () -> Unit)
{
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onConfirmExit) {
                Text("YES")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("NO")
            }
        },
        title = { Text("Exit Confirmation") },
        text = { Text("Do you really want to exit?") }
    )
}

@Preview(showBackground = true)
@Composable
fun MainMenuPreview() {
    HangmanTheme {
        MainMenu(
            playerName = "PlayerOne",
            onStartPvC = { },
            onStartPvP = { },
            onShowInstructions = { },
            onExit = { }
        )
    }
}
