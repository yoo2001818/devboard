package com.example.devboard;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yoo2001818 on 17. 6. 19.
 */

// Based on JS version of dubeolsik IME made in 2014.
public class DubeolsikInputMethod implements CJKInputMethod {

    protected static final char[] START_CHARS = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ".toCharArray();
    protected static final char[] MIDDLE_CHARS = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ".toCharArray();
    protected static final char[] END_CHARS = " ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ".toCharArray();
    protected static final char START_CODE = '가'; // 0xAC00

    // Devboard -> QWERTY map
    protected static final char[] RAW_KEY_MAP = "qwertyuiopasdfghjkl\0zxcvbnm\0".toCharArray();

    // How should it be assembled in QWERTY keyboard? This seems to be quite inefficient, though.
    // If I had enough time to optimize this, it can be done efficiently by using a Trie.
    protected static final String[] START_CONV = "r,R,s,e,E,f,a,q,Q,t,T,d,w,W,c,z,x,v,g".split(",");
    protected static final String[] MIDDLE_CONV = "k,o,i,O,j,p,u,P,h,hk,ho,hl,y,n,nj,np,nl,b,m,ml,l".split(",");
    protected static final String[] END_CONV = ",r,R,rt,s,sw,sg,e,f,fr,fa,fq,ft,fx,fv,fg,a,q,qt,t,T,d,w,c,z,x,v,g".split(",");
    protected static final char[] VOWEL_CONV = "yuiophjklbnm".toCharArray();
    protected static final char[] CONSONANT_CONV = "qwertasdfgzxcv".toCharArray();

    protected static final char[] CONVERT_CHARS = "ㅂㅈㄷㄱㅅㅛㅕㅑㅐㅔㅁㄴㅇㄹㅎㅗㅓㅏㅣ\0ㅋㅌㅊㅍㅠㅜㅡ\0".toCharArray();
    protected static final char[] CONVERT_SHIFT_CHARS = "ㅃㅉㄸㄲㅆㅛㅕㅑㅒㅖㅁㄴㅇㄹㅎㅗㅓㅏㅣ\0ㅋㅌㅊㅍㅠㅜㅡ\0".toCharArray();

    private int startCode = -1;
    private int middleCode = -1;
    private int endCode = 0;
    private StringBuilder queue = new StringBuilder();
    private StringBuilder latinQueue = new StringBuilder();

    // Why doesn't Java provide this by default? Also, I hate primitive types.
    private static int indexOf(char needle, char[] haystack) {
        for (int i = 0; i < haystack.length; i++) {
            if (haystack[i] == needle) return i;
        }

        return -1;
    }
    private static <T> int indexOf(T needle, T[] haystack) {
        for (int i = 0; i < haystack.length; i++) {
            if (haystack[i] != null && haystack[i].equals(needle)
                    || needle == null && haystack[i] == null) return i;
        }

        return -1;
    }

    private boolean isVowel(char code) {
        return indexOf(code, VOWEL_CONV) != -1;
    }

    private boolean isConsonant(char code) {
        return indexOf(code, CONSONANT_CONV) != -1;
    }

    private int getStartIndex(String code) {
        int orig = indexOf(code, START_CONV);
        if (orig != -1) return orig;
        return indexOf(code.toLowerCase(), START_CONV);
    }

    private int getMiddleIndex(String code) {
        int orig = indexOf(code, MIDDLE_CONV);
        if (orig != -1) return orig;
        return indexOf(code.toLowerCase(), MIDDLE_CONV);
    }

    private int getEndIndex(String code) {
        int orig = indexOf(code, END_CONV);
        if (orig != -1) return orig;
        orig = indexOf(code.toLowerCase(), END_CONV);
        if (orig != -1) return orig;
        return 0;
    }

    private char createHangul(int start, int middle, int end) {
        int startCode = start * MIDDLE_CHARS.length * END_CHARS.length;
        int middleCode = middle * END_CHARS.length;
        int endCode = end;
        return (char)(START_CODE + startCode + middleCode + endCode);
    }

    @Override
    public void reset() {
        startCode = -1;
        middleCode = -1;
        endCode = 0;
        // queue.setLength(0);
    }

    public char getChar() {
        if (middleCode == -1 && startCode == -1) {
            return '\0';
        } else if (middleCode == -1) {
            return START_CHARS[startCode];
        } else if (startCode == -1) {
            return MIDDLE_CHARS[middleCode];
        } else {
            return createHangul(startCode, middleCode, endCode);
        }
    }

    public void pushQueue() {
        queue.append(getChar());
    }

    @Override
    public boolean processDevboard(int position, boolean shift) {
        if (position >= RAW_KEY_MAP.length) return false;
        char input = RAW_KEY_MAP[position];
        if (input == 0) return false;
        // This is absurd.
        if (shift) input = (char)(input - 'a' + 'A');
        return process(input);
    }

