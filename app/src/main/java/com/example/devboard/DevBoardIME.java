package com.example.devboard;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.media.AudioManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethod;
import android.view.inputmethod.InputMethodManager;
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

    // Java is too verbose.
    public static final String USE_KOREAN = "useKorean";
    public static final String AUDIO_TYPE = "audioType";
    public static final String AUDIO_VOLUME = "audioVolume";
    public static final String VIBRATE_VOLUME = "vibrateVolume";
    public static final String LAYOUT_DATA = "layoutData";
    public static final String THEME = "theme";
    public static final String KEY_HEIGHT = "keyHeight";

    public static final int KEYCODE_SHIFT = -1;
    public static final int KEYCODE_BACKSPACE = -2;
    public static final int KEYCODE_LOCALE = -3;
    public static final int KEYCODE_FN = -4;
    public static final int KEYCODE_FN2 = -5;
    public static final int KEYCODE_MULTIPLE = -6;
    public static final int KEYCODE_SINGLE = -12;

    public static final int KEYCODE_UP = -7;
    public static final int KEYCODE_DOWN = -8;
    public static final int KEYCODE_LEFT = -9;
    public static final int KEYCODE_RIGHT = -10;

    public static final int KEYCODE_OPTIONS = -11;

    private static DevBoardIME instance;

    private CJKInputMethod[] methods;
    private int currentMethod = 0;

    private StringBuilder composeQueue = new StringBuilder();
    private DevBoardView inputView;

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
    private List[] layouts = new List[8];
    private KeyLayout keyLayout;

    // If the toggle key, like shift or fn keys are "clicked" without pressing any keys, toggle key
    // mode should initiate.
    private ToggleKeyState shiftKey = new ToggleKeyState(new ToggleKeyState.Listener() {
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
    private ToggleKeyState fnKey = new ToggleKeyState(new ToggleKeyState.Listener() {
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
    private ToggleKeyState fn2Key = new ToggleKeyState(new ToggleKeyState.Listener() {
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

    private Gson gson = new Gson();

    private Vibrator vibrator;
    private int vibrateVolume = 20;
    private SoundPlayer soundPlayer;;
    private int currentTheme = -1;
    private int currentHeight;

    private Key previousKey;
    private int sameKeyCount = 0;

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

    public DevBoardIME() {
        super();
        instance = this;
    }

    @Override
    public void onInitializeInterface() {
        loadConfiguration();
        /*
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
        soundPlayer = new SoundPlayer(getApplicationContext());
        this.placeLayouts();
        */
        vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    public View onCreateInputView() {
        if (inputView != null) return inputView;
        DevBoardView view = new DevBoardView(new ContextThemeWrapper(getApplicationContext(), currentTheme));
        view.setListener(this);
        view.setHeight(currentHeight);
        view.setKeyLayout(getCurrentLayout());
        inputView = view;
        return view;
    }

    public View forceCreateView() {
        DevBoardView view = new DevBoardView(new ContextThemeWrapper(getApplicationContext(), currentTheme));
        view.setListener(this);
        view.setHeight(currentHeight);
        view.setKeyLayout(getCurrentLayout());
        inputView = view;
        setInputView(view);
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
        if (restarting) {
            onCreateInputView();
            updateLayout();
        }
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

    private boolean isConnectBot() {
        // ConnectBot simply ignores suggestions - which is bad. We need to workaround to not to use
        // IME to make this work on ConnectBot.
        EditorInfo ei = getCurrentInputEditorInfo();
        return ei.inputType == 0;
    }

    @Override
    public void onPress(int id, Key key) {
        soundPlayer.playPress();
        int primaryCode = key.getCode();
        if (primaryCode == KEYCODE_SHIFT) {
            shiftKey.press();
            vibrator.vibrate(vibrateVolume);
        }
        if (primaryCode == KEYCODE_FN) {
            fnKey.press();
            vibrator.vibrate(vibrateVolume);
        }
        if (primaryCode == KEYCODE_FN2) {
            fn2Key.press();
            vibrator.vibrate(vibrateVolume);
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
    public boolean onLongPress(int id, Key key) {
        int primaryCode = key.getCode();
        switch (primaryCode) {
            case KEYCODE_LOCALE:
                ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker();
                return true;
        }
        return false;
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
            vibrator.vibrate(vibrateVolume);
            // If this is the first time and the buffer is not empty, commit already existing buffer.
            if (composeQueue.length() > 0) {
                commitTyped(ic);
            }
            // ... Set the composing text to IME's buffer.
            if (isConnectBot()) {
                String buffer = getInputMethod().empty();
                ic.commitText(buffer, buffer.length());
                ic.setComposingText(getInputMethod().getCurrent(), 1);
            } else {
                String current = getInputMethod().getCurrent();
                ic.setComposingText(current, 1);
            }
            useToggle();
            return;
        }
        int primaryCode = key.getCode();
        switch (primaryCode) {
            case 0:
                return;
            case ' ':
                commitIME();
                commitTyped(ic);
                sendKey(primaryCode);
                updateShiftKey(getCurrentInputEditorInfo());
                break;
            case '\n':
                commitIME();
                commitTyped(ic);
                sendKey(primaryCode);
                break;
            case KEYCODE_BACKSPACE:
                handleBackspace();
                break;
            case KEYCODE_SHIFT:
            case KEYCODE_FN:
            case KEYCODE_FN2:
                return;
            case KEYCODE_LOCALE:
                commitIME();
                currentMethod = (currentMethod + 1) % methods.length;
                updateLayout();
                break;
            case KEYCODE_MULTIPLE:
                commitIME();
                int previousIndex = (sameKeyCount - 1) % key.getExtra().size();
                String previous = previousIndex < 0 ? "" : key.getExtra().get(previousIndex);
                String current = key.getExtra().get(sameKeyCount % key.getExtra().size());
                // Remove N characters and add as current. Easy?
                composeQueue.setLength(composeQueue.length() - previous.length());
                composeQueue.append(current);
                ic.setComposingText(composeQueue, 1);
                break;
            case KEYCODE_SINGLE:
                commitIME();
                String a = key.getExtra().get(0);
                composeQueue.append(a);
                ic.setComposingText(composeQueue, 1);
                break;
            case '\t':
                commitIME();
                commitTyped(ic);
                keyDownUp(KeyEvent.KEYCODE_TAB);
                break;
            case KEYCODE_UP:
                keyDownUp(KeyEvent.KEYCODE_DPAD_UP);
                break;
            case KEYCODE_DOWN:
                keyDownUp(KeyEvent.KEYCODE_DPAD_DOWN);
                break;
            case KEYCODE_LEFT:
                keyDownUp(KeyEvent.KEYCODE_DPAD_LEFT);
                break;
            case KEYCODE_RIGHT:
                keyDownUp(KeyEvent.KEYCODE_DPAD_RIGHT);
                break;
            case KEYCODE_OPTIONS:
                commitIME();
                commitTyped(ic);
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                break;
            default:
                commitIME();
                // if (inputView.isShifted()) primaryCode = Character.toUpperCase(primaryCode);
                composeQueue.append((char) primaryCode);
                ic.setComposingText(composeQueue, 1);
                updateShiftKey(getCurrentInputEditorInfo());
                if (isConnectBot()) commitTyped(ic);
                break;
        }
        // Vibrate the phone for short moment
        vibrator.vibrate(vibrateVolume);
        useToggle();
    }


    public void loadConfiguration() {
        Log.i("DevboardIME", "IME reloading config");
        SharedPreferences pref = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        if (pref.getBoolean(USE_KOREAN, true)) {
            methods = new CJKInputMethod[]{
                    new NoopInputMethod(),
                    new DubeolsikInputMethod(),
            };
        } else {
            methods = new CJKInputMethod[]{
                    new NoopInputMethod()
            };
        }
        if (pref.contains(LAYOUT_DATA)) {
            keyLayout = gson.fromJson(pref.getString(LAYOUT_DATA, ""), KeyLayout.class);
        } else {
            try {
                Reader reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.default_layout), "UTF-8"));
                // Load default layout
                keyLayout = gson.fromJson(reader, KeyLayout.class);
            } catch (UnsupportedEncodingException e) {
                // This shouldn't happen
            }
        }
        if (soundPlayer != null) soundPlayer.release();
        String audioTypeName = pref.getString(AUDIO_TYPE, "system");
        int audioType = AudioManager.STREAM_SYSTEM;
        switch (audioTypeName) {
            case "system":
                audioType = AudioManager.STREAM_SYSTEM;
                break;
            case "media":
                audioType = AudioManager.STREAM_MUSIC;
                break;
        }
        soundPlayer = new SoundPlayer(getApplicationContext(), audioType);
        soundPlayer.setVolume(pref.getInt(AUDIO_VOLUME, 100) / 100f);
        vibrateVolume = pref.getInt(VIBRATE_VOLUME, 20);
        currentHeight = pref.getInt(KEY_HEIGHT, 220);
        if (inputView != null) inputView.setHeight(currentHeight);
        int desiredTheme = ThemeLoader.getTheme(getApplicationContext());
        if (currentTheme != -1 && currentTheme != desiredTheme) {
            // Force update view
            currentTheme = desiredTheme;
            forceCreateView();
        }
        currentTheme = desiredTheme;
        this.placeLayouts();
        if (inputView != null) inputView.setKeyLayout(getCurrentLayout());
    }

    public static DevBoardIME getInstance() {
        return instance;
    }
}
