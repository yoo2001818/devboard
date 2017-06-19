package com.example.devboard;

/**
 * Created by yoo2001818 on 17. 6. 19.
 */

public interface CJKInputMethod {
    void reset();
    String finish();
    boolean process(int charCode);
    String getCurrent();
}
