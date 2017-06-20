package com.example.devboard;

/**
 * Created by yoo2001818 on 2017-06-20.
 */

public class ToggleKeyState {
    private boolean pressed;
    private boolean enabled;
    private boolean used;

    private Listener listener;

    public ToggleKeyState(Listener listener) {
        this.listener = listener;
    }

    public void press() {
        enabled = !enabled;
        pressed = true;
        used = false;
        if (enabled) listener.press();
        else listener.release();
    }

    public void release() {
        if (used && enabled) {
            enabled = false;
            listener.release();
        }
        pressed = false;
    }

    public void use() {
        if (!pressed && enabled) {
            enabled = false;
            listener.release();
        }
        used = true;
    }

    public boolean isPressed() {
        return pressed;
    }

    public void setPressed(boolean pressed) {
        this.pressed = pressed;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public interface Listener {
        public void release();
        public void press();
    }
}
