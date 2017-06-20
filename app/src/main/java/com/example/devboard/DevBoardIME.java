package com.example.devboard;

import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Vibrator;
import android.text.InputType;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodSubtype;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sunrin on 2017-06-13.
 */

public class DevBoardIME extends InputMethodService implements DevBoardView.Listener {

    public static final int KEYCODE_SHIFT = -1;
    public static final int KEYCODE_BACKSPACE = -2;
    public static final int KEYCODE_LOCALE = -3;
    public static final int KEYCODE_FN = -4;
    public static final int KEYCODE_FN2 = -5;
    public static final int KEYCODE_MULTIPLE = -6;

    private CJKInputMethod[] methods;
    private int currentMethod = 0;

    StringBuilder composeQueue = new StringBuilder();
    DevBoardView inputView;

    // There are 8 planes in the keyboard - Fn1, Fn2, Shift. Shift planes are generated automatically.
    // 0 - normal
    // 1 - normal + shift
    // 2 - Fn1
    // 3 - Fn1 + shift
    // 4 - Fn2
    // 5 - Fn2 + shift
    // 6 - Korean
    // 7 - Korean + shift
    // As you can see, even numbers are normal layouts, and odd numbers are shift layouts.
    // Note that we are using raw List - it couldn't be done using generic List.
    // Also note that it's possible to support infinitely many input methods - but that probably won't happen.
    List[] layouts = new List[8];
    KeyLayout keyLayout;

