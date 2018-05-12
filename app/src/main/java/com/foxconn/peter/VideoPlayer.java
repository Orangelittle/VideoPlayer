package com.foxconn.peter;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by peter on 2018/5/11.
 **/
public class VideoPlayer extends FrameLayout implements TextureView.SurfaceTextureListener, VideoPlayerInterFace {

    private TextureView mTextureView; //映射视图
    private SurfaceTexture mSurfaceTexture;
    private VideoController mVideoController;
    private MediaPlayer mMediaPlayer;
    private Context mContext;
    private FrameLayout mContainer;//窗口切换容器
    private String vedioUrl;
    private int mBufferPercentage;

    // TODO: 2018/5/11 重播白屏、正常模式隐藏topbar、获取第一帧画面 
    
    /**
     * MediaPlayer 状态值
     */
    public static final int STATE_ERROR = -1;          // 播放错误
    public static final int STATE_IDLE = 0;            // 播放未开始
    public static final int STATE_PREPARING = 1;       // 播放准备中
    public static final int STATE_PREPARED = 2;        // 播放准备就绪
    public static final int STATE_PLAYING = 3;         // 正在播放
    public static final int STATE_PAUSED = 4;          // 暂停播放
    /**
     * 正在缓冲(播放器正在播放时，缓冲区数据不足，进行缓冲，缓冲区数据足够后恢复播放)
     **/
    public static final int STATE_BUFFERING_PLAYING = 5;
    /**
     * 正在缓冲(播放器正在播放时，缓冲区数据不足，进行缓冲，此时暂停播放器，继续缓冲，缓冲区数据足够后恢复暂停)
     **/
    public static final int STATE_BUFFERING_PAUSED = 6;
    public static final int STATE_COMPLETED = 7;       // 播放完成
    private int mPlayerState;


    public static final int MODE_NORMAL = 0; //普通模式
    public static final int MODE_FULL_SCREEN = 1; //全屏模式
    public static final int MODE_TINY_WINDOW = 2; //小窗模式
    private int mCurrentMode = MODE_NORMAL;


    public VideoPlayer(@NonNull Context context) {
        super(context);
    }

    public VideoPlayer(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context ;
        initContainer();
    }


    private void initContainer() {
        mContainer = new FrameLayout(mContext);
        mContainer.setBackgroundColor(Color.BLACK);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        this.addView(mContainer, params);
    }

    private void initMediaPlayer() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setScreenOnWhilePlaying(true); //视频播放时，保持屏幕长亮

