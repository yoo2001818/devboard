package com.example.devboard;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

    @Override
    protected void onPause() {
        saveLayout();
        if (DevBoardIME.getInstance() != null) {
            DevBoardIME.getInstance().loadConfiguration();
        }
        super.onPause();
    }

    private void loadLayouts() {
        // :P
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        KeyLayout currentLayout = null;
        KeyLayout defaultLayout = null;
        if (pref.contains(DevBoardIME.LAYOUT_DATA)) {
            currentLayout = gson.fromJson(pref.getString(DevBoardIME.LAYOUT_DATA, ""), KeyLayout.class);
        }
        try {
            Reader reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.default_layout), "UTF-8"));
            // Load default layout
            defaultLayout = gson.fromJson(reader, KeyLayout.class);
        } catch (UnsupportedEncodingException e) {
            // This shouldn't happen
        }
        try {
            if (currentLayout == null) currentLayout = defaultLayout.clone();
        } catch (CloneNotSupportedException e) {
            // ...
        }
        currentLayout.setPrimary(true);
        // And... load candidates.
        Type listType = new TypeToken<List<KeyLayout>>(){}.getType();
        layouts = gson.fromJson(pref.getString("layouts", "[]"), listType);
        layouts.add(0, currentLayout);
        layouts.add(defaultLayout);
        // Finally, update combobox and the keyboard.
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, layouts);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i != selectedLayoutPos) {
                    // Okay?
                    swapLayout(i);
                    selectedLayout = spinnerAdapter.getItem(0);
                    selectedLayoutPos = 0;
                    setPage(0);
                    spinner.setSelection(0);
                    spinnerAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // Do nothing
            }
        });
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
                final Key key = (Key) presetView.getAdapter().getItem(i);
                if (selectedLayoutPos != 0) {
                    return;
                }
                writeKey(selectedPos, key);
            }
        });
    }

    private void swapLayout(int target) {
        KeyLayout targetLayout = layouts.get(target);
        KeyLayout primaryLayout = layouts.get(0);
        if (target == layouts.size() - 1) {
            // This case is different - we shouldn't swap it. Instead, we should copy them.
            layouts.add(target, primaryLayout);
            try {
                layouts.set(0, targetLayout.clone());
            } catch (CloneNotSupportedException e) {
                // This shouldn't occur.
            }
            targetLayout = layouts.get(0);
        } else {
            layouts.set(target, primaryLayout);
            layouts.set(0, targetLayout);
        }
        primaryLayout.setPrimary(false);
        targetLayout.setPrimary(true);
        spinnerAdapter.notifyDataSetChanged();
        saveLayout();
        saveLayouts();
    }

    private void saveLayout() {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString(DevBoardIME.LAYOUT_DATA, gson.toJson(layouts.get(0), KeyLayout.class));
        editor.apply();
    }

    private void saveLayouts() {
        Type listType = new TypeToken<List<KeyLayout>>(){}.getType();
        List<KeyLayout> saveList = layouts.subList(1, layouts.size() - 1);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString("layouts", gson.toJson(saveList, listType));
        editor.apply();
    }

    private void askText(String name, String value, final TextResultListener listener) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(name);
        alert.setMessage(value);

        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                listener.run(input.getText().toString());
            }
        });

        alert.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        alert.show();
    }

    private void writeKey(final int pos, Key key) {
        if (key.getCode() == -10000) {
            // Dictionary key
            askText("관용구 키", "누르면 입력할 관용구를 입력해 주세요.", new TextResultListener() {
                @Override
                public void run(final String value) {
                    askText("관용구 키", "키의 이름을 입력해 주세요.", new TextResultListener() {
                        @Override
                        public void run(String name) {
                            Key key = new Key(name, DevBoardIME.KEYCODE_SINGLE, Arrays.asList(value));
                            writeKey(pos, key);
                        }
                    });
                }
            });
            return;
        }
        if (key.getCode() == -10001) {
            // Dictionary list key
            askText("관용구 집합 키", "누르면 입력할 관용구들을 ,로 구분해서 입력해 주세요.", new TextResultListener() {
                @Override
                public void run(final String value) {
                    final List<String> values = Arrays.asList(value.split(","));
                    for (int i = 0; i < values.size(); ++i) {
                        values.set(i, values.get(i).trim());
                    }
                    askText("관용구 집합 키", "키의 이름을 입력해 주세요.", new TextResultListener() {
                        @Override
                        public void run(String name) {
                            Key key = new Key(name, DevBoardIME.KEYCODE_MULTIPLE, values);
                            writeKey(pos, key);
                        }
                    });
                }
            });
            return;
        }
        if (key.getCode() == -10002) {
            // Character list key
            askText("글자 집합 키", "누르면 입력할 글자들을 입력해 주세요.", new TextResultListener() {
                @Override
                public void run(final String value) {
                    String[] values = new String[value.length()];
                    for(int i = 0; i < values.length; i++)
                    {
                        values[i] = String.valueOf(value.charAt(i));
                    }
                    final List<String> valuesList = Arrays.asList(values);
                    askText("글자 집합 키", "키의 이름을 입력해 주세요.", new TextResultListener() {
                        @Override
                        public void run(String name) {
                            Key key = new Key(name, DevBoardIME.KEYCODE_MULTIPLE, valuesList);
                            writeKey(pos, key);
                        }
                    });
                }
            });
            return;
        }
        if (key.getCode() == DevBoardIME.KEYCODE_FN) {
            selectedLayout.getLayout().get(0).set(pos, key);
            selectedLayout.getLayout().get(1).set(pos, key);
        }
        if (key.getCode() == DevBoardIME.KEYCODE_FN2) {
            selectedLayout.getLayout().get(0).set(pos, key);
            selectedLayout.getLayout().get(2).set(pos, key);
        }
        currentKeys.set(pos, key);
        devBoardView.updateButton(pos, key);
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

    public void handleCopy() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("레이아웃 복사");
        alert.setMessage("레이아웃의 이름을 지정해 주세요.");

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        String tmp = selectedLayout.getName();
        if (tmp == null) tmp = "이름 없음";
        input.setText(tmp + " 복사");
        alert.setView(input);

        alert.setPositiveButton("복사", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();
                try {
                    KeyLayout newLayout = selectedLayout.clone();
                    newLayout.setName(value);
                    newLayout.setPrimary(true);
                    selectedLayout.setPrimary(false);
                    layouts.add(0, newLayout);
                    selectedLayout = newLayout;
                    selectedLayoutPos = 0;
                    spinnerAdapter.notifyDataSetChanged();
                    spinner.setSelection(selectedLayoutPos);
                    setPage(0);
                    saveLayout();
                    saveLayouts();
                } catch (CloneNotSupportedException e) {
                    // This shouldn't happen.
                    Log.e("LayoutActivity", e.toString());
                }
                return;
            }
        });

        alert.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                return;
            }
        });
        alert.show();
    }

    public void handleDelete() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("레이아웃 삭제");
        alert.setMessage("정말 레이아웃 " + selectedLayout.toString() + "을(를) 삭제하시겠습니까?");

        alert.setPositiveButton("삭제", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                layouts.remove(selectedLayoutPos);
                selectedLayoutPos = 0;
                selectedLayout = layouts.get(0);
                selectedLayout.setPrimary(true);
                spinnerAdapter.notifyDataSetChanged();
                spinner.setSelection(selectedLayoutPos);
                setPage(0);
                saveLayout();
                saveLayouts();
            }
        });

        alert.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                return;
            }
        });
        alert.show();
    }

    public void handleImport() {

    }

    public void handleExport() {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_layout, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.copyBtn:
                handleCopy();
                return true;
            case R.id.deleteBtn:
                handleDelete();
                return true;
            case R.id.importBtn:
                handleImport();
                return true;
            case R.id.exportBtn:
                handleExport();
                return true;
        }
        return false;
    }

    private interface TextResultListener {
        public void run(String result);
    }
}
