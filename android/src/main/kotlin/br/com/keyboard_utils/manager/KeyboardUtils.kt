package br.com.keyboard_utils.manager

import android.app.Activity
import android.os.CountDownTimer
import android.view.View
import android.view.ViewTreeObserver
import android.graphics.Rect

/**
 * Created by Wilson Martins on 2019-10-25.
 */

interface KeyboardUtils {
    fun start()

    fun dispose()

    fun handleKeyboard()

    fun onKeyboardOpen(action: (height: Int) -> Unit)

    fun onKeyboardClose(action: () -> Unit)
}

class KeyboardUtilsImpl(private val activity: Activity) : KeyboardUtils {

    // screen parent view
    private var parentView: View = activity.findViewById(android.R.id.content)

    // keyboard action listeners -> this objects will call when KeyboardUtils handle keyboard
    // open event or close event;
    private var keyboardOpenedEvent: (altura: Int) -> Unit? = {}
    private var keyboardClosedEvent: () -> Unit? = {}

    // keyboard status flag
    private var keyboardOpened: Boolean = false

    // last keyboard height
    private var lastKeyboardHeight = 0

    // device manager -> this object do magics!
    private val deviceDimensionsManager: DeviceDimesions

    // keyboard sessions heights -> TODO
    private var keyboardSessionHeights = arrayListOf<Int>()

    // keyboard session timer -> TODO
    private var keyboardSessionTimer: CountDownTimer? = null

    init {
        deviceDimensionsManager = DeviceDimesionsImpl(activity, parentView)
        handleKeyboard()
    }

    override fun handleKeyboard() {
        keyboardSessionTimer = object : CountDownTimer(150, 1) {
            override fun onFinish() {
                keyboardSessionHeights.max()?.let {
                    if (it > 0 && lastKeyboardHeight != it) {
                        var statusBar = 0
                        var finalHeight = it
                        val resources = activity.resources
                        val resourceId = resources.getIdentifier(
                                "status_bar_height", "dimen", "android")
                        if (resourceId > 0) {
                            statusBar = resources.getDimensionPixelSize(resourceId)
                        }
                        if (statusBar > 100) {
                            finalHeight += statusBar
                        }
                        keyboardOpenedEvent(finalHeight)
                        lastKeyboardHeight = -1
                    } else if (it <= 0) {
                        keyboardClosedEvent()
                    }
                    keyboardOpened = false
                    keyboardSessionTimer?.cancel()
                    keyboardSessionHeights.clear()
                    lastKeyboardHeight = it
                }
            }

            override fun onTick(millisUntilFinished: Long) {
                val alturaTecladoCalculada = deviceDimensionsManager.keyboardHeight()
                keyboardSessionHeights.add(alturaTecladoCalculada)
            }
        }
    }

    override fun start() {
        registerKeyboardListener()
    }

    override fun dispose() {
        parentView.viewTreeObserver.removeOnGlobalLayoutListener {  }
        keyboardSessionTimer?.cancel()
    }
    
    private fun registerKeyboardListener() {
        parentView.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            private val r: Rect = Rect()
            override fun onGlobalLayout() {
                // Conclude whether the keyboard is shown or not.
                parentView.getWindowVisibleDisplayFrame(r)
                var screenHeight = parentView.rootView.height
                var heightDiff = screenHeight - (r.bottom - r.top)

                // 0.15 ratio is perhaps enough to determine keypad height.
                if (heightDiff > screenHeight * 0.15) { 
                    keyboardOpenedEvent(heightDiff)
                }
                else {
                    keyboardClosedEvent()
                }
            }
        })
    }
    override fun onKeyboardOpen(action: (height: Int) -> Unit) {
        keyboardOpenedEvent = action
    }

    override fun onKeyboardClose(action: () -> Unit) {
        keyboardClosedEvent = action
    }
}