package com.example.devboard;

import java.util.List;

/**
 * Created by yoo2001818 on 17. 6. 19.
 */

// Input method that does nothing. required to implement traditional non-IME keyboards, which is
// a regular English (US) keyboard.
public class NoopInputMethod implements CJKInputMethod {

    @Override
    public void reset() {

    }

    @Override
    public String finish() {
        return "";
    }

    @Override
    public boolean processDevboard(int position, boolean shift) {
        return false;
    }

    @Override
    public boolean process(int charCode) {
        return false;
    }

    @Override
    public boolean backspace() {
        return false;
    }

    @Override
    public String getCurrent() {
        return "";
    }

    @Override
    public List<Key> getLayout(List<Key> original) {
        return original;
    }

    @Override
    public List<Key> getLayoutShift(List<Key> original) {
        return original;
    }
}
