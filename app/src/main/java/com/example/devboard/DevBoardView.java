package com.example.devboard;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

/**
 * Created by yoo2001818 on 17. 6. 18.
 */

public class DevBoardView extends LinearLayout {

    // The keyboard table is hardcoded - it's not supposed to change whatsoever!
    public static final float[][] LAYOUT_TABLE = new float[][] {
            new float[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 },
            new float[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1.5f },
            new float[] { 1.5f, 1, 1, 1, 1, 1, 1, 1, 1.5f },
            new float[] { 1, 1, 1, 4, 1, 1, 1 },
    };

    private AttributeSet attrs;

    protected LinearLayout[] layoutTable;
    protected View[] keyTable;

    public DevBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.attrs = attrs;
        this.createLayout();
    }

    public DevBoardView(Context context) {
        super(context);
        this.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        this.createLayout();
    }

    private void createLayout() {
        this.setOrientation(VERTICAL);
        // Create layout according to the layout table.
        layoutTable = new LinearLayout[LAYOUT_TABLE.length];
        // Count all keys and get the size.
        int keyTableSize = 0;
        for (float[] layoutRow : LAYOUT_TABLE) {
            keyTableSize += layoutRow.length;
        }
        keyTable = new View[keyTableSize];
        int counter = 0;
        int yCounter = 0;
        for (float[] layoutRow : LAYOUT_TABLE) {
            LinearLayout col = new LinearLayout(getContext(), null);
            col.setOrientation(HORIZONTAL);
            this.addView(col, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
            layoutTable[yCounter] = col;
            for (float layoutCol : layoutRow) {
                Button button = new Button(getContext(), null, R.attr.imeButtonStyle);
                col.addView(button, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, layoutCol));
                button.setText(Integer.toString(counter));
                keyTable[counter] = button;
                counter += 1;
            }
            yCounter += 1;
        }
    }
}
