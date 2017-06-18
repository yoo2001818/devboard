package com.example.devboard;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodSubtype;

/**
 * Created by Sunrin on 2017-06-13.
 */

public class DevBoardIME extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    StringBuilder composeQueue = new StringBuilder();
    KeyboardView inputView;

    @Override
    public void onInitializeInterface() {

    }

    @Override
    public View onCreateInputView() {
        KeyboardView view = new KeyboardView(null, null);
        view.setOnKeyboardActionListener(this);
        view.setKeyboard(new Keyboard(this, R.xml.qwerty));
        inputView = view;
        return view;
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        composeQueue.setLength(0);
    }

    @Override
    public void onFinishInput() {
        super.onFinishInput();
        composeQueue.setLength(0);
        if (inputView != null) inputView.closing();
    }


    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
    }

    @Override
    protected void onCurrentInputMethodSubtypeChanged(InputMethodSubtype newSubtype) {
        super.onCurrentInputMethodSubtypeChanged(newSubtype);
    }

    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
        if (composeQueue.length() > 0 && (newSelStart != candidatesEnd || newSelEnd != candidatesEnd)) {
            composeQueue.setLength(0);
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) ic.finishComposingText();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (event.getRepeatCount() == 0 && inputView == null) {
                    if(inputView.handleBack()) return true;
                }
                break;
            case KeyEvent.KEYCODE_DEL:
                if (composeQueue.length() > 0) {
                    onKey(Keyboard.KEYCODE_DELETE, null);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_ENTER:
                return false;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return super.onKeyUp(keyCode, event);
    }

    private void commitTyped(InputConnection ic) {
        if (composeQueue.length() > 0) {
            ic.commitText(composeQueue, composeQueue.length());
            composeQueue.setLength(0);
        }
    }

    private void updateShiftKey(EditorInfo attr) {
        if (attr != null && inputView != null) {
            int caps = 0;
            EditorInfo ei = getCurrentInputEditorInfo();
            if (ei != null && ei.inputType != InputType.TYPE_NULL) {
                caps = getCurrentInputConnection().getCursorCapsMode(attr.inputType);
            }
            inputView.setShifted(caps != 0);
        }
    }

    private void keyDownUp(int keyEventCode) {
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }

    private void sendKey(int keyCode) {
        switch (keyCode) {
            case '\n':
                keyDownUp(KeyEvent.KEYCODE_ENTER);
                break;
            default:
                if (keyCode >= '0' && keyCode <= '9') {
                    keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
                } else {
                    getCurrentInputConnection().commitText(String.valueOf((char) keyCode), 1);
                }
                break;
        }
    }

    private void handleBackspace() {
        int length = composeQueue.length();
        if (length > 1) {
            composeQueue.delete(length - 1, length);
            getCurrentInputConnection().setComposingText(composeQueue, 1);
        } else if (length > 0) {
            composeQueue.setLength(0);
            getCurrentInputConnection().setComposingText("", 0);
        } else {
            keyDownUp(KeyEvent.KEYCODE_DEL);
        }
        updateShiftKey(getCurrentInputEditorInfo());
    }

    private void handleShift() {
        inputView.setShifted(!inputView.isShifted());
    }

    private void handleClose() {
        commitTyped(getCurrentInputConnection());
        requestHideSelf(0);
        inputView.closing();
    }

    @Override
    public void onPress(int primaryCode) {

    }

    @Override
    public void onRelease(int primaryCode) {

    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        if (primaryCode == ' ') {
            commitTyped(getCurrentInputConnection());
            sendKey(primaryCode);
            updateShiftKey(getCurrentInputEditorInfo());
        } else if (primaryCode == Keyboard.KEYCODE_DELETE) {
            handleBackspace();
        } else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
            handleShift();
        } else if (primaryCode == Keyboard.KEYCODE_CANCEL) {
            handleClose();
        } else {
            if (inputView.isShifted()) primaryCode = Character.toUpperCase(primaryCode);
            composeQueue.append((char) primaryCode);
            getCurrentInputConnection().setComposingText(composeQueue, 1);
            updateShiftKey(getCurrentInputEditorInfo());
        }
    }

    @Override
    public void onText(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.beginBatchEdit();
        if (composeQueue.length() > 0) {
            commitTyped(ic);
        }
        ic.commitText(text, 0);
        ic.endBatchEdit();
        updateShiftKey(getCurrentInputEditorInfo());
    }

    @Override
    public void swipeLeft() {

    }

    @Override
    public void swipeRight() {

    }

    @Override
    public void swipeDown() {

    }

    @Override
    public void swipeUp() {

    }
}
