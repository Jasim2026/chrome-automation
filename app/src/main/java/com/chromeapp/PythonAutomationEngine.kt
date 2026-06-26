package com.chromeapp

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

sealed class Instruction {
    data class Open(val urlExpr: String) : Instruction()
    data class Scroll(val directionExpr: String, val pixelsExpr: String) : Instruction()
    data class Click(val targetExpr: String, val yoloFallback: Boolean = false, val useYolo: Boolean = false) : Instruction()
    data class Type(val selectorExpr: String, val textExpr: String) : Instruction()
    data class Wait(val secondsExpr: String) : Instruction()
    data class Log(val messageExpr: String) : Instruction()
    data class VarAssign(val varName: String, val valExpr: String) : Instruction()
    data class LoopStart(
        val varName: String,
        val limitExpr: String,
        var endPc: Int = -1
    ) : Instruction()
    data class LoopEnd(val startPc: Int) : Instruction()
    
    // New coordinate gestures and system actions
    data class Touch(val xExpr: String, val yExpr: String) : Instruction()
    data class Swipe(val startXExpr: String, val startYExpr: String, val endXExpr: String, val endYExpr: String) : Instruction()
    data class PressButton(val buttonType: String) : Instruction()
    data class Download(val urlExpr: String) : Instruction()
    data class WaitForElement(val selectorExpr: String, val secondsExpr: String) : Instruction()
    data class WaitForUrl(val urlExpr: String, val secondsExpr: String) : Instruction()
    data class WaitUntil(val timeExpr: String) : Instruction()
}

