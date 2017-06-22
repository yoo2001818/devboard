package com.example.devboard;

import java.util.List;

/**
 * Created by yoo2001818 on 17. 6. 22.
 */

public class KeyPreset {

    public List<Category> categories;

    public List<Category> getCategories() {
        return categories;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }

    public class Category {
        public String name;
        public List<Entry> entries;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Entry> getEntries() {
            return entries;
        }

        public void setEntries(List<Entry> entries) {
            this.entries = entries;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public class Entry extends Key {
        public String name;

        public String getName() {
            if (name == null) return getLabel();
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return getName();
        }
    }
}