    // If the toggle key, like shift or fn keys are "clicked" without pressing any keys, toggle key
    // mode should initiate.
    ToggleKeyState shiftKey = new ToggleKeyState(new ToggleKeyState.Listener() {
        @Override
        public void release() {
            getCurrentInputConnection().sendKeyEvent(
                    new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SHIFT_LEFT));
            updateShiftKey(getCurrentInputEditorInfo());
        }

        @Override
        public void press() {
            getCurrentInputConnection().sendKeyEvent(
                    new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_LEFT));
            updateShiftKey(getCurrentInputEditorInfo());
        }
    });
    ToggleKeyState fnKey = new ToggleKeyState(new ToggleKeyState.Listener() {
        @Override
        public void release() {
            updateLayout();
        }

        @Override
        public void press() {
            fn2Key.setEnabled(false);
            updateLayout();
        }
    });
    ToggleKeyState fn2Key = new ToggleKeyState(new ToggleKeyState.Listener() {
        @Override
        public void release() {
            updateLayout();
        }

        @Override
        public void press() {
            fnKey.setEnabled(false);
            updateLayout();
        }
    });

    Vibrator vibrator;
    SoundPlayer soundPlayer;

    Key previousKey;
    int sameKeyCount = 0;

    public DevBoardIME() {
    }

    private void placeLayouts() {
        // Load layout data from keyboard layouts, then process / add it to the list.
        for (int i = 0; i < 3; ++i) {
            layouts[i * 2] = keyLayout.getLayout().get(i);
            layouts[i * 2 + 1] = applyShift(layouts[i * 2]);
        }
        // Fn1 / Fn2 is not done yet..
        // Have some hardcoded IME
        if (methods.length > 1) {
            layouts[6] = methods[1].getLayout(layouts[0]);
            layouts[7] = methods[1].getLayoutShift(layouts[1]);
        }
    }

    private List<Key> applyShift(List<Key> keys) {
        List<Key> output = new ArrayList<>();
        for (Key key : keys) {
            // Do you even SHIFT???
            int code = key.getCode();
            if ('a' <= code && code <= 'z') {
                output.add(new Key(key.getLabel().toUpperCase(), code - 'a' + 'A', key.getExtra()));
            } else if ('A' <= code && code <= 'Z') {
                output.add(new Key(key.getLabel().toLowerCase(), code - 'A' + 'a', key.getExtra()));
            } else {
                output.add(key);
            }
        }
        return output;
    }

    private List<Key> getCurrentLayout() {
        // Looks weird
        if (fnKey.isEnabled()) {
            return layouts[2 + (shiftKey.isEnabled() ? 1 : 0)];
        }
        if (fn2Key.isEnabled()) {
            return layouts[4 + (shiftKey.isEnabled() ? 1 : 0)];
        }
        if (currentMethod == 1) {
            return layouts[6 + (shiftKey.isEnabled() ? 1 : 0)];
        }
        return layouts[(shiftKey.isEnabled() ? 1 : 0)];
    }

    private CJKInputMethod getInputMethod() {
        return methods[currentMethod];
    }

    private void updateLayout() {
        List<Key> currentLayout = getCurrentLayout();
        if (currentLayout != inputView.getKeyLayout()) {
            inputView.setKeyLayout(currentLayout);
        }
    }

    @Override
    public void onInitializeInterface() {
        methods = new CJKInputMethod[] {
                new NoopInputMethod(),
                new DubeolsikInputMethod(),
        };
        try {
            Reader reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.default_layout), "UTF-8"));
            Gson gson = new Gson();
            // Load default layout
            keyLayout = gson.fromJson(reader, KeyLayout.class);
        } catch (UnsupportedEncodingException e) {
            // This shouldn't happen
        }
        this.placeLayouts();
        vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        soundPlayer = new SoundPlayer(getApplicationContext());
    }

    @Override
    public View onCreateInputView() {
        DevBoardView view = new DevBoardView(new ContextThemeWrapper(getApplicationContext(), R.style.AppTheme));
        view.setListener(this);
        inputView = view;
        return view;
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        // Reset state
        shiftKey.reset();
        fnKey.reset();
        fn2Key.reset();
        composeQueue.setLength(0);
        if (restarting) updateLayout();
        getInputMethod().finish();
    }

    @Override
    public void onFinishInput() {
        super.onFinishInput();
        composeQueue.setLength(0);
        getInputMethod().finish();
        // if (inputView != null) inputView.closing();
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
            getInputMethod().finish();
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) ic.finishComposingText();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // if (event.getRepeatCount() == 0) soundPlayer.playPress();
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (event.getRepeatCount() == 0 && inputView == null) {
                    // if(inputView.handleBack()) return true;
                }
                break;
            case KeyEvent.KEYCODE_DEL:
                handleBackspace();
                return true;
            case KeyEvent.KEYCODE_LANGUAGE_SWITCH:
                commitIME();
                currentMethod = (currentMethod + 1) % methods.length;
                updateLayout();
                return true;
            case KeyEvent.KEYCODE_SPACE:
                if (event.isShiftPressed()) {
                    currentMethod = (currentMethod + 1) % methods.length;
                    updateLayout();
                    return true;
                }
            default:
                // Intercept to IME first.
                char label = event.getDisplayLabel();
                if (label == 0) {
                    return super.onKeyDown(keyCode, event);
                }
                if (!event.isShiftPressed()) label = Character.toLowerCase(label);
                if (getInputMethod().process(label)) {
                    // If this is the first time and the buffer is not empty, commit already existing buffer.
                    if (composeQueue.length() > 0) {
                        commitTyped(getCurrentInputConnection());
                    }
                    // ... Set the composing text to IME's buffer.
                    String current = getInputMethod().getCurrent();
                    getCurrentInputConnection().setComposingText(current, current.length());
                    useToggle();
                    return true;
                } else {
                    commitIME();
                    commitTyped(getCurrentInputConnection());
                    updateShiftKey(getCurrentInputEditorInfo());
                }
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // soundPlayer.playRelease();
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
            /*int caps = 0;
            EditorInfo ei = getCurrentInputEditorInfo();
            if (ei != null && ei.inputType != InputType.TYPE_NULL) {
                caps = getCurrentInputConnection().getCursorCapsMode(attr.inputType);
            }*/
            updateLayout();
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
        InputConnection ic = getCurrentInputConnection();
        if (getInputMethod().backspace()) {
            // The IME has spoken that it has processed backspace by itself - horray!
            // ... Set the composing text to IME's buffer.
            String current = getInputMethod().getCurrent();
            ic.setComposingText(current, 1);
            updateShiftKey(getCurrentInputEditorInfo());
            return;
        }
        int length = composeQueue.length();
        if (length > 0) {
            composeQueue.delete(length - 1, length);
            ic.setComposingText(composeQueue, 1);
        } else {
            keyDownUp(KeyEvent.KEYCODE_DEL);
        }
        updateShiftKey(getCurrentInputEditorInfo());
    }

    private void handleClose() {
        commitTyped(getCurrentInputConnection());
        requestHideSelf(0);
        // inputView.closing();
    }

    private void commitIME() {
        // If input method's buffer is not empty, commit it.
        String current = getInputMethod().finish();
        if (current.length() > 0) {
            getCurrentInputConnection().commitText(current, current.length());
            previousKey = null;
            sameKeyCount = 0;
        }
    }

    private void useToggle() {
        shiftKey.use();
        fnKey.use();
        fn2Key.use();
    }

    @Override
    public void onPress(int id, Key key) {
        soundPlayer.playPress();
        int primaryCode = key.getCode();
        if (primaryCode == KEYCODE_SHIFT) {
            shiftKey.press();
            vibrator.vibrate(20);
        }
        if (primaryCode == KEYCODE_FN) {
            fnKey.press();
            vibrator.vibrate(20);
        }
        if (primaryCode == KEYCODE_FN2) {
            fn2Key.press();
            vibrator.vibrate(20);
        }

    }

    @Override
    public void onRelease(int id, Key key) {
        soundPlayer.playRelease();
        int primaryCode = key.getCode();
        if (primaryCode == KEYCODE_SHIFT) shiftKey.release();
        if (primaryCode == KEYCODE_FN) fnKey.release();
        if (primaryCode == KEYCODE_FN2) fn2Key.release();
    }

    @Override
    public void onKey(int id, Key key) {
        InputConnection ic = getCurrentInputConnection();
        if (previousKey == key) sameKeyCount += 1;
        else sameKeyCount = 0;
        previousKey = key;
        // Intercept to IME first.
        if (!fnKey.isEnabled() && !fn2Key.isEnabled() &&
                getInputMethod().processDevboard(id, shiftKey.isEnabled())
        ) {
            // Vibrate the phone for short moment
            vibrator.vibrate(20);
            // If this is the first time and the buffer is not empty, commit already existing buffer.
            if (composeQueue.length() > 0) {
                commitTyped(ic);
            }
            // ... Set the composing text to IME's buffer.
            String current = getInputMethod().getCurrent();
            ic.setComposingText(current, current.length());
            useToggle();
            return;
        }
        int primaryCode = key.getCode();
        if (primaryCode == 0) {
            // Null key
            return;
        } else if (primaryCode == ' ') {
            commitIME();
            commitTyped(ic);
            sendKey(primaryCode);
            updateShiftKey(getCurrentInputEditorInfo());
        } else if (primaryCode == '\n' || primaryCode == '\t') {
            commitIME();
            commitTyped(ic);
            sendKey(primaryCode);
        } else if (primaryCode == KEYCODE_BACKSPACE) {
            handleBackspace();
        } else if (primaryCode == KEYCODE_SHIFT) {
            return;
        } else if (primaryCode == KEYCODE_FN) {
            return;
        } else if (primaryCode == KEYCODE_FN2) {
            return;
        } else if (primaryCode == KEYCODE_LOCALE) {
            commitIME();
            currentMethod = (currentMethod + 1) % methods.length;
            updateLayout();
        // } else if (primaryCode == Keyboard.KEYCODE_CANCEL) {
        //    handleClose();
        } else if (primaryCode == KEYCODE_MULTIPLE) {
            commitIME();
            int previousIndex = (sameKeyCount - 1) % key.getExtra().size();
            String previous = previousIndex < 0 ? "" : key.getExtra().get(previousIndex);
            String current = key.getExtra().get(sameKeyCount % key.getExtra().size());
            // Remove N characters and add as current. Easy?
            composeQueue.setLength(composeQueue.length() - previous.length());
            composeQueue.append(current);
            ic.setComposingText(composeQueue, 1);
        } else {
            commitIME();
            // if (inputView.isShifted()) primaryCode = Character.toUpperCase(primaryCode);
            composeQueue.append((char) primaryCode);
            ic.setComposingText(composeQueue, 1);
            updateShiftKey(getCurrentInputEditorInfo());
        }
        // Vibrate the phone for short moment
        vibrator.vibrate(20);
        useToggle();
    }
}
