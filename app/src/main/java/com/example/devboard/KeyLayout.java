package com.example.devboard;

import java.util.List;

/**
 * Created by yoo2001818 on 17. 6. 18.
 */

public class KeyLayout {
    public String name;

    public List<List<Key>> layout;

    public List<List<Key>> getLayout() {
        return layout;
    }

    public void setLayout(List<List<Key>> layout) {
        this.layout = layout;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name == null ? "(현재)" : this.name;
    }
}
