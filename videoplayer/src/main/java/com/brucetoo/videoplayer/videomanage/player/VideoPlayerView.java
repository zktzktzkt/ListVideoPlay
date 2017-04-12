package com.brucetoo.videoplayer.videomanage.player;


import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.TextureView;

import com.brucetoo.videoplayer.Config;
import com.brucetoo.videoplayer.videomanage.interfaces.IMediaPlayer;
import com.brucetoo.videoplayer.videomanage.interfaces.VideoPlayerListener;
import com.brucetoo.videoplayer.utils.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This is player implementation based on {@link TextureView}
 * It encapsulates {@link MediaPlayer}.
 * <p>
 * It ensures that MediaPlayer methods are called from not main thread.
 * MediaPlayer methods are directly connected with hardware. That's why they should not be called from UI thread
 */
public class VideoPlayerView extends ScalableTextureView
    implements TextureView.SurfaceTextureListener,
    VideoPlayerListener {

    private static final boolean SHOW_LOGS = Config.SHOW_LOGS;
    private String TAG = VideoPlayerView.class.getSimpleName();

    private static final String IS_VIDEO_MUTED = "IS_VIDEO_MUTED";

    private IMediaPlayer mMediaPlayer;

    private TextureView.SurfaceTextureListener mLocalSurfaceTextureListener;

    private String mVideoPath;

    private final Set<VideoPlayerListener> mMediaPlayerMainThreadListeners = new HashSet<>();

    public VideoPlayerView(Context context) {
        super(context);
        initView();
    }

    public VideoPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public VideoPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public VideoPlayerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView();
    }

    private void checkThread() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new RuntimeException("cannot be in main thread");
        }
    }

    public void reset() {
        checkThread();
        if (mMediaPlayer != null)
            try {
                mMediaPlayer.reset();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public void release() {
        checkThread();
        if (mMediaPlayer != null)
            try {
                mMediaPlayer.release();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public void clearPlayerInstance() {
        if (SHOW_LOGS) Logger.v(TAG, ">> clearPlayerInstance");

        checkThread();

        if (mMediaPlayer != null)
            mMediaPlayer.clearAll();
        mMediaPlayer = null;

        if (SHOW_LOGS) Logger.v(TAG, "<< clearPlayerInstance");
    }

    public void createNewPlayerInstance() {
        if (SHOW_LOGS) Logger.v(TAG, ">> createNewPlayerInstance");

        if (SHOW_LOGS)
            Logger.v(TAG, "createNewPlayerInstance main Looper " + Looper.getMainLooper());
        if (SHOW_LOGS) Logger.v(TAG, "createNewPlayerInstance my Looper " + Looper.myLooper());

        checkThread();
        mMediaPlayer = new DefaultMediaPlayer(getContext(), this);

        SurfaceTexture texture = getSurfaceTexture();
        if (SHOW_LOGS) Logger.v(TAG, "texture " + texture);
        try {
            if (mMediaPlayer != null) {
                mMediaPlayer.setSurfaceTexture(texture);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (SHOW_LOGS) Logger.v(TAG, "<< createNewPlayerInstance");
    }

    public void prepare() {
        checkThread();
        try {
            if (mMediaPlayer != null) {
                mMediaPlayer.prepare();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        checkThread();
        try {
            if (mMediaPlayer != null) {
                mMediaPlayer.stop();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void notifyOnVideoStopped() {
        if (SHOW_LOGS) Logger.v(TAG, "notifyOnVideoStopped");
        List<VideoPlayerListener> listCopy;
        synchronized (mMediaPlayerMainThreadListeners) {
            listCopy = new ArrayList<>(mMediaPlayerMainThreadListeners);
        }
        for (VideoPlayerListener listener : listCopy) {
            listener.onVideoStoppedMainThread();
        }
    }

    public void start() {
        if (SHOW_LOGS) Logger.v(TAG, ">> start");
        if (mMediaPlayer != null) {
            try {
                if (mMediaPlayer != null) {
                    mMediaPlayer.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (SHOW_LOGS) Logger.v(TAG, "<< start");
    }

    private void initView() {
        if (!isInEditMode()) {
            if (SHOW_LOGS) Logger.v(TAG, "initView");
            setScaleType(ScalableTextureView.ScaleType.FILL);
            super.setSurfaceTextureListener(this);
        }
    }

    @Override
    public final void setSurfaceTextureListener(TextureView.SurfaceTextureListener listener) {
        mLocalSurfaceTextureListener = listener;
    }

    public void setDataSource(String path) {
        checkThread();
        if (SHOW_LOGS) Logger.v(TAG, "setDataSource, path " + path + ", this " + this);

        try {
            if (mMediaPlayer != null) {
                mMediaPlayer.setDataSource(path);
            }
        } catch (IOException e) {
            Logger.d(TAG, e.getMessage());
            throw new RuntimeException(e);
        }
        mVideoPath = path;
    }

    public void addMediaPlayerListener(VideoPlayerListener listener) {
        synchronized (mMediaPlayerMainThreadListeners) {
            mMediaPlayerMainThreadListeners.add(listener);
        }
    }

    public void removeMediaPlayerListener(VideoPlayerListener listener) {
        synchronized (mMediaPlayerMainThreadListeners) {
            mMediaPlayerMainThreadListeners.remove(this);
        }
    }

    public void removeAllPlayerListener() {
        synchronized (mMediaPlayerMainThreadListeners) {
            mMediaPlayerMainThreadListeners.clear();
        }
    }

    @Override
    public void onVideoSizeChangedMainThread(int width, int height) {

        if (SHOW_LOGS)
            Logger.v(TAG, ">> onVideoSizeChangedMainThread, width " + width + ", height " + height);

        if (width != 0 && height != 0) {
            setContentWidth(width);
            setContentHeight(height);
            updateTextureViewSize();
        }

        notifyOnVideoSizeChangedMainThread(width, height);

        if (SHOW_LOGS)
            Logger.v(TAG, "<< onVideoSizeChangedMainThread, width " + width + ", height " + height);
    }

    private void notifyOnVideoSizeChangedMainThread(int width, int height) {
        if (SHOW_LOGS)
            Logger.v(TAG, "notifyOnVideoSizeChangedMainThread, width " + width + ", height " + height);
        List<VideoPlayerListener> listCopy;
        synchronized (mMediaPlayerMainThreadListeners) {
            listCopy = new ArrayList<>(mMediaPlayerMainThreadListeners);
        }
        for (VideoPlayerListener listener : listCopy) {
            listener.onVideoSizeChangedMainThread(width, height);
        }
    }

    @Override
    public void onVideoCompletionMainThread() {
        notifyOnVideoCompletionMainThread();
    }

    private void notifyOnVideoCompletionMainThread() {
        if (SHOW_LOGS) Logger.v(TAG, "notifyVideoCompletionMainThread");
        List<VideoPlayerListener> listCopy;
        synchronized (mMediaPlayerMainThreadListeners) {
            listCopy = new ArrayList<>(mMediaPlayerMainThreadListeners);
        }
        for (VideoPlayerListener listener : listCopy) {
            listener.onVideoCompletionMainThread();
        }
    }

    private void notifyOnVideoPreparedMainThread() {
        if (SHOW_LOGS) Logger.v(TAG, "notifyOnVideoPreparedMainThread");
        List<VideoPlayerListener> listCopy;
        synchronized (mMediaPlayerMainThreadListeners) {
            listCopy = new ArrayList<>(mMediaPlayerMainThreadListeners);
        }
        for (VideoPlayerListener listener : listCopy) {
            listener.onVideoPreparedMainThread();
        }
    }

    private void notifyOnErrorMainThread(int what, int extra) {
        if (SHOW_LOGS) Logger.v(TAG, "notifyOnErrorMainThread");
        List<VideoPlayerListener> listCopy;
        synchronized (mMediaPlayerMainThreadListeners) {
            listCopy = new ArrayList<>(mMediaPlayerMainThreadListeners);
        }
        for (VideoPlayerListener listener : listCopy) {
            listener.onErrorMainThread(what, extra);
        }
    }

    @Override
    public void onVideoPreparedMainThread() {
        notifyOnVideoPreparedMainThread();
    }

    @Override
    public void onErrorMainThread(final int what, final int extra) {
        if (SHOW_LOGS) Logger.v(TAG, "onErrorMainThread, this " + VideoPlayerView.this);
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                if (SHOW_LOGS) Logger.v(TAG, "onErrorMainThread, what MEDIA_ERROR_SERVER_DIED");
                printErrorExtra(extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                if (SHOW_LOGS) Logger.v(TAG, "onErrorMainThread, what MEDIA_ERROR_UNKNOWN");
                printErrorExtra(extra);
                break;
        }

        notifyOnErrorMainThread(what, extra);
    }

    @Override
    public void onBufferingUpdateMainThread(int percent) {
        notifyBufferingUpdate(percent);
    }

    private void notifyBufferingUpdate(int percent) {
        if (SHOW_LOGS) Logger.v(TAG, "notifyBufferingUpdate");
        List<VideoPlayerListener> listCopy;
        synchronized (mMediaPlayerMainThreadListeners) {
            listCopy = new ArrayList<>(mMediaPlayerMainThreadListeners);
        }
        for (VideoPlayerListener listener : listCopy) {
            listener.onBufferingUpdateMainThread(percent);
        }
    }

    @Override
    public void onVideoStoppedMainThread() {
        notifyOnVideoStopped();
    }

    @Override
    public void onInfoMainThread(int what) {
        notifyOnInfo(what);
    }

    private void notifyOnInfo(int what) {
        if (SHOW_LOGS) Logger.v(TAG, "notifyOnInfo");
        List<VideoPlayerListener> listCopy;
        synchronized (mMediaPlayerMainThreadListeners) {
            listCopy = new ArrayList<>(mMediaPlayerMainThreadListeners);
        }
        for (VideoPlayerListener listener : listCopy) {
            listener.onInfoMainThread(what);
        }
    }

    private void printErrorExtra(int extra) {
        switch (extra) {
            case MediaPlayer.MEDIA_ERROR_IO:
                if (SHOW_LOGS) Logger.v(TAG, "error extra MEDIA_ERROR_IO");
                break;
            case MediaPlayer.MEDIA_ERROR_MALFORMED:
                if (SHOW_LOGS) Logger.v(TAG, "error extra MEDIA_ERROR_MALFORMED");
                break;
            case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                if (SHOW_LOGS) Logger.v(TAG, "error extra MEDIA_ERROR_UNSUPPORTED");
                break;
            case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                if (SHOW_LOGS) Logger.v(TAG, "error extra MEDIA_ERROR_TIMED_OUT");
                break;
        }
    }


    public void muteVideo() {
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putBoolean(IS_VIDEO_MUTED, true).commit();
        try {
            if (mMediaPlayer != null) {
                mMediaPlayer.setVolume(0, 0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void unMuteVideo() {
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putBoolean(IS_VIDEO_MUTED, false).commit();
        try {
            if (mMediaPlayer != null) {
                mMediaPlayer.setVolume(1, 1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isAllVideoMute() {
        return PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(IS_VIDEO_MUTED, false);
    }

    public void pause() {
        if (SHOW_LOGS) Logger.d(TAG, ">> pause ");
        try {
            if (mMediaPlayer != null) {
                mMediaPlayer.pause();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (SHOW_LOGS) Logger.d(TAG, "<< pause");
    }

    /**
     * @see MediaPlayer#getDuration()
     */
    public int getDuration() {
        try {
            if (mMediaPlayer != null) {
                return mMediaPlayer.getDuration();
            } else {
                return 0;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        if (SHOW_LOGS)
            Logger.v(TAG, "onSurfaceTextureAvailable, width " + width + ", height " + height + ", this " + this);
        if (mLocalSurfaceTextureListener != null) {
            mLocalSurfaceTextureListener.onSurfaceTextureAvailable(surfaceTexture, width, height);
        }

        if (mMediaPlayer != null) {
            try {
                mMediaPlayer.setSurfaceTexture(getSurfaceTexture());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        if (mLocalSurfaceTextureListener != null) {
            mLocalSurfaceTextureListener.onSurfaceTextureSizeChanged(surface, width, height);
        }
    }

    /**
     * Note : this method might be called after {@link #onDetachedFromWindow()}
     *
     * @param surface
     * @return
     */
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (SHOW_LOGS) Logger.v(TAG, "onSurfaceTextureDestroyed, surface " + surface);

        if (mLocalSurfaceTextureListener != null) {
            mLocalSurfaceTextureListener.onSurfaceTextureDestroyed(surface);
        }

        // We have to release this surface manually for better control.
        // Also we do this because we return false from this method
        surface.release();
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        if (mLocalSurfaceTextureListener != null) {
            mLocalSurfaceTextureListener.onSurfaceTextureUpdated(surface);
        }
    }

    public IMediaPlayer getMediaPlayer() {
        return mMediaPlayer;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + hashCode();
    }
}
