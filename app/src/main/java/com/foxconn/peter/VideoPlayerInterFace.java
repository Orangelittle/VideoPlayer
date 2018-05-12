package com.foxconn.peter;

/**
 * Created by peter on 2018/5/11.
 *
 * 返回MediaPlayer的状态值
 **/
public interface VideoPlayerInterFace {
    boolean isIdle();
    boolean isPreparing();
    boolean isPrepared();
    boolean isBufferingPlaying();
    boolean isBufferingPaused();
    boolean isPlaying();
    boolean isPaused();
    boolean isError();
    boolean isCompleted();

    boolean isFullScreen();
    boolean isNormal();
    boolean isTinyWindow();

    void enterFullScreen();
    boolean exitFullScreen();
    void enterTinyWindow();
    boolean exitTinyWindow();

    void start();
    void restart();
    void pause();
    void seekTo(int pos);
    int getDuration();
    int getCurrentPosition();
    int getBufferPercentage();
    void release();

}
