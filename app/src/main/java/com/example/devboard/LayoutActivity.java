package com.example.devboard;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class LayoutActivity extends AppCompatActivity {

    Gson gson = new Gson();

    int selectedPos = -1;
    int selectedLayoutPos = 0;
    int selectedPage = 0;
    List<Key> currentKeys;
    KeyLayout selectedLayout;
    List<KeyLayout> layouts;
    ArrayAdapter<KeyLayout> spinnerAdapter;
    DevBoardView devBoardView;
    Button[] pageButtons;
    Spinner spinner;

    ListView categoryView;
    ListView presetView;

    List<KeyPreset.Category> categories;
    ArrayAdapter<KeyPreset.Category> categoryAdapter;
    List<ArrayAdapter<KeyPreset.Entry>> entryAdapters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_layout);
        // Load theme of keyboard - it's not the subject to change.
        LinearLayout l = (LinearLayout) findViewById(R.id.devboardScaffold);
        int currentTheme = ThemeLoader.getTheme(this);
        devBoardView = new DevBoardView(new ContextThemeWrapper(this, currentTheme));
        devBoardView.setListener(new DevBoardView.Listener() {
            @Override
            public void onPress(int id, Key key) {
                handleClick(id, key);
            }

            @Override
            public boolean onLongPress(int id, Key key) {
                return false;
            }

            @Override
            public void onRelease(int id, Key key) {

            }

            @Override
            public void onKey(int id, Key key) {

            }
        });
        l.addView(devBoardView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        pageButtons = new Button[] {
                (Button) findViewById(R.id.normalBtn),
                (Button) findViewById(R.id.fnBtn),
                (Button) findViewById(R.id.fn2Btn)
        };
        // Haha! did you know lambdas in Java are really messed up?
        // Because I do! :(
        // Srsly I hate Java
        for (int i = 0; i < pageButtons.length; ++i) {
            final int j = i;
            pageButtons[i].setOnClickListener(new View.OnClickListener() {
                int assignedVal = j;
                @Override
                public void onClick(View view) {
                    setPage(assignedVal);
                }
            });
        }
        spinner = (Spinner) findViewById(R.id.spinner);
        categoryView = (ListView) findViewById(R.id.categoryView);
        presetView = (ListView) findViewById(R.id.presetView);
        // Load complete list of layouts - then append '(Current)' on top.
        this.loadLayouts();
        // Then load presets.
        this.loadPresets();
    }

    private void loadLayouts() {
        // :P
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        KeyLayout currentLayout = null;
        if (pref.contains(DevBoardIME.LAYOUT_DATA)) {
            currentLayout = gson.fromJson(pref.getString(DevBoardIME.LAYOUT_DATA, ""), KeyLayout.class);
        }
        if (currentLayout == null) {
            try {
                Reader reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.default_layout), "UTF-8"));
                // Load default layout
                currentLayout = gson.fromJson(reader, KeyLayout.class);
            } catch (UnsupportedEncodingException e) {
                // This shouldn't happen
            }
        }
        // And... load candidates.
        Type listType = new TypeToken<List<KeyLayout>>(){}.getType();
        layouts = gson.fromJson(pref.getString("layouts", "[]"), listType);
        layouts.add(0, currentLayout);
        // Finally, update combobox and the keyboard.
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, layouts);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);
        selectedLayoutPos = 0;
        selectedLayout = layouts.get(selectedLayoutPos);
        setPage(0);
    }

    // Is it simple enough?
    private void loadPresets() {
        KeyPreset preset = null;
        try {
            Reader reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.key_presets), "UTF-8"));
            // Load default layout
            preset = gson.fromJson(reader, KeyPreset.class);
        } catch (UnsupportedEncodingException e) {
            // This shouldn't happen
        }
        // This shouldn't happen, too.
        if (preset == null) throw new NullPointerException("preset is null");
        categories = preset.getCategories();
        entryAdapters = new ArrayList<>();
        for (KeyPreset.Category category : categories) {
            // Add entryAdapter for each categories. Simple enough?
            entryAdapters.add(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, category.getEntries()));
        }
        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, categories);
        categoryView.setAdapter(categoryAdapter);

        categoryView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                presetView.setAdapter(entryAdapters.get(i));
            }
        });
        presetView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                // Set current preset's value to the key!
                if (selectedPos == -1) return;
                Key key = (Key) presetView.getAdapter().getItem(i);
                currentKeys.set(selectedPos, key);
                devBoardView.updateButton(selectedPos, key);
            }
        });
    }

    // Oh no
    private void setPage(int page) {
        selectedPage = page;
        for (int i = 0; i < pageButtons.length; ++i) {
            if (i != page) {
                pageButtons[i].getBackground().clearColorFilter();
            } else {
                pageButtons[i].getBackground().setColorFilter(Color.parseColor("#aaaaff"),
                        PorterDuff.Mode.MULTIPLY);
            }
        }
        currentKeys = selectedLayout.getLayout().get(selectedPage);
        devBoardView.setKeyLayout(currentKeys);
    }

    private void handleClick(int pos, Key key) {
        if (selectedPos != -1) {
            Button prevButton = devBoardView.getButton(selectedPos);
            prevButton.getBackground().clearColorFilter();
            prevButton.invalidate();
        }
        Button button = devBoardView.getButton(pos);
        button.getBackground().setColorFilter(Color.GRAY,
                PorterDuff.Mode.SRC);
        button.invalidate();
        selectedPos = pos;
    }
}
