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

    private float volume = 1f;
    private int streamType;

    private Random random = new Random();

    public SoundPlayer(Context context) {
        this(context, AudioManager.STREAM_SYSTEM);
    }

    public SoundPlayer(Context context, int streamType) {
        this.streamType = streamType;
        pool = new SoundPool(4, streamType, 0);

        pressSounds = new int[4];
        releaseSounds = new int[5];
        int i = 0;

        // press 1 is too "pointy"
        for (int resId : new int[] {
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
        pool.play(pressSounds[random.nextInt(pressSounds.length)], volume, volume, 0, 0, 1);
    }

    public void playRelease() {
        pool.play(releaseSounds[random.nextInt(releaseSounds.length)], volume, volume, 0, 0, 1);
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    // Cleanup
    public void release() {
        pool.release();
    }
}
