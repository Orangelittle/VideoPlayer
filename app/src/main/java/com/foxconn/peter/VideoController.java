package com.foxconn.peter;

import android.content.Context;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by peter on 2018/5/11.
 *
 * 视频播放控制器
 **/
public class VideoController extends FrameLayout implements View.OnClickListener,SeekBar.OnSeekBarChangeListener{

    private static final String TAG = "VideoController";

    private ImageView mImage;
    private ImageView mCenterStart;
    private RelativeLayout mTop;
    private ImageView mBack;
    private TextView mTitle;
    private LinearLayout mBottom;
    private ImageView mRestartPause;
    private TextView mPosition;
    private TextView mDuration;
    private SeekBar mSeek;
    private ImageView mFullScreen;
    private LinearLayout mLoading;
    private TextView mLoadText;
    private LinearLayout mError;
    private TextView mRetry;
    private LinearLayout mCompleted;
    private TextView mReplay;

    private Timer mUpdateProgressTimer;
    private TimerTask mUpdateProgressTimerTask;
    private boolean topBottomVisible;
    private CountDownTimer mDismissTopBottomCountDownTimer;


    private Context mContext;
    private VideoPlayer mVideoPlayer;

    private int mCurrentMode; //记录当前窗口模式 用于显示顶部标题栏


    public VideoController(@NonNull Context context) {
        super(context);
        this.mContext = context;
        init();
    }

    private void init(){
       LayoutInflater.from(mContext).inflate(R.layout.controller, this, true);
        mCenterStart = (ImageView) findViewById(R.id.center_start);
        mImage = (ImageView) findViewById(R.id.image);

        mTop = (RelativeLayout) findViewById(R.id.top);
        mBack = (ImageView) findViewById(R.id.back);
        mTitle = (TextView) findViewById(R.id.title);

        mBottom = (LinearLayout) findViewById(R.id.bottom);
        mRestartPause = (ImageView) findViewById(R.id.restart_or_pause);
        mPosition = (TextView) findViewById(R.id.position);
        mDuration = (TextView) findViewById(R.id.duration);
        mSeek = (SeekBar) findViewById(R.id.seek);
        mFullScreen = (ImageView) findViewById(R.id.full_screen);

        mLoading = (LinearLayout) findViewById(R.id.loading);
        mLoadText = (TextView) findViewById(R.id.load_text);

        mError = (LinearLayout) findViewById(R.id.error);
        mRetry = (TextView) findViewById(R.id.retry);

        mCompleted = (LinearLayout) findViewById(R.id.completed);
        mReplay = (TextView) findViewById(R.id.replay);

        mCenterStart.setOnClickListener(this);
        mBack.setOnClickListener(this);
        mRestartPause.setOnClickListener(this);
        mFullScreen.setOnClickListener(this);
        mRetry.setOnClickListener(this);
        mReplay.setOnClickListener(this);
        mSeek.setOnSeekBarChangeListener(this);
        this.setOnClickListener(this);
    }

    public  void setTitle(String tile){
        mTitle.setText(tile);
    }