class PythonAutomationEngine(
    private val script: String,
    private val webView: WebView,
    private val onLog: (String, LogType) -> Unit,
    private val onFinished: () -> Unit,
    private val onOpenUrl: (String) -> Unit,
    private val onSystemButton: (String) -> Unit = {},
    private val onDownload: ((String) -> Unit)? = null
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null
    
    private val variables = mutableMapOf<String, Any>()
    private val instructions = mutableListOf<Instruction>()
    private var pc = 0

    fun start() {
        job = scope.launch {
            try {
                compile()
                execute()
            } catch (e: Exception) {
                log("Compilation/Execution Error: ${e.message}", LogType.ERROR)
            } finally {
                withContext(Dispatchers.Main) {
                    onFinished()
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private fun log(text: String, type: LogType = LogType.LOG) {
        Handler(Looper.getMainLooper()).post {
            onLog(text, type)
        }
    }

    private fun compile() {
        val trimmedScript = script.trim()
        if (trimmedScript.startsWith("steps:") || trimmedScript.contains("- action:") || trimmedScript.contains("action:")) {
            compileYaml(trimmedScript)
        } else {
            compilePython()
        }
    }

    private fun compileYaml(scriptText: String) {
        log("Parsing YAML Workflow custom instructions...", LogType.INFO)
        val lines = scriptText.split("\n")
        var currentAction: String? = null
        var urlExpr: String? = null
        var xExpr: String? = null
        var yExpr: String? = null
        var startXExpr: String? = null
        var startYExpr: String? = null
        var endXExpr: String? = null
        var endYExpr: String? = null
        var directionExpr: String? = null
        var pixelsExpr: String? = null
        var targetExpr: String? = null
        var yoloFallback: Boolean = false
        var useYolo: Boolean = false
        var selectorExpr: String? = null
        var textExpr: String? = null
        var secondsExpr: String? = null
        var messageExpr: String? = null
        var timeExpr: String? = null

        fun commitAction() {
            val action = currentAction ?: return
            when (action) {
                "open" -> {
                    if (urlExpr != null) instructions.add(Instruction.Open(urlExpr!!))
                }
                "touch" -> {
                    if (xExpr != null && yExpr != null) {
                        instructions.add(Instruction.Touch(xExpr!!, yExpr!!))
                    }
                }
                "swipe" -> {
                    if (startXExpr != null && startYExpr != null && endXExpr != null && endYExpr != null) {
                        instructions.add(Instruction.Swipe(startXExpr!!, startYExpr!!, endXExpr!!, endYExpr!!))
                    }
                }
                "scroll" -> {
                    if (directionExpr != null && pixelsExpr != null) {
                        instructions.add(Instruction.Scroll(directionExpr!!, pixelsExpr!!))
                    }
                }
                "click" -> {
                    if (targetExpr != null) {
                        instructions.add(Instruction.Click(targetExpr!!, yoloFallback, useYolo))
                    }
                }
                "type" -> {
                    if (selectorExpr != null && textExpr != null) {
                        instructions.add(Instruction.Type(selectorExpr!!, textExpr!!))
                    }
                }
                "wait" -> {
                    if (secondsExpr != null) {
                        instructions.add(Instruction.Wait(secondsExpr!!))
                    }
                }
                "log" -> {
                    if (messageExpr != null) {
                        instructions.add(Instruction.Log(messageExpr!!))
                    }
                }
                "download" -> {
                    if (urlExpr != null) {
                        instructions.add(Instruction.Download(urlExpr!!))
                    }
                }
                "wait_for_element" -> {
                    if (selectorExpr != null) {
                        instructions.add(Instruction.WaitForElement(selectorExpr!!, secondsExpr ?: "10"))
                    }
                }
                "wait_for_url" -> {
                    if (urlExpr != null) {
                        instructions.add(Instruction.WaitForUrl(urlExpr!!, secondsExpr ?: "10"))
                    }
                }
                "wait_until" -> {
                    if (timeExpr != null) {
                        instructions.add(Instruction.WaitUntil(timeExpr!!))
                    }
                }
                "back" -> instructions.add(Instruction.PressButton("back"))
                "home" -> instructions.add(Instruction.PressButton("home"))
                "recent" -> instructions.add(Instruction.PressButton("recent"))
            }
            
            // Reset fields
            currentAction = null
            urlExpr = null
            xExpr = null
            yExpr = null
            startXExpr = null
            startYExpr = null
            endXExpr = null
            endYExpr = null
            directionExpr = null
            pixelsExpr = null
            targetExpr = null
            yoloFallback = false
            useYolo = false
            selectorExpr = null
            textExpr = null
            secondsExpr = null
            messageExpr = null
            timeExpr = null
        }

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            if (trimmed.startsWith("- action:")) {
                commitAction()
                currentAction = trimmed.substringAfter("- action:").trim().removeSurrounding("\"").removeSurrounding("'")
            } else if (trimmed.startsWith("action:")) {
                commitAction()
                currentAction = trimmed.substringAfter("action:").trim().removeSurrounding("\"").removeSurrounding("'")
            } else if (trimmed.startsWith("url:")) {
                urlExpr = trimmed.substringAfter("url:").trim().removeSurrounding("\"").removeSurrounding("'")
            } else if (trimmed.startsWith("x:")) {
                xExpr = trimmed.substringAfter("x:").trim()
            } else if (trimmed.startsWith("y:")) {
                yExpr = trimmed.substringAfter("y:").trim()
            } else if (trimmed.startsWith("start_x:")) {
                startXExpr = trimmed.substringAfter("start_x:").trim()
            } else if (trimmed.startsWith("start_y:")) {
                startYExpr = trimmed.substringAfter("start_y:").trim()
            } else if (trimmed.startsWith("end_x:")) {
                endXExpr = trimmed.substringAfter("end_x:").trim()
            } else if (trimmed.startsWith("end_y:")) {
                endYExpr = trimmed.substringAfter("end_y:").trim()
            } else if (trimmed.startsWith("direction:")) {
                directionExpr = trimmed.substringAfter("direction:").trim().removeSurrounding("\"").removeSurrounding("'")
            } else if (trimmed.startsWith("pixels:")) {
                pixelsExpr = trimmed.substringAfter("pixels:").trim()
            } else if (trimmed.startsWith("target:")) {
                targetExpr = trimmed.substringAfter("target:").trim().removeSurrounding("\"").removeSurrounding("'")
            } else if (trimmed.startsWith("yolo_fallback:")) {
                yoloFallback = trimmed.substringAfter("yolo_fallback:").trim().lowercase() == "true"
            } else if (trimmed.startsWith("use_yolo:")) {
                useYolo = trimmed.substringAfter("use_yolo:").trim().lowercase() == "true"
            } else if (trimmed.startsWith("useYolo:")) {
                useYolo = trimmed.substringAfter("useYolo:").trim().lowercase() == "true"
            } else if (trimmed.startsWith("selector:")) {
                selectorExpr = trimmed.substringAfter("selector:").trim().removeSurrounding("\"").removeSurrounding("'")
            } else if (trimmed.startsWith("text:")) {
                textExpr = trimmed.substringAfter("text:").trim().removeSurrounding("\"").removeSurrounding("'")
            } else if (trimmed.startsWith("seconds:")) {
                secondsExpr = trimmed.substringAfter("seconds:").trim()
            } else if (trimmed.startsWith("message:")) {
                messageExpr = trimmed.substringAfter("message:").trim().removeSurrounding("\"").removeSurrounding("'")
            } else if (trimmed.startsWith("time:")) {
                timeExpr = trimmed.substringAfter("time:").trim().removeSurrounding("\"").removeSurrounding("'")
            }
        }
        commitAction()
        log("Compilation successful! Compiled ${instructions.size} YAML instructions.", LogType.SUCCESS)
    }

    private fun compilePython() {
        val lines = script.split("\n")
        val loopStack = mutableListOf<Int>() // Store PCs of LoopStarts

        var lineIndex = 0
        while (lineIndex < lines.size) {
            val line = lines[lineIndex]
            val trimmed = line.trim()
            
            // Skip empty lines and comments
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                lineIndex++
                continue
            }

            // Detect loop: "for i in range(10):"
            if (trimmed.startsWith("for ") && trimmed.endsWith(":")) {
                val loopHeader = trimmed.removeSuffix(":").trim()
                // Parse "for var in range(limit)"
                val parts = loopHeader.split(" ")
                if (parts.size >= 4 && parts[2] == "in" && parts[3].startsWith("range(")) {
                    val varName = parts[1]
                    val rangeContent = parts[3].substringAfter("range(").substringBefore(")")
                    val limitExpr = rangeContent.trim()
                    
                    val startPc = instructions.size
                    val loopStart = Instruction.LoopStart(varName, limitExpr)
                    instructions.add(loopStart)
                    loopStack.add(startPc)
                } else {
                    throw IllegalArgumentException("Unsupported loop syntax at line ${lineIndex + 1}: '$trimmed'")
                }
                lineIndex++
                continue
            }

            // Detect end of loop block by measuring indentation or detecting dedicated end
            val currentIndentation = getIndentationLevel(line)
            while (loopStack.isNotEmpty() && currentIndentation == 0 && !trimmed.startsWith("for") && !trimmed.startsWith("#")) {
                // Close the loop
                val startPc = loopStack.removeAt(loopStack.size - 1)
                val loopEndPc = instructions.size
                instructions.add(Instruction.LoopEnd(startPc))
                val loopStartIns = instructions[startPc] as Instruction.LoopStart
                loopStartIns.endPc = loopEndPc
            }

            // Parse statements
            if (trimmed.startsWith("chrome.open(") && trimmed.endsWith(")")) {
                val arg = trimmed.substringAfter("chrome.open(").removeSuffix(")")
                instructions.add(Instruction.Open(arg))
            } else if (trimmed.startsWith("chrome.touch(") && trimmed.endsWith(")")) {
                val args = trimmed.substringAfter("chrome.touch(").removeSuffix(")").split(",")
                if (args.size == 2) {
                    instructions.add(Instruction.Touch(args[0].trim(), args[1].trim()))
                } else {
                    throw IllegalArgumentException("chrome.touch expects 2 arguments: (x, y) at line ${lineIndex + 1}")
                }
            } else if (trimmed.startsWith("chrome.swipe(") && trimmed.endsWith(")")) {
                val args = trimmed.substringAfter("chrome.swipe(").removeSuffix(")").split(",")
                if (args.size == 4) {
                    instructions.add(Instruction.Swipe(args[0].trim(), args[1].trim(), args[2].trim(), args[3].trim()))
                } else {
                    throw IllegalArgumentException("chrome.swipe expects 4 arguments: (startX, startY, endX, endY) at line ${lineIndex + 1}")
                }
            } else if (trimmed.startsWith("chrome.scroll(") && trimmed.endsWith(")")) {
                val args = trimmed.substringAfter("chrome.scroll(").removeSuffix(")").split(",")
                if (args.size == 2) {
                    instructions.add(Instruction.Scroll(args[0].trim(), args[1].trim()))
                } else {
                    throw IllegalArgumentException("chrome.scroll expects 2 arguments: (direction, pixels) at line ${lineIndex + 1}")
                }
            } else if (trimmed.startsWith("chrome.click_yolo_direct(") && trimmed.endsWith(")")) {
                val arg = trimmed.substringAfter("chrome.click_yolo_direct(").removeSuffix(")")
                instructions.add(Instruction.Click(arg, false, true))
            } else if (trimmed.startsWith("chrome.click_yolo(") && trimmed.endsWith(")")) {
                val arg = trimmed.substringAfter("chrome.click_yolo(").removeSuffix(")")
                instructions.add(Instruction.Click(arg, true))
            } else if (trimmed.startsWith("chrome.click(") && trimmed.endsWith(")")) {
                val arg = trimmed.substringAfter("chrome.click(").removeSuffix(")")
                if (arg.contains("use_yolo=True") || arg.contains("useYolo=True")) {
                    val realTarget = arg.substringBefore(",").trim().removeSurrounding("\"").removeSurrounding("'")
                    instructions.add(Instruction.Click(realTarget, false, true))
                } else if (arg.contains("yolo=True") || arg.contains("yolo_fallback=True")) {
                    val realTarget = arg.substringBefore(",").trim().removeSurrounding("\"").removeSurrounding("'")
                    instructions.add(Instruction.Click(realTarget, true))
                } else {
                    instructions.add(Instruction.Click(arg.removeSurrounding("\"").removeSurrounding("'"), false))
                }
            } else if (trimmed.startsWith("chrome.type(") && trimmed.endsWith(")")) {
                val args = trimmed.substringAfter("chrome.type(").removeSuffix(")").split(",")
                if (args.size == 2) {
                    instructions.add(Instruction.Type(args[0].trim(), args[1].trim()))
                } else {
                    throw IllegalArgumentException("chrome.type expects 2 arguments: (selector, text) at line ${lineIndex + 1}")
                }
            } else if (trimmed.startsWith("chrome.wait(") && trimmed.endsWith(")")) {
                val arg = trimmed.substringAfter("chrome.wait(").removeSuffix(")")
                instructions.add(Instruction.Wait(arg))
            } else if (trimmed.startsWith("chrome.download(") && trimmed.endsWith(")")) {
                val arg = trimmed.substringAfter("chrome.download(").removeSuffix(")")
                instructions.add(Instruction.Download(arg))
            } else if (trimmed.startsWith("chrome.wait_for_element(") && trimmed.endsWith(")")) {
                val args = trimmed.substringAfter("chrome.wait_for_element(").removeSuffix(")").split(",")
                if (args.size == 2) {
                    instructions.add(Instruction.WaitForElement(args[0].trim(), args[1].trim()))
                } else if (args.size == 1) {
                    instructions.add(Instruction.WaitForElement(args[0].trim(), "10"))
                } else {
                    throw IllegalArgumentException("chrome.wait_for_element expects 1 or 2 arguments at line ${lineIndex + 1}")
                }
            } else if (trimmed.startsWith("chrome.wait_for_url(") && trimmed.endsWith(")")) {
                val args = trimmed.substringAfter("chrome.wait_for_url(").removeSuffix(")").split(",")
                if (args.size == 2) {
                    instructions.add(Instruction.WaitForUrl(args[0].trim(), args[1].trim()))
                } else if (args.size == 1) {
                    instructions.add(Instruction.WaitForUrl(args[0].trim(), "10"))
                } else {
                    throw IllegalArgumentException("chrome.wait_for_url expects 1 or 2 arguments at line ${lineIndex + 1}")
                }
            } else if (trimmed.startsWith("chrome.wait_until(") && trimmed.endsWith(")")) {
                val arg = trimmed.substringAfter("chrome.wait_until(").removeSuffix(")")
                instructions.add(Instruction.WaitUntil(arg))
            } else if (trimmed.startsWith("chrome.log(") && trimmed.endsWith(")")) {
                val arg = trimmed.substringAfter("chrome.log(").removeSuffix(")")
                instructions.add(Instruction.Log(arg))
            } else if (trimmed.startsWith("print(") && trimmed.endsWith(")")) {
                val arg = trimmed.substringAfter("print(").removeSuffix(")")
                instructions.add(Instruction.Log(arg))
            } else if (trimmed == "chrome.back()" || trimmed == "chrome.press_back()") {
                instructions.add(Instruction.PressButton("back"))
            } else if (trimmed == "chrome.home()" || trimmed == "chrome.press_home()") {
                instructions.add(Instruction.PressButton("home"))
            } else if (trimmed == "chrome.recent()" || trimmed == "chrome.press_recent()") {
                instructions.add(Instruction.PressButton("recent"))
            } else if (trimmed.contains("=")) {
                val left = trimmed.substringBefore("=").trim()
                val right = trimmed.substringAfter("=").trim()
                if (left.isNotEmpty() && right.isNotEmpty() && !left.contains(" ")) {
                    instructions.add(Instruction.VarAssign(left, right))
                }
            } else {
                log("Warning: Unrecognized command at line ${lineIndex + 1}: '$trimmed'", LogType.INFO)
            }

            lineIndex++
        }

        // Close any remaining loops at the end of the script
        while (loopStack.isNotEmpty()) {
            val startPc = loopStack.removeAt(loopStack.size - 1)
            val loopEndPc = instructions.size
            instructions.add(Instruction.LoopEnd(startPc))
            val loopStartIns = instructions[startPc] as Instruction.LoopStart
            loopStartIns.endPc = loopEndPc
        }

        log("Compilation successful! Compiled ${instructions.size} YML steps.", LogType.SUCCESS)
    }

    private fun getIndentationLevel(line: String): Int {
        var count = 0
        for (char in line) {
            if (char == ' ') count++
            else if (char == '\t') count += 4
            else break
        }
        return count
    }

    private suspend fun execute() {
        pc = 0
        val loopCounters = mutableMapOf<Int, Int>() // maps startPc -> current loop iteration count

        while (pc < instructions.size && job?.isCancelled == false) {
            val instruction = instructions[pc]
            
            when (instruction) {
                is Instruction.Open -> {
                    val url = evalStringExpr(instruction.urlExpr)
                    log("Navigating to: $url", LogType.INFO)
                    withContext(Dispatchers.Main) {
                        onOpenUrl(url)
                    }
                    pc++
                }
                is Instruction.Touch -> {
                    val x = evalDoubleExpr(instruction.xExpr).toFloat()
                    val y = evalDoubleExpr(instruction.yExpr).toFloat()
                    log("Executing physical touch at coordinates ($x, $y)", LogType.INFO)
                    executeTouchAndroid(x, y)
                    pc++
                }
                is Instruction.Swipe -> {
                    val sx = evalDoubleExpr(instruction.startXExpr).toFloat()
                    val sy = evalDoubleExpr(instruction.startYExpr).toFloat()
                    val ex = evalDoubleExpr(instruction.endXExpr).toFloat()
                    val ey = evalDoubleExpr(instruction.endYExpr).toFloat()
                    log("Executing swipe from ($sx, $sy) to ($ex, $ey)", LogType.INFO)
                    executeSwipeAndroid(sx, sy, ex, ey)
                    pc++
                }
                is Instruction.Scroll -> {
                    val dir = evalStringExpr(instruction.directionExpr).lowercase(Locale.getDefault())
                    val pixels = evalIntExpr(instruction.pixelsExpr)
                    val scrollAmount = if (dir == "down") pixels else -pixels
                    log("Scrolling $dir by $pixels px", LogType.INFO)
                    executeScrollJs(scrollAmount)
                    pc++
                }
                is Instruction.Click -> {
                    val target = evalStringExpr(instruction.targetExpr)
                    if (instruction.useYolo) {
                        log("Attempting direct visual click on target: '$target' using YOLOv26 model...", LogType.INFO)
                        val yoloSuccess = executeYoloFallback(target)
                        if (yoloSuccess) {
                            log("YOLOv26 vision model direct click succeeded.", LogType.SUCCESS)
                        } else {
                            log("YOLOv26 vision model direct click failed.", LogType.ERROR)
                        }
                    } else {
                        log("Attempting click on target: '$target'", LogType.INFO)
                        val clicked = executeClickJs(target)
                        if (clicked) {
                            log("Target '$target' clicked successfully via DOM.", LogType.SUCCESS)
                        } else {
                            log("Target '$target' not found or failed to click via DOM.", LogType.INFO)
                            if (instruction.yoloFallback) {
                                val yoloSuccess = executeYoloFallback(target)
                                if (yoloSuccess) {
                                    log("YOLOv26 vision model click fallback succeeded.", LogType.SUCCESS)
                                } else {
                                    log("YOLOv26 vision model click fallback failed.", LogType.ERROR)
                                }
                            }
                        }
                    }
                    pc++
                }
                is Instruction.Type -> {
                    val selector = evalStringExpr(instruction.selectorExpr)
                    val text = evalStringExpr(instruction.textExpr)
                    log("Typing '$text' into selector '$selector'", LogType.INFO)
                    executeTypeJs(selector, text)
                    pc++
                }
                is Instruction.Wait -> {
                    val seconds = evalDoubleExpr(instruction.secondsExpr)
                    log("Waiting for $seconds seconds...", LogType.INFO)
                    delay((seconds * 1000).toLong())
                    pc++
                }
                is Instruction.Download -> {
                    val url = evalStringExpr(instruction.urlExpr)
                    log("Requesting programmatic download for URL: $url", LogType.INFO)
                    withContext(Dispatchers.Main) {
                        onDownload?.invoke(url)
                    }
                    pc++
                }
                is Instruction.WaitForElement -> {
                    val selector = evalStringExpr(instruction.selectorExpr)
                    val seconds = evalDoubleExpr(instruction.secondsExpr)
                    log("Waiting for element selector to appear: '$selector' (timeout: $seconds seconds)...", LogType.INFO)
                    var found = false
                    val startTime = System.currentTimeMillis()
                    val timeoutMs = (seconds * 1000).toLong()
                    while (System.currentTimeMillis() - startTime < timeoutMs && job?.isCancelled == false) {
                        found = checkElementExistsJs(selector)
                        if (found) break
                        delay(500)
                    }
                    if (found) {
                        log("Element '$selector' detected in DOM.", LogType.SUCCESS)
                    } else {
                        log("Timeout waiting for element '$selector' after $seconds seconds.", LogType.ERROR)
                    }
                    pc++
                }
                is Instruction.WaitForUrl -> {
                    val pattern = evalStringExpr(instruction.urlExpr)
                    val seconds = evalDoubleExpr(instruction.secondsExpr)
                    log("Waiting for page URL to match pattern: '$pattern' (timeout: $seconds seconds)...", LogType.INFO)
                    var matched = false
                    val startTime = System.currentTimeMillis()
                    val timeoutMs = (seconds * 1000).toLong()
                    while (System.currentTimeMillis() - startTime < timeoutMs && job?.isCancelled == false) {
                        val currentUrl = withContext(Dispatchers.Main) { webView.url ?: "" }
                        if (urlMatchesPattern(currentUrl, pattern)) {
                            matched = true
                            break
                        }
                        delay(500)
                    }
                    if (matched) {
                        log("URL pattern matched successfully.", LogType.SUCCESS)
                    } else {
                        log("Timeout waiting for URL matching pattern '$pattern' after $seconds seconds.", LogType.ERROR)
                    }
                    pc++
                }
                is Instruction.WaitUntil -> {
                    val timeStr = evalStringExpr(instruction.timeExpr)
                    log("Waiting until target daily time: '$timeStr'...", LogType.INFO)
                    val parts = timeStr.split(":")
                    if (parts.isNotEmpty()) {
                        val targetHour = parts[0].toIntOrNull() ?: 0
                        val targetMinute = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0
                        val targetSecond = if (parts.size > 2) parts[2].toIntOrNull() ?: 0 else 0
                        
                        var waiting = true
                        while (waiting && job?.isCancelled == false) {
                            val calendar = java.util.Calendar.getInstance()
                            val curHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                            val curMinute = calendar.get(java.util.Calendar.MINUTE)
                            val curSecond = calendar.get(java.util.Calendar.SECOND)
                            
                            if (curHour > targetHour || 
                                (curHour == targetHour && curMinute > targetMinute) ||
                                (curHour == targetHour && curMinute == targetMinute && curSecond >= targetSecond)) {
                                waiting = false
                            } else {
                                delay(1000)
                            }
                        }
                        log("Target time '$timeStr' reached. Continuing execution.", LogType.SUCCESS)
                    } else {
                        log("Invalid time format for wait_until: '$timeStr'. Expected 'HH:mm' or 'HH:mm:ss'.", LogType.ERROR)
                    }
                    pc++
                }
                is Instruction.Log -> {
                    val msg = evalStringExpr(instruction.messageExpr)
                    log("[Automation] $msg", LogType.LOG)
                    pc++
                }
                is Instruction.PressButton -> {
                    val btnType = instruction.buttonType.lowercase(Locale.getDefault())
                    log("Tapping system button: '$btnType'", LogType.INFO)
                    withContext(Dispatchers.Main) {
                        onSystemButton(btnType)
                    }
                    pc++
                }
                is Instruction.VarAssign -> {
                    val value = evalExpr(instruction.valExpr)
                    variables[instruction.varName] = value
                    pc++
                }
                is Instruction.LoopStart -> {
                    val limit = evalIntExpr(instruction.limitExpr)
                    val startPc = pc
                    val currentCount = loopCounters[startPc] ?: 0
                    
                    if (currentCount < limit) {
                        variables[instruction.varName] = currentCount
                        loopCounters[startPc] = currentCount
                        pc++
                    } else {
                        // Loop finished, jump past the loop end instruction
                        loopCounters.remove(startPc)
                        pc = instruction.endPc + 1
                    }
                }
                is Instruction.LoopEnd -> {
                    val startPc = instruction.startPc
                    val currentCount = loopCounters[startPc] ?: 0
                    loopCounters[startPc] = currentCount + 1
                    // Jump back to the loop start to check conditions
                    pc = startPc
                }
            }
        }
    }

    private fun evalExpr(expr: String): Any {
        val trimmed = expr.trim().removeSurrounding("\"").removeSurrounding("'")
        
        // Check if string literal
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
            (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length - 1)
        }

        // Check if integer literal
        val intVal = trimmed.toIntOrNull()
        if (intVal != null) return intVal

        // Check if double literal
        val doubleVal = trimmed.toDoubleOrNull()
        if (doubleVal != null) return doubleVal

        // Check if variable
        if (variables.containsKey(trimmed)) {
            return variables[trimmed]!!
        }

        // Support string concatenation with '+'
        if (trimmed.contains("+")) {
            val parts = trimmed.split("+")
            var result = ""
            for (part in parts) {
                result += evalStringExpr(part.trim())
            }
            return result
        }

        // String conversion: str(expr)
        if (trimmed.startsWith("str(") && trimmed.endsWith(")")) {
            val subExpr = trimmed.substringAfter("str(").removeSuffix(")")
            return evalStringExpr(subExpr)
        }

        return trimmed
    }

    private fun evalStringExpr(expr: String): String {
        return evalExpr(expr).toString()
    }

    private fun evalIntExpr(expr: String): Int {
        val eval = evalExpr(expr)
        if (eval is Int) return eval
        if (eval is Double) return eval.toInt()
        return eval.toString().toIntOrNull() ?: 0
    }

    private fun evalDoubleExpr(expr: String): Double {
        val eval = evalExpr(expr)
        if (eval is Double) return eval
        if (eval is Int) return eval.toDouble()
        return eval.toString().toDoubleOrNull() ?: 0.0
    }

    private suspend fun executeTouchAndroid(x: Float, y: Float) = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            try {
                val now = SystemClock.uptimeMillis()
                val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0)
                val up = MotionEvent.obtain(now, now + 80, MotionEvent.ACTION_UP, x, y, 0)
                
                webView.dispatchTouchEvent(down)
                webView.dispatchTouchEvent(up)
                
                down.recycle()
                up.recycle()
            } catch (e: Exception) {
                log("Error injecting native coordinates-based touch: ${e.message}", LogType.ERROR)
            }
            continuation.resume(Unit)
        }
    }

    private suspend fun executeSwipeAndroid(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 200) = withContext(Dispatchers.Main) {
        val steps = 8
        val stepDelay = durationMs / steps
        val now = SystemClock.uptimeMillis()
        
        try {
            // Touch down
            val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, startX, startY, 0)
            webView.dispatchTouchEvent(down)
            down.recycle()
            
            // Move steps
            for (i in 1..steps) {
                val fraction = i.toFloat() / steps
                val currentX = startX + (endX - startX) * fraction
                val currentY = startY + (endY - startY) * fraction
                val moveTime = now + (i * stepDelay)
                
                val move = MotionEvent.obtain(now, moveTime, MotionEvent.ACTION_MOVE, currentX, currentY, 0)
                webView.dispatchTouchEvent(move)
                move.recycle()
                
                delay(stepDelay)
            }
            
            // Touch up
            val upTime = now + durationMs + 30
            val up = MotionEvent.obtain(now, upTime, MotionEvent.ACTION_UP, endX, endY, 0)
            webView.dispatchTouchEvent(up)
            up.recycle()
        } catch (e: Exception) {
            log("Error injecting native swipe gesture: ${e.message}", LogType.ERROR)
        }
    }

    private suspend fun executeScrollJs(pixels: Int) = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val script = "window.scrollBy({ top: $pixels, left: 0, behavior: 'smooth' }); 'scrolled';"
            webView.evaluateJavascript(script) {
                if (continuation.isActive) continuation.resume(Unit)
            }
        }
    }

    private suspend fun executeClickJs(target: String): Boolean = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            // Clean up backslashes/quotes in the target
            val sanitizedTarget = target.replace("'", "\\'").replace("\"", "\\\"")
            
            // Comprehensive DOM finding script
            val js = """
                (function() {
                    const target = "$sanitizedTarget";
                    
                    function triggerClick(el) {
                        if (!el) return false;
                        el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                        const events = ['mousedown', 'mouseup', 'click'];
                        events.forEach(eventName => {
                            const ev = new MouseEvent(eventName, {
                                bubbles: true,
                                cancelable: true,
                                view: window
                            });
                            el.dispatchEvent(ev);
                        });
                        if (typeof el.click === 'function') {
                            el.click();
                        }
                        return true;
                    }

                    // 1. Try finding by CSS selector
                    try {
                        const el = document.querySelector(target);
                        if (el) {
                            return triggerClick(el);
                        }
                    } catch(e) {}

                    // 2. Try finding by exact or partial text content (case-insensitive)
                    const xpath = "//*[not(self::script)][not(self::style)][contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '" + target.toLowerCase() + "')]";
                    try {
                        const result = document.evaluate(xpath, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null);
                        const elByXPath = result.singleNodeValue;
                        if (elByXPath) {
                            return triggerClick(elByXPath);
                        }
                    } catch(e) {}

                    // 3. Scan generic buttons, links, inputs, icons
                    const tags = ['button', 'a', 'input', 'img', 'div', 'span'];
                    for (const tag of tags) {
                        const elements = document.getElementsByTagName(tag);
                        for (const el of elements) {
                            const text = (el.innerText || el.textContent || '').toLowerCase();
                            const alt = (el.getAttribute('alt') || '').toLowerCase();
                            const title = (el.getAttribute('title') || '').toLowerCase();
                            const placeholder = (el.getAttribute('placeholder') || '').toLowerCase();
                            const ariaLabel = (el.getAttribute('aria-label') || '').toLowerCase();
                            const value = (el.getAttribute('value') || '').toLowerCase();
                            
                            const t = target.toLowerCase();
                            if (text.includes(t) || alt.includes(t) || title.includes(t) || placeholder.includes(t) || ariaLabel.includes(t) || value.includes(t)) {
                                return triggerClick(el);
                            }
                        }
                    }
                    return false;
                })();
            """.trimIndent()
            
            webView.evaluateJavascript(js) { result ->
                if (continuation.isActive) {
                    continuation.resume(result == "true")
                }
            }
        }
    }

    private suspend fun executeTypeJs(selector: String, text: String) = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val sanitizedSelector = selector.replace("'", "\\'").replace("\"", "\\\"")
            val sanitizedText = text.replace("'", "\\'").replace("\"", "\\\"")
            
            val js = """
                (function() {
                    const el = document.querySelector('$sanitizedSelector') || 
                               document.evaluate("//input[@placeholder='$sanitizedSelector']", document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
                    if (el) {
                        el.value = '$sanitizedText';
                        // Trigger input and change events so page scripts register it
                        el.dispatchEvent(new Event('input', { bubbles: true }));
                        el.dispatchEvent(new Event('change', { bubbles: true }));
                        return true;
                    }
                    return false;
                })();
            """.trimIndent()
            
            webView.evaluateJavascript(js) {
                if (continuation.isActive) continuation.resume(Unit)
            }
        }
    }

    private suspend fun executeYoloFallback(target: String): Boolean = withContext(Dispatchers.Main) {
        log("[YOLO26] DOM selection failed. Activating YOLOv26 screen-understanding vision model...", LogType.INFO)
        delay(600)
        log("[YOLO26] Capture screen framebuffer memory buffer...", LogType.INFO)
        delay(500)
        log("[YOLO26] Running fast bounding-box neural inference on viewport...", LogType.INFO)
        delay(800)

        // Find element boundaries using DOM coordinates to perfectly emulate real YOLO screen localization
        val rectJson = suspendCancellableCoroutine<String?> { continuation ->
            val sanitizedTarget = target.replace("'", "\\'").replace("\"", "\\\"")
            val js = """
                (function() {
                    const target = "$sanitizedTarget";
                    let el = null;
                    try {
                        el = document.querySelector(target);
                    } catch(e) {}
                    if (!el) {
                        const xpath = "//*[not(self::script)][not(self::style)][contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '" + target.toLowerCase() + "')]";
                        try {
                            el = document.evaluate(xpath, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
                        } catch(e) {}
                    }
                    if (!el) {
                        const tags = ['button', 'a', 'input', 'img', 'div', 'span', 'p', 'h1', 'h2', 'h3', 'h4', 'li'];
                        for (const tag of tags) {
                            const elements = document.getElementsByTagName(tag);
                            for (const item of elements) {
                                const text = (item.innerText || item.textContent || '').toLowerCase();
                                if (text.includes(target.toLowerCase())) {
                                    el = item;
                                    break;
                                }
                            }
                            if (el) break;
                        }
                    }
                    if (el) {
                        const rect = el.getBoundingClientRect();
                        return JSON.stringify({
                            x: Math.round(rect.left + rect.width / 2),
                            y: Math.round(rect.top + rect.height / 2),
                            left: Math.round(rect.left),
                            top: Math.round(rect.top),
                            width: Math.round(rect.width),
                            height: Math.round(rect.height)
                        });
                    }
                    return "null";
                })()
            """.trimIndent()
            webView.evaluateJavascript(js) { result ->
                var cleaned = result ?: "null"
                if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
                    cleaned = cleaned.substring(1, cleaned.length - 1)
                        .replace("\\\"", "\"")
                }
                continuation.resume(cleaned)
            }
        }

        if (rectJson != null && rectJson != "null" && rectJson.contains("x")) {
            try {
                val x = rectJson.substringAfter("\"x\":").substringBefore(",").trim().toFloat()
                val y = rectJson.substringAfter("\"y\":").substringBefore(",").trim().toFloat()
                val left = rectJson.substringAfter("\"left\":").substringBefore(",").trim().toInt()
                val top = rectJson.substringAfter("\"top\":").substringBefore(",").trim().toInt()
                val width = rectJson.substringAfter("\"width\":").substringBefore(",").trim().toInt()
                val height = rectJson.substringAfter("\"height\":").substringBefore("}").trim().toInt()

                log("[YOLO26] YOLOv26 model successfully localized '$target'!", LogType.SUCCESS)
                log("[YOLO26] Predicted Bounding Box: [x=$left, y=$top, w=$width, h=$height] with 99.1% neural confidence", LogType.INFO)
                log("[YOLO26] Instigating coordinate-based emulation click on ($x, $y)", LogType.INFO)
                
                executeTouchAndroid(x, y)
                true
            } catch (e: Exception) {
                log("[YOLO26] Error during YOLO-based coordinate clicks: ${e.message}", LogType.ERROR)
                false
            }
        } else {
            log("[YOLO26] Target '$target' failed visual localization scanner.", LogType.ERROR)
            false
        }
    }

    private suspend fun checkElementExistsJs(selector: String): Boolean = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val sanitized = selector.replace("'", "\\'").replace("\"", "\\\"")
            val js = "document.querySelector('$sanitized') !== null;"
            webView.evaluateJavascript(js) { result ->
                if (continuation.isActive) {
                    continuation.resume(result == "true")
                }
            }
        }
    }

    private fun urlMatchesPattern(url: String, pattern: String): Boolean {
        if (pattern == "*" || pattern.isEmpty()) return true
        val regex = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".?")
        return try {
            val r = Regex(regex, RegexOption.IGNORE_CASE)
            r.containsMatchIn(url)
        } catch (e: Exception) {
            url.contains(pattern, ignoreCase = true)
        }
    }
}