    @Override
    public boolean process(int charValue) {
        char charCode = (char) charValue;
        String charStr = Character.toString(charCode);
        // Code must be in ASCII range - that is:
        if (!('a' <= charCode && charCode <= 'z') && !('A' <= charCode && charCode <= 'Z')) {
            // Send it to default processor.
            this.pushQueue();
            this.reset();
            return false;
        }
        latinQueue.append(charStr);
        // Digest the character!
        if (isConsonant(charCode)) {
            if (startCode == -1) {
                // Start code is not set yet
                startCode = getStartIndex(charStr);
            } else if (this.middleCode == -1) {
                // Middle code is not set yet
                // Just reset
                this.pushQueue();
                this.reset();
                this.startCode = getStartIndex(charStr);
            } else {
                // Set end code
                int endTemp = getEndIndex(END_CONV[this.endCode] + charStr);
                if (endTemp == 0) {
                    // requested character does not exist
                    this.pushQueue();
                    this.reset();
                    this.startCode = getStartIndex(charStr);
                } else {
                    this.endCode = endTemp;
                }
            }
        } else if (isVowel(charCode)) {
            if (this.endCode == 0) {
                // Set middle code
                int middleTemp;
                if (this.middleCode == -1) {
                    middleTemp = getMiddleIndex(charStr);
                } else {
                    middleTemp = getMiddleIndex(MIDDLE_CONV[this.middleCode] + charStr);
                }
                if (middleTemp == -1) {
                    // Start new char
                    this.pushQueue();
                    this.reset();
                    this.middleCode = getMiddleIndex(charStr);
                } else {
                    this.middleCode = middleTemp;
                }
            } else {
                // Try to 'subtract' end code
                String endChar = END_CONV[this.endCode];
                int newStart;
                if (endChar.length() == 1) {
                    this.endCode = 0;
                    newStart = getStartIndex(endChar);
                } else {
                    this.endCode = getEndIndex(endChar.substring(0, endChar.length() - 1));
                    newStart = getStartIndex(endChar.substring(endChar.length() - 1));
                }
                this.pushQueue();
                this.reset();
                this.startCode = newStart;
                this.middleCode = getMiddleIndex(charStr);
            }
        } else {
            // Finish char right now
            this.pushQueue();
            this.reset();
            this.queue.append(charStr);
        }
        return true;
    }

    @Override
    public boolean backspace() {
        // No!!!!!!!! This is not possible without using a stack.
        // https://www.youtube.com/watch?v=umDr0mPuyQc
        // ..... Anyway, we can use two methods!
        // 1) Use StringBuilder to store raw char values, and process all of them again everytime.
        // 2) Store each char's state - record if the completed character has been published -
        //    while restoring, check the flag and delete StringBuffer's each character.
        // We'll use the first method due to its simplicity.
        if (latinQueue.length() == 0) return false;
        this.pushQueue();
        this.reset();
        this.queue.setLength(0);
        char[] reprocessed = latinQueue.toString().toCharArray();
        latinQueue.setLength(0);
        // Heh.
        for (int i = 0; i < reprocessed.length - 1; ++i) {
            char c = reprocessed[i];
            process(c);
        }

        return true;
    }

    @Override
    public String finish() {
        this.pushQueue();
        this.reset();
        String output = this.queue.toString();
        this.queue.setLength(0);
        latinQueue.setLength(0);
        return output;
    }

    @Override
    public String getCurrent() {
        char temp = this.getChar();
        if (temp == '\0') return this.queue.toString();
        return this.queue.toString() + temp;
    }

    @Override
    public List<Key> getLayout(List<Key> original) {
        // Just convert them using a table.
        List<Key> output = new ArrayList<>();
        for (int i = 0; i < original.size(); ++i) {
            Key key = original.get(i);
            if (i < CONVERT_CHARS.length && CONVERT_CHARS[i] != 0) {
                output.add(new Key(Character.toString(CONVERT_CHARS[i]), key.getCode(), key.getExtra()));
            } else {
                output.add(key);
            }
        }
        return output;
    }

    @Override
    public List<Key> getLayoutShift(List<Key> original) {
        // Bleh.
        List<Key> output = new ArrayList<>();
        for (int i = 0; i < original.size(); ++i) {
            Key key = original.get(i);
            if (i < CONVERT_SHIFT_CHARS.length && CONVERT_SHIFT_CHARS[i] != 0) {
                output.add(new Key(Character.toString(CONVERT_SHIFT_CHARS[i]), key.getCode(), key.getExtra()));
            } else {
                output.add(key);
            }
        }
        return output;
    }
}