    /**
     * 根据状态值设置UI逻辑显示
     * @param playMode
     * @param playStatus
     */
    public void setControllerState(int playMode, int playStatus){
        Log.d(TAG, "setControllerState: playMode= "+playMode+"  playStatus="  +playStatus);
        mCurrentMode = playMode;
        switch (playMode) {
            case VideoPlayer.MODE_NORMAL:
                mTop.setVisibility(GONE);
                mFullScreen.setVisibility(VISIBLE);
                mFullScreen.setImageResource(R.mipmap.ic_player_enlarge);
                break;
            case VideoPlayer.MODE_FULL_SCREEN:
                mTop.setVisibility(VISIBLE);
                mFullScreen.setVisibility(VISIBLE);
                mFullScreen.setImageResource(R.mipmap.ic_player_shrink);
                break;
        }

        switch (playStatus) {
            case VideoPlayer.STATE_IDLE:

                break;
            case VideoPlayer.STATE_PREPARING:
                // 只显示准备中
                mLoading.setVisibility(View.VISIBLE);
                mLoadText.setText("正在准备...");
                mError.setVisibility(View.GONE);
                mCompleted.setVisibility(View.GONE);
                mTop.setVisibility(View.GONE);
                mCenterStart.setVisibility(View.GONE);
                break;
            case VideoPlayer.STATE_PREPARED:
                startUpdateProgressTimer();
                break;

            case VideoPlayer.STATE_PLAYING:
                mLoading.setVisibility(View.GONE);
                mImage.setVisibility(View.GONE);
                mRestartPause.setImageResource(R.mipmap.ic_player_pause);
                startDismissTopBottomTimer();
                break;

            case VideoPlayer.STATE_PAUSED:
                mLoading.setVisibility(View.GONE);
                mRestartPause.setImageResource(R.mipmap.ic_player_start);
                cancelDismissTopBottomTimer();
                break;

            case VideoPlayer.STATE_BUFFERING_PLAYING:
                mLoading.setVisibility(View.VISIBLE);
                mRestartPause.setImageResource(R.mipmap.ic_player_pause);
                mLoadText.setText("正在缓冲...");
                startDismissTopBottomTimer();
                break;

            case VideoPlayer.STATE_BUFFERING_PAUSED:
                mLoading.setVisibility(View.VISIBLE);
                mRestartPause.setImageResource(R.mipmap.ic_player_start);
                mLoadText.setText("正在缓冲...");
                cancelDismissTopBottomTimer();
                break;

            case VideoPlayer.STATE_COMPLETED:
                cancelUpdateProgressTimer();
                setTopBottomVisible(false);
                mImage.setVisibility(View.VISIBLE);
                mCompleted.setVisibility(View.VISIBLE);
                if (mVideoPlayer.isFullScreen()) {
                    mVideoPlayer.exitFullScreen();
                }

                if (mVideoPlayer.isTinyWindow()) {
                    mVideoPlayer.exitTinyWindow();
                }
                break;

            case VideoPlayer.STATE_ERROR:
                cancelUpdateProgressTimer();
                setTopBottomVisible(false);
                mTop.setVisibility(View.VISIBLE);
                mError.setVisibility(View.VISIBLE);
                break;
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                if (mVideoPlayer.isFullScreen()) {
                    mVideoPlayer.exitFullScreen();
                }else if (mVideoPlayer.isTinyWindow()) {
                    mVideoPlayer.exitTinyWindow();
                }
                break;

            case R.id.center_start:
                if (mVideoPlayer.isIdle())
                mVideoPlayer.start();
                break;

            case R.id.restart_or_pause:
                if (mVideoPlayer.isPlaying() || mVideoPlayer.isBufferingPlaying()) {
                    mVideoPlayer.pause();
                } else if (mVideoPlayer.isPaused() || mVideoPlayer.isBufferingPaused()) {
                    mVideoPlayer.restart();
                }
                break;

            case R.id.full_screen:
                if (mVideoPlayer.isNormal() || mVideoPlayer.isTinyWindow()) {
                    mVideoPlayer.enterFullScreen();
                } else if (mVideoPlayer.isFullScreen()) {
                    mVideoPlayer.exitFullScreen();
                }
                break;

            case R.id.retry:
            case R.id.replay:
                mVideoPlayer.release();
                mVideoPlayer.start();
                break;

            default:
                if (mVideoPlayer.isPlaying()
                        || mVideoPlayer.isPaused()
                        || mVideoPlayer.isBufferingPlaying()
                        || mVideoPlayer.isBufferingPaused()) {
                    setTopBottomVisible(!topBottomVisible);
                }
                break;
        }
    }


