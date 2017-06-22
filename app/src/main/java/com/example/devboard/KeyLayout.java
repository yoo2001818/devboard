package com.example.devboard;

import java.util.List;

/**
 * Created by yoo2001818 on 17. 6. 18.
 */

public class KeyLayout implements Cloneable {

    public transient boolean isPrimary;

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

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }

    @Override
    public String toString() {
        return (this.name == null ? "이름 없음" : this.name) + (isPrimary ? " (현재)" : "");
    }

    public KeyLayout clone() throws CloneNotSupportedException {
        return (KeyLayout)(super.clone());
    }
}
