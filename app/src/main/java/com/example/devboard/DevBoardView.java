package com.example.devboard;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * Created by yoo2001818 on 17. 6. 18.
 */

public class DevBoardView extends LinearLayout {

    // The keyboard table is hardcoded - it's not supposed to change whatsoever!
    public static final float[][] LAYOUT_TABLE = new float[][] {
            new float[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 },
            new float[] { -0.5f, 1, 1, 1, 1, 1, 1, 1, 1, 1, -0.5f },
            new float[] { 1f, 1, 1, 1, 1, 1, 1, 1, 1, 1f },
            new float[] { 1, 1, 1, 4, 1, 1, 1 },
    };

    private AttributeSet attrs;

    protected KeyLayout keyLayout;
    protected LinearLayout[] rows;
    protected Button[] buttons;

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
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.DevBoardView);
        this.setBackgroundColor(a.getColor(R.styleable.DevBoardView_imeBackground, 0));
        this.setOrientation(VERTICAL);
        // Create layout according to the layout table.
        rows = new LinearLayout[LAYOUT_TABLE.length];
        // Count all keys and get the size.
        int keyTableSize = 0;
        for (float[] layoutRow : LAYOUT_TABLE) {
            for (float layoutSize : layoutRow) {
                if (layoutSize >= 0) keyTableSize += 1;
            }
        }
        buttons = new Button[keyTableSize];
        int counter = 0;
        int yCounter = 0;
        for (float[] layoutRow : LAYOUT_TABLE) {
            LinearLayout col = new LinearLayout(getContext(), null);
            col.setOrientation(HORIZONTAL);
            this.addView(col, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
            rows[yCounter] = col;
            for (float layoutCol : layoutRow) {
                if (layoutCol >= 0) {
                    Button button = new Button(getContext(), null, R.attr.imeButtonStyle);
                    col.addView(button, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, layoutCol));
                    button.setText(Integer.toString(counter));
                    buttons[counter] = button;
                    counter += 1;
                } else {
                    // Just add empty view
                    LinearLayout layout = new LinearLayout(getContext(), null);
                    col.addView(layout, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, -layoutCol));
                }
            }
            yCounter += 1;
        }
        this.updateLayout();
    }

    private void updateLayout() {
        // TODO Move to somewhere else
        try {
            Reader reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.default_layout), "UTF-8"));
            Gson gson = new Gson();
            this.keyLayout = gson.fromJson(reader, KeyLayout.class);
        } catch (UnsupportedEncodingException e) {
            // This shouldn't happen
        }
        List<Key> keys = keyLayout.getLayout();
        int counter = 0;
        for (Key key : keys) {
            buttons[counter].setText(key.getLabel());
            counter += 1;
        }
    }
}
