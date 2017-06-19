package com.example.devboard;

import java.util.List;

/**
 * Created by yoo2001818 on 17. 6. 19.
 */

public interface CJKInputMethod {
    void reset();
    String finish();
    boolean processDevboard(int position, boolean shift);
    boolean process(int charCode);
    String getCurrent();

    List<Key> getLayout(List<Key> original);
    List<Key> getLayoutShift(List<Key> original);
}
