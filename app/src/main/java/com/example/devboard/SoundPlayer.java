package com.example.devboard;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import java.util.Random;

/**
 * Created by yoo2001818 on 17. 6. 20.
 */

public class SoundPlayer {
    private SoundPool pool;
    private int[] pressSounds;
    private int[] releaseSounds;

    private Random random = new Random();

    public SoundPlayer(Context context) {

        pool = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);

        pressSounds = new int[5];
        releaseSounds = new int[5];
        int i = 0;

        // press 1 is too "pointy"
        for (int resId : new int[] {
                R.raw.keyboard_press2,
                R.raw.keyboard_press2,
                R.raw.keyboard_press3,
                R.raw.keyboard_press4,
                R.raw.keyboard_press5
        }) {
            pressSounds[i] = pool.load(context, resId, 1);
            i += 1;
        }

        i = 0;

        for (int resId : new int[] {
                R.raw.keyboard_release,
                R.raw.keyboard_release2,
                R.raw.keyboard_release3,
                R.raw.keyboard_release4,
                R.raw.keyboard_release5
        }) {
            releaseSounds[i] = pool.load(context, resId, 1);
            i += 1;
        }
    }

    public void playPress() {
        pool.play(pressSounds[random.nextInt(pressSounds.length)], 1f, 1f, 0, 0, 1);
    }

    public void playRelease() {
        pool.play(releaseSounds[random.nextInt(releaseSounds.length)], 1f, 1f, 0, 0, 1);
    }
    // Cleanup
    public void release() {
        pool.release();
    }
}
