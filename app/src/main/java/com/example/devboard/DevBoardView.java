package com.example.devboard;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ScaleDrawable;
import android.os.Handler;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
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

public class DevBoardView extends LinearLayout implements View.OnClickListener, View.OnTouchListener, View.OnLongClickListener {

    // The keyboard table is hardcoded - it's not supposed to change whatsoever!
    public static final float[][] LAYOUT_TABLE = new float[][] {
            new float[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 },
            new float[] { -0.5f, 1, 1, 1, 1, 1, 1, 1, 1, 1, -0.5f },
            new float[] { 1f, 1, 1, 1, 1, 1, 1, 1, 1, 1f },
            new float[] { 1, 1, 1, 1, 3, 1, 1, 1 },
    };

    private Handler handler;
    private AttributeSet attrs;
    private Listener listener;
    private int height;

    protected List<Key> keyLayout;
    protected LinearLayout[] rows;
    protected Button[] buttons;
    protected Runnable[] runnables;

    public DevBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setLayoutParams(new ViewGroup.LayoutParams(this.getContext(), attrs));
        this.attrs = attrs;
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.DevBoardView);
        height = a.getDimensionPixelSize(R.styleable.DevBoardView_imeHeight, 100);
        a.recycle();
        this.createLayout();
    }

    public DevBoardView(Context context) {
        super(context);
        this.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.DevBoardView);
        height = a.getDimensionPixelSize(R.styleable.DevBoardView_imeHeight, 100);
        a.recycle();
        this.createLayout();
    }

    public List<Key> getKeyLayout() {
        return keyLayout;
    }

    public void setHeight(int dp) {
        Resources r = getResources();
        height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
    }

    public void setKeyLayout(List<Key> keyLayout) {
        this.keyLayout = keyLayout;
        this.updateLayout();
    }

    private int getKeyHeight() {
        return height / LAYOUT_TABLE.length;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height);
    }

    private void createLayout() {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.DevBoardView);
        this.setBackgroundColor(a.getColor(R.styleable.DevBoardView_imeBackground, 0));
        a.recycle();
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
        runnables = new Runnable[keyTableSize];
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
                    button.setId(counter);
                    button.setOnClickListener(this);
                    button.setOnTouchListener(this);
                    button.setOnLongClickListener(this);
                    button.setSoundEffectsEnabled(false);
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

    private void setButtonDrawable(Button button, int resourceId) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.DevBoardView);
        int iconSize = a.getDimensionPixelSize(R.styleable.DevBoardView_imeImageSize, 0);
        int color = a.getColor(R.styleable.DevBoardView_imeForeground, 0);
        a.recycle();

        Drawable drawable = DrawableCompat.wrap(getResources().getDrawable(resourceId));
        DrawableCompat.setTint(drawable, color);
        drawable.setBounds(0, 0, iconSize * 2, iconSize * 2);
        button.setPadding(0, (getKeyHeight() - iconSize * 2) / 2, 0, 0);
        button.setCompoundDrawables(null, drawable, null, null);
        button.setText("");
    }

    private void setButtonText(Button button, String text) {
        button.setPadding(0, 0, 0, 0);
        button.setCompoundDrawables(null, null, null, null);
        button.setText(text);
    }

    private void updateLayout() {
        // TODO Move to somewhere else
        if (keyLayout == null) {
            try {
                Reader reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.default_layout), "UTF-8"));
                Gson gson = new Gson();
                this.keyLayout = gson.fromJson(reader, KeyLayout.class).getLayout().get(0);
            } catch (UnsupportedEncodingException e) {
                // This shouldn't happen
            }
        }
        List<Key> keys = keyLayout;
        int counter = 0;
        for (Key key : keys) {
            switch (key.getLabel()) {
                case "Shift":
                    setButtonDrawable(buttons[counter], R.drawable.icon_shift);
                    break;
                case "Locale":
                    setButtonDrawable(buttons[counter], R.drawable.icon_language);
                    break;
                case "Backspace":
                    setButtonDrawable(buttons[counter], R.drawable.icon_backspace);
                    break;
                case "Return":
                    setButtonDrawable(buttons[counter], R.drawable.icon_return);
                    break;
                case "Option":
                    setButtonDrawable(buttons[counter], R.drawable.icon_option);
                    break;
                case "Emoji":
                    setButtonDrawable(buttons[counter], R.drawable.icon_emoji);
                    break;
                case "Tab":
                    setButtonDrawable(buttons[counter], R.drawable.icon_tab);
                    break;
                case "Up":
                    setButtonDrawable(buttons[counter], R.drawable.icon_up);
                    break;
                case "Left":
                    setButtonDrawable(buttons[counter], R.drawable.icon_left);
                    break;
                case "Right":
                    setButtonDrawable(buttons[counter], R.drawable.icon_right);
                    break;
                case "Down":
                    setButtonDrawable(buttons[counter], R.drawable.icon_down);
                    break;
                default:
                    setButtonText(buttons[counter], key.getLabel());
                    break;
            }
            counter += 1;
        }
    }

    @Override
    public void onClick(View view) {
        // Do not process onClick - It should be processed with onTouch
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        Key key = keyLayout.get(view.getId());
        int id = view.getId();
        // Do not digest the event - underlying button should listen to this
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            if (listener != null) listener.onPress(id, key);
            if (listener != null) listener.onKey(view.getId(), key);
        } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            if (runnables[id] != null) {
                getHandler().removeCallbacks(runnables[id]);
                runnables[id] = null;
            }
            if (listener != null) listener.onRelease(id, key);
        }
        return false;
    }

    @Override
    public boolean onLongClick(View view) {
        // I hate Java.
        final int id = view.getId();
        final Key key = keyLayout.get(view.getId());
        if (listener != null && listener.onLongPress(id, key)) return true;
        if (runnables[id] != null) throw new RuntimeException("Runnable " + id + " is already present; this should not happen");
        runnables[id] = new Runnable() {
            @Override
            public void run() {
                if (listener != null) listener.onKey(id, key);
                getHandler().postDelayed(runnables[id], 50);
            }
        };
        runnables[id].run();
        return true;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        void onPress(int id, Key key);
        boolean onLongPress(int id, Key key);
        void onRelease(int id, Key key);
        void onKey(int id, Key key);
    }
}
