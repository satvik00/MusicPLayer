package com.example.musicplayer;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ActivityManager;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LinearLayout relativeLayout=(LinearLayout) findViewById(R.id.linearLayout);
        AnimationDrawable animationDrawable = (AnimationDrawable) relativeLayout.getBackground();

        animationDrawable.setEnterFadeDuration(1000);
        animationDrawable.setExitFadeDuration(2000);

        animationDrawable.start();
    }

    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private static final int REQUEST_PERMISSIONS=12345;
    private static final int permissions_count=1;
    private boolean arepermissionsdenied(){
        for(int i=0;i<permissions_count;i++){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(checkSelfPermission(PERMISSIONS[i])!= PackageManager.PERMISSION_GRANTED){
                    return true;
                }
            }
        }
        return false;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(arepermissionsdenied()){
            ((ActivityManager)(this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
            recreate();
        }else{
            onResume();
        }
    }
    private boolean isMusicPLayerInit;
    private List<String> musicFilesList;
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void addMusicFilesFrom(String dirPath){
        final File musicDir=new File(dirPath);
        if(!musicDir.exists()){
           musicDir.mkdir();
           return;
        }
        if(musicDir==null){
            return;
        }
        final File[] files=musicDir.listFiles();
        for(File file: Objects.<File[]>requireNonNull(files)){
            final String path=file.getAbsolutePath();
            if(path.endsWith(".mp3")){
                musicFilesList.add(path);
            }
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void fillMusicList(){
        musicFilesList.clear();
        addMusicFilesFrom(String.valueOf(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC)));
        addMusicFilesFrom(String.valueOf(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS)));
    }
    private MediaPlayer mp;
    private int playMusicFile(String path){
        mp =new MediaPlayer();
        try{
            mp.setDataSource(path);
            mp.prepare();
            mp.start();
        }catch(Exception e){
            e.printStackTrace();
        }
        return mp.getDuration();
    }
    private int songPosition;
    private boolean isSongPlaying=false;
    View playbackControls;

    @Override
    protected void onResume(){
        super.onResume();
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M && arepermissionsdenied()){
            requestPermissions(PERMISSIONS,REQUEST_PERMISSIONS);
            return;
        }
        if(!isMusicPLayerInit){
            final ListView listview=findViewById(R.id.listview);
            final TextAdapter textAdapter=new TextAdapter();
            musicFilesList=new ArrayList<>();
            fillMusicList();
            listview.setAdapter(textAdapter);
            if(musicFilesList.isEmpty()){
                textAdapter.setData(Collections.singletonList("please add .mp3 files in Downloads or Music directory"));
                return;
            }else {
                textAdapter.setData(musicFilesList);
            }
            final SeekBar seekBar=findViewById(R.id.seekBar);
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                int songProgress;
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    songProgress=progress;
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    songPosition=songProgress;
                    mp.seekTo(songPosition*1000);
                }
            });
            final TextView songPositionTextView=findViewById(R.id.currentPosition);
            final TextView songDurationTextView=findViewById(R.id.songDuration);
            final Button pauseButton=findViewById(R.id.pauseButton);

            listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(final AdapterView<?> parent, View view, int position, long id) {
                    playbackControls=findViewById(R.id.playbackButtons);
                    final String musicFilePath=musicFilesList.get(position);
                    final int songDuration=playMusicFile(musicFilePath)/1000;
                    seekBar.setMax(songDuration);
                    playbackControls.setVisibility(View.VISIBLE);
                    seekBar.setVisibility(View.VISIBLE);
                    songDurationTextView.setText(String.valueOf(songDuration/3600)+":"+String.valueOf((songDuration%3600)/60)+":"+String.valueOf((songDuration%3600)%60));
                    isSongPlaying=true;
                    new Thread() {
                        public void run(){
                            songPosition = 0;
                            while (songPosition < songDuration) {
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                if (isSongPlaying) {
                                    songPosition++;
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            seekBar.setProgress(songPosition);
                                            songPositionTextView.setText(String.valueOf(songPosition / 3600) + ":" + String.valueOf((songPosition % 3600) / 60) + ":" + String.valueOf((songPosition % 3600) % 60));
                                            pauseButton.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    if(isSongPlaying){
                                                        mp.pause();
                                                        pauseButton.setText("play");
                                                    }else{
                                                        mp.start();
                                                        pauseButton.setText("pause");
                                                    }
                                                    isSongPlaying=!isSongPlaying;
                                                }
                                            });
                                            if(songPosition==songDuration){
                                                mp.pause();
                                                songPosition = 0;
                                                mp.seekTo(songPosition);
                                                songPositionTextView.setText(String.valueOf(songPosition / 3600) + ":" + String.valueOf((songPosition % 3600) / 60) + ":" + String.valueOf((songPosition % 3600) % 60));
                                                pauseButton.setText("play");
                                                isSongPlaying = false;
                                                seekBar.setProgress(songPosition);
                                            }
                                            listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                                @Override
                                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                                    mp.pause();
                                                    String musicFilePath=musicFilesList.get(position);
                                                    int songDuration=playMusicFile(musicFilePath)/1000;
                                                    songPosition=0;
                                                    seekBar.setMax(songDuration);
                                                    songDurationTextView.setText(String.valueOf(songDuration/3600)+":"+String.valueOf((songDuration%3600)/60)+":"+String.valueOf((songDuration%3600)%60));
                                                    songPositionTextView.setText(String.valueOf(songPosition / 3600) + ":" + String.valueOf((songPosition % 3600) / 60) + ":" + String.valueOf((songPosition % 3600) % 60));
                                                }
                                            });
                                        }
                                    });
                                }
                        }}
                    }.start();
                }
            });
            isMusicPLayerInit=true;
        }
    }
}