    public void setVideoPlayer(VideoPlayer mVideoPlayer){
        this.mVideoPlayer = mVideoPlayer;
        // TODO: 2018/5/14 对外抛接口 获取第一帧图片 此方耗时 
//        mImage.setImageBitmap(mVideoPlayer.getFirstFrame()); //设置视频第一帧画面 
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        cancelDismissTopBottomTimer();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (mVideoPlayer.isBufferingPaused() || mVideoPlayer.isPaused()) {
            mVideoPlayer.restart();
        }
        int position = (int) (mVideoPlayer.getDuration() * seekBar.getProgress() / 100f);
        mVideoPlayer.seekTo(position);
        startDismissTopBottomTimer();
    }



    private void startUpdateProgressTimer() {
        cancelUpdateProgressTimer();
        if (mUpdateProgressTimer == null) {
            mUpdateProgressTimer = new Timer();
        }
        if (mUpdateProgressTimerTask == null) {
            mUpdateProgressTimerTask = new TimerTask() {
                @Override
                public void run() {
                    VideoController.this.post(new Runnable() {
                        @Override
                        public void run() {
                            updateProgress();
                        }
                    });
                }
            };
        }
        mUpdateProgressTimer.schedule(mUpdateProgressTimerTask, 0, 300);
    }

    private void updateProgress() {
        int position = mVideoPlayer.getCurrentPosition();
        int duration = mVideoPlayer.getDuration();
        int bufferPercentage = mVideoPlayer.getBufferPercentage();
        mSeek.setSecondaryProgress(bufferPercentage);
        int progress = (int) (100f * position / duration);
        mSeek.setProgress(progress);
        mPosition.setText(Util.formatTime(position));
        mDuration.setText(Util.formatTime(duration));

//        Log.d("自定义标签", "updateProgress: position= "+position   +"duration=  "+duration);
    }


    private void cancelUpdateProgressTimer() {
        if (mUpdateProgressTimer != null) {
            mUpdateProgressTimer.cancel();
            mUpdateProgressTimer = null;
        }
        if (mUpdateProgressTimerTask != null) {
            mUpdateProgressTimerTask.cancel();
            mUpdateProgressTimerTask = null;
        }
    }

    private void startDismissTopBottomTimer() {
        cancelDismissTopBottomTimer();
        if (mDismissTopBottomCountDownTimer == null) {
            mDismissTopBottomCountDownTimer = new CountDownTimer(8000, 8000) {
                @Override
                public void onTick(long millisUntilFinished) {

                }

                @Override
                public void onFinish() {
                    setTopBottomVisible(false);
                }
            };
        }
        mDismissTopBottomCountDownTimer.start();
    }

    private void setTopBottomVisible(boolean visible) {
        if (mCurrentMode == VideoPlayer.MODE_FULL_SCREEN) {
            mTop.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        mBottom.setVisibility(visible ? View.VISIBLE : View.GONE);
        topBottomVisible = visible;
        if (visible) {
            if (!mVideoPlayer.isPaused() && !mVideoPlayer.isBufferingPaused()) {
                startDismissTopBottomTimer();
            }
        } else {
            cancelDismissTopBottomTimer();
        }
    }


    private void cancelDismissTopBottomTimer() {
        if (mDismissTopBottomCountDownTimer != null) {
            mDismissTopBottomCountDownTimer.cancel();
        }
    }

    /**
     * 控制器恢复到初始状态
     */
    public void reset() {
        topBottomVisible = false;
        cancelUpdateProgressTimer();
        cancelDismissTopBottomTimer();
        mSeek.setProgress(0);
        mSeek.setSecondaryProgress(0);

        mCenterStart.setVisibility(View.VISIBLE);
        mImage.setVisibility(View.VISIBLE);

        mBottom.setVisibility(View.GONE);
        mFullScreen.setImageResource(R.mipmap.ic_player_enlarge);

        mLoading.setVisibility(View.GONE);
        mError.setVisibility(View.GONE);
        mCompleted.setVisibility(View.GONE);
    }
}
