package com.example.devboard;

/**
 * Created by yoo2001818 on 17. 6. 18.
 */

public class Key {
    // Since drawable resource ID is not portable, DevBoardView will take care of converting label
    // string to resource ID - it needs to be portable. :/
    String label;
    int code;

    public Key() {
    }

    public Key(String label, int code) {
        this.label = label;
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "Key{" +
                "label='" + label + '\'' +
                ", code=" + code +
                '}';
    }
}