            mMediaPlayer.setOnPreparedListener(mOnPreparedListener);
            mMediaPlayer.setOnVideoSizeChangedListener(mOnVideoSizeChangedListener);
            mMediaPlayer.setOnInfoListener(mOnInfoListener);
            mMediaPlayer.setOnBufferingUpdateListener(mOnBufferingUpdateListener);
            mMediaPlayer.setOnCompletionListener(mOnCompletionListener);
            mMediaPlayer.setOnErrorListener(mOnErrorListener);
        }
    }

    private void initTextureView() {
        if (mTextureView == null) {
            mTextureView = new TextureView(mContext);
            mTextureView.setSurfaceTextureListener(this);
        }
    }

    private void addTextureView() {
        mContainer.removeView(mTextureView);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mContainer.addView(mTextureView, 0, params); //添加子元素到第一个
    }


    /**
     * 设置播放器地址
     *
     * @param url
     */
    public void setVideoUrl(String url) {
        this.vedioUrl = url;
    }

    /**
     * 设置控制器
     * @param videoController
     */
    public void setVideoController(VideoController videoController){
        mVideoController = videoController;

        mVideoController.setVideoPlayer(this);
        mContainer.removeView(mVideoController);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mContainer.addView(mVideoController,params);

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        //数据准备就绪回调
        if (mSurfaceTexture == null) {
            mSurfaceTexture = surface;
            openMediaPlayer();
        } else {
            mTextureView.setSurfaceTexture(mSurfaceTexture);
        }
    }

    private void openMediaPlayer() {

        try {
            mMediaPlayer.setDataSource(mContext, Uri.parse(vedioUrl));
            mMediaPlayer.setSurface(new Surface(mSurfaceTexture));
            mMediaPlayer.prepareAsync();
            mPlayerState = STATE_PREPARING;
            mVideoController.setControllerState(mCurrentMode, mPlayerState);
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("自定义标签", "openMediaPlayer:====打开播放器失败" + e.toString());
        }

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return mSurfaceTexture == null;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    private MediaPlayer.OnPreparedListener mOnPreparedListener
            = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            mp.start();
            mPlayerState = STATE_PREPARED;
            mVideoController.setControllerState(mCurrentMode, mPlayerState);
        }
    };

    private MediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangedListener
            = new MediaPlayer.OnVideoSizeChangedListener() {
        @Override
        public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        }
    };

    private MediaPlayer.OnCompletionListener mOnCompletionListener
            = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            mPlayerState = STATE_COMPLETED;
            mVideoController.setControllerState(mCurrentMode, mPlayerState);
        }
    };

    private MediaPlayer.OnErrorListener mOnErrorListener
            = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            mPlayerState = STATE_ERROR;
            mVideoController.setControllerState(mCurrentMode, mPlayerState);
            return false;
        }
    };

    private MediaPlayer.OnInfoListener mOnInfoListener
            = new MediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(MediaPlayer mp, int what, int extra) {
            if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                // 播放器开始渲染
                mPlayerState = STATE_PLAYING;
                mVideoController.setControllerState(mCurrentMode, mPlayerState);
            } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                // MediaPlayer暂时不播放，以缓冲更多的数据
                if (mPlayerState == STATE_PAUSED || mPlayerState == STATE_BUFFERING_PAUSED) {
                    mPlayerState = STATE_BUFFERING_PAUSED;
                } else {
                    mPlayerState = STATE_BUFFERING_PLAYING;
                }
                mVideoController.setControllerState(mCurrentMode, mPlayerState);
            } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                // 填充缓冲区后，MediaPlayer恢复播放/暂停
                if (mPlayerState == STATE_BUFFERING_PLAYING) {
                    mPlayerState = STATE_PLAYING;
                    mVideoController.setControllerState(mCurrentMode, mPlayerState);
                }
                if (mPlayerState == STATE_BUFFERING_PAUSED) {
                    mPlayerState = STATE_PAUSED;
                    mVideoController.setControllerState(mCurrentMode, mPlayerState);
                }
            } else {

            }
            return true;
        }
    };

    private MediaPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener
            = new MediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            mBufferPercentage = percent;
        }
    };


    @Override
    public void start() {

        if (mPlayerState == STATE_IDLE
                || mPlayerState == STATE_ERROR
                || mPlayerState == STATE_COMPLETED) {
            initMediaPlayer();
            initTextureView();
            addTextureView();
        }
    }

    @Override
    public void restart() {
        if (mPlayerState == STATE_PAUSED) {
            mMediaPlayer.start();
            mPlayerState = STATE_PLAYING;
            mVideoController.setControllerState(mCurrentMode, mPlayerState);
        }
        if (mPlayerState == STATE_BUFFERING_PAUSED) {
            mMediaPlayer.start();
            mPlayerState = STATE_BUFFERING_PLAYING;
            mVideoController.setControllerState(mCurrentMode, mPlayerState);
        }
    }

    @Override
    public void pause() {
        if (mPlayerState == STATE_PLAYING) {
            mMediaPlayer.pause();
            mPlayerState = STATE_PAUSED;
            mVideoController.setControllerState(mCurrentMode, mPlayerState);
        }
        if (mPlayerState == STATE_BUFFERING_PLAYING) {
            mMediaPlayer.pause();
            mPlayerState = STATE_BUFFERING_PAUSED;
            mVideoController.setControllerState(mCurrentMode, mPlayerState);
        }
    }

    @Override
    public void seekTo(int pos) {
        if (mMediaPlayer != null) {
            mMediaPlayer.seekTo(pos);
        }
    }

    /**
     *
     * 全屏，将mContainer(内部包含mTextureView和mController)从当前容器中移除，并添加到android.R.content中.
     * activity标签下添加android:configChanges="orientation|keyboardHidden|screenSize"配置
     *
     * activity根布局本质是一个FrameLayout
     */
    @Override
    public void enterFullScreen() {
        if (mCurrentMode == MODE_FULL_SCREEN) return;

        Util.hideActionBar(mContext);
        Util.scanForActivity(mContext)
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        this.removeView(mContainer);
        ViewGroup contentView = (ViewGroup) Util.scanForActivity(mContext)
                .findViewById(android.R.id.content);
        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        contentView.addView(mContainer, params);

        mCurrentMode = MODE_FULL_SCREEN;
        mVideoController.setControllerState(mCurrentMode, mPlayerState);
    }

    /**
     * 退出全屏，移除mTextureView和mController，并添加到非全屏的容器中。
     * @return true退出全屏.
     */
    @Override
    public boolean exitFullScreen() {
        if (mCurrentMode == MODE_FULL_SCREEN) {
            Util.showActionBar(mContext);
            Util.scanForActivity(mContext)
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            ViewGroup contentView = (ViewGroup) Util.scanForActivity(mContext)
                    .findViewById(android.R.id.content);
            contentView.removeView(mContainer);
            LayoutParams params = new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            this.addView(mContainer, params);

            mCurrentMode = MODE_NORMAL;
            mVideoController.setControllerState(mCurrentMode, mPlayerState);
            return true;
        }
        return false;
    }


    /**
     * 进入小窗口播放，小窗口播放的实现原理与全屏播放类似。
     */
    @Override
    public void enterTinyWindow() {
        if (mCurrentMode == MODE_TINY_WINDOW) return;
        this.removeView(mContainer);

        ViewGroup contentView = (ViewGroup) Util.scanForActivity(mContext)
                .findViewById(android.R.id.content);
        // 小窗口的宽度为屏幕宽度的60%，长宽比默认为16:9，右边距、下边距为8dp。
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                (int) (Util.getScreenWidth(mContext) * 0.6f),
                (int) (Util.getScreenWidth(mContext) * 0.6f * 9f / 16f));
        params.gravity = Gravity.BOTTOM | Gravity.END;
        params.rightMargin = Util.dp2px(mContext, 8f);
        params.bottomMargin = Util.dp2px(mContext, 8f);

        contentView.addView(mContainer, params);

        mCurrentMode = MODE_TINY_WINDOW;
        mVideoController.setControllerState(mCurrentMode, mPlayerState);
    }

    /**
     * 退出小窗口播放
     */
    @Override
    public boolean exitTinyWindow() {
        if (mCurrentMode == MODE_TINY_WINDOW) {
            ViewGroup contentView = (ViewGroup) Util.scanForActivity(mContext)
                    .findViewById(android.R.id.content);
            contentView.removeView(mContainer);
            LayoutParams params = new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            this.addView(mContainer, params);

            mCurrentMode = MODE_NORMAL;
            mVideoController.setControllerState(mCurrentMode, mPlayerState);
            return true;
        }
        return false;
    }

    /**
     * 获取视频第一帧画面
     *
     * @return
     */
    public Bitmap getFirstFrame(){
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {

            //根据网络视频的url获取第一帧
            if (Build.VERSION.SDK_INT >= 14) {
                retriever.setDataSource(vedioUrl, new HashMap<String, String>());
            } else {
                retriever.setDataSource(vedioUrl);
            }
            //获得第一帧图片
            bitmap = retriever.getFrameAtTime(1, MediaMetadataRetriever.OPTION_CLOSEST);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } finally {
            retriever.release();
        }
        return bitmap;

    }

    @Override
    public boolean isIdle() {
        return mPlayerState == STATE_IDLE;
    }

    @Override
    public boolean isPreparing() {
        return mPlayerState == STATE_PREPARING;
    }

    @Override
    public boolean isPrepared() {
        return mPlayerState == STATE_PREPARED;
    }

    @Override
    public boolean isBufferingPlaying() {
        return mPlayerState == STATE_BUFFERING_PLAYING;
    }

    @Override
    public boolean isBufferingPaused() {
        return mPlayerState == STATE_BUFFERING_PAUSED;
    }

    @Override
    public boolean isPlaying() {
        return mPlayerState == STATE_PLAYING;
    }

    @Override
    public boolean isPaused() {
        return mPlayerState == STATE_PAUSED;
    }

    @Override
    public boolean isError() {
        return mPlayerState == STATE_ERROR;
    }

    @Override
    public boolean isCompleted() {
        return mPlayerState == STATE_COMPLETED;
    }

    @Override
    public boolean isFullScreen() {
        return mCurrentMode == MODE_FULL_SCREEN;
    }

    @Override
    public boolean isNormal() {
        return mCurrentMode == MODE_NORMAL;
    }

    @Override
    public boolean isTinyWindow() {
        return mCurrentMode == MODE_TINY_WINDOW;
    }

    @Override
    public int getDuration() {
        return mMediaPlayer != null ? mMediaPlayer.getDuration() : 0;
    }

    @Override
    public int getCurrentPosition() {
        return mMediaPlayer != null ? mMediaPlayer.getCurrentPosition() : 0;
    }

    @Override
    public int getBufferPercentage() {
        return mBufferPercentage;
    }

    @Override
    public void release() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        mContainer.removeView(mTextureView);

        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }

        if (mVideoController != null) {
            mVideoController.reset();
        }
        mPlayerState = STATE_IDLE;
        mCurrentMode = MODE_NORMAL;
    }

}
