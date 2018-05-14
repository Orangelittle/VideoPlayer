package com.foxconn.peter;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private VideoPlayer videoPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoPlayer = (VideoPlayer) findViewById(R.id.videoView);
        VideoController controller =new VideoController(this);

    
//        videoPlayer.setVideoUrl("http://file.xiaole-sharp.com:8314/group1/M00/00/05/Cr_IdVrz98eAPm2rAORyoGC-Mq4567.mp4");
        videoPlayer.setVideoUrl("http://orewrc0vz.bkt.clouddn.com/testMovie.mp4");
        videoPlayer.setVideoController(controller);


    }

    public  void changeMode(View view){
        if (videoPlayer.isPlaying()
                || videoPlayer.isBufferingPlaying()
                || videoPlayer.isPaused()
                || videoPlayer.isBufferingPaused()) {
            videoPlayer.enterTinyWindow();
        } else {
            Toast.makeText(this, "要播放后才能进入小窗口", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoPlayer != null) {
            videoPlayer.release();
        }
    }
}
