package com.example.devboard;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.text.InputType;
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

    private CJKInputMethod[] methods;
    private boolean isShift = false;
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

    public DevBoardIME() {
    }

    private void placeLayouts() {
        // Load layout data from keyboard layouts, then process / add it to the list.
        layouts[0] = keyLayout.getLayout();
        layouts[1] = applyShift(layouts[0]);
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
        if (currentMethod == 1) {
            return layouts[6 + (isShift ? 1 : 0)];
        }
        return layouts[(isShift ? 1 : 0)];
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
        composeQueue.setLength(0);
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
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (event.getRepeatCount() == 0 && inputView == null) {
                    // if(inputView.handleBack()) return true;
                }
                break;
            case KeyEvent.KEYCODE_DEL:
                if (composeQueue.length() > 0) {
                    onKey(0, new Key("BKSP", KEYCODE_BACKSPACE, null));
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

    private void handleClose() {
        commitTyped(getCurrentInputConnection());
        requestHideSelf(0);
        // inputView.closing();
    }

    @Override
    public void onPress(int id, Key key) {
        int primaryCode = key.getCode();
        if (primaryCode == KEYCODE_SHIFT) {
            getCurrentInputConnection().sendKeyEvent(
                    new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_LEFT));
            isShift = true;
            updateShiftKey(getCurrentInputEditorInfo());
        }
    }

    @Override
    public void onRelease(int id, Key key) {
        int primaryCode = key.getCode();
        if (primaryCode == KEYCODE_SHIFT) {
            getCurrentInputConnection().sendKeyEvent(
                    new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SHIFT_LEFT));
            isShift = false;
            updateShiftKey(getCurrentInputEditorInfo());
        }
    }

    @Override
    public void onKey(int id, Key key) {
        // TODO Intercept to IME first
        int primaryCode = key.getCode();
        if (primaryCode == ' ') {
            commitTyped(getCurrentInputConnection());
            sendKey(primaryCode);
            updateShiftKey(getCurrentInputEditorInfo());
        } else if (primaryCode == KEYCODE_BACKSPACE) {
            handleBackspace();
        } else if (primaryCode == KEYCODE_SHIFT) {
            // handleShift();
        } else if (primaryCode == KEYCODE_LOCALE) {
            currentMethod = (currentMethod + 1) % methods.length;
            updateLayout();
        // } else if (primaryCode == Keyboard.KEYCODE_CANCEL) {
        //    handleClose();
        } else {
            // if (inputView.isShifted()) primaryCode = Character.toUpperCase(primaryCode);
            composeQueue.append((char) primaryCode);
            getCurrentInputConnection().setComposingText(composeQueue, 1);
            updateShiftKey(getCurrentInputEditorInfo());
        }
    }
}
