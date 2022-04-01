package com.qy.qmusicplayer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;


import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private int songIndexPlaying = 0;
    int RmSongsList_num = 0;
    private boolean hasSongIN = false;// 队列是否有歌，停止播放后为false
    private boolean hasSonRandom = false;
    private boolean isChanging=false;//互斥变量，防止定时器与SeekBar拖动时进度冲突
    private TextView songCount, playingSongInfo, nowTime;
    private Song msong;
    private ArrayAdapter<Song> mArrayAdapter;
    private List<Song> mSongsList = new ArrayList<>();
    List<Song> RRmSongsList = new ArrayList<>();
    private ListView mlv_musics;
    private Button mBtnPre, mBtnNext, mBtnChangeRandom, mBtnPause;
    private SeekBar mseekBar;
    private MediaPlayer mediaPlayer;

    private Timer mTimer;
    private TimerTask mTimerTask;

    private TextView mlist_view;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getPermission();
        initView();
        initData();
    }

    private void initData() {
        //设置焦点
        playingSongInfo.requestFocus();

        mediaPlayer = new MediaPlayer();
        //绑定adapter
        mArrayAdapter = new ArrayAdapter<Song>(this,
        R.layout.list_view,
                mSongsList);
        mlv_musics.setAdapter(mArrayAdapter);

        readMusics();
        if (mArrayAdapter.getCount() == 0) {
            Toast.makeText(this, "暂无歌曲", Toast.LENGTH_SHORT).show();
        }
        songCount.setText("歌曲列表(" + mArrayAdapter.getCount() + ")");

        Handler handler =new Handler();
        final Runnable r = new Runnable() {
            public void run() {
                handler.postDelayed(this, 10);
                if(mediaPlayer.isPlaying())
                {
                    nowTime.setText(getFormatedDateTime("mm:ss", mediaPlayer.getCurrentPosition()) + "/" + getFormatedDateTime("mm:ss", mediaPlayer.getDuration()));
                }
            }
        };
        handler.postDelayed(r, 10);

        //播放完成回调
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer player) {
                next(true);
            }
        });

        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                return true;
            }
        });

        //seekBar进度
        mseekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isChanging=true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mediaPlayer.seekTo(seekBar.getProgress());
                isChanging=false;
            }
        });
    }

    private void initView() {
        mBtnPre = findViewById(R.id.btn_pre);
        mBtnNext = findViewById(R.id.btn_next);
        mBtnChangeRandom = findViewById(R.id.btn_random);
        mBtnPause = findViewById(R.id.btn_pause);

        mBtnPre.setOnClickListener(this);
        mBtnNext.setOnClickListener(this);
        mBtnChangeRandom.setOnClickListener(this);
        mBtnPause.setOnClickListener(this);

        playingSongInfo = findViewById(R.id.playingSongInfo);

        songCount = findViewById(R.id.songCount);
        mlv_musics = (ListView) findViewById(R.id.lv_musics);
        mlv_musics.setOnItemClickListener(this::onItemClick);

        nowTime = findViewById(R.id.nowTime);
        mseekBar = (SeekBar)this.findViewById(R.id.seekBar);

        mlist_view = findViewById(R.id.list_views);



    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_pre: {
                next(false);
            }
            break;
            case R.id.btn_next: {
                next(true);
            }
            break;
            case R.id.btn_random: {
                if (hasSonRandom) {
                    hasSonRandom = false;
                    mBtnChangeRandom.setText("\uD83D\uDD01");
                } else {
                    hasSonRandom = true;
                    mBtnChangeRandom.setText("\uD83D\uDD00");
                    Collections.shuffle(RRmSongsList);
                    RmSongsList_num = RRmSongsList.indexOf(msong);
                }
            }
            break;
            case R.id.btn_pause: {
                pauseandplay();
            }
            break;
        }
    }

    private void getPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "授权成功！", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "拒绝权限将无法使用程序", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                break;
        }
    }

    private void readMusics() {
        Cursor cursor = null;
        try {
            ContentResolver mCR = getContentResolver();
            cursor = mCR.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    null, null, null, null);
            if (cursor != null) {
                int num=1;
                mSongsList.clear();
                while (cursor.moveToNext()) {
                    //歌名
                    @SuppressLint("Range") String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                    //歌手
                    @SuppressLint("Range") String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                    //歌的时长
                    @SuppressLint("Range") int duration = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                    //大小
                    @SuppressLint("Range") float size = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE));
                    //显示名
                    @SuppressLint("Range") String displayName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
                    //全路径
                    @SuppressLint("Range") String fileUrl = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                    //全路径
                    @SuppressLint("Range") int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
                    //封面
                     //BitmapDrawable image =getImage(id,fileUrl);
//                    Song song = new Song(num,title, artist, duration, size, displayName, fileUrl,image);
                    Song song = new Song(num,title, artist, duration, size, displayName, fileUrl);
                    //淘汰小文件
                    if(song.getDuration()>60000)
                    {
                        mSongsList.add(song);
                        RRmSongsList.add(song);
                        num++;
                    }
                }
                mArrayAdapter.notifyDataSetChanged();
                Toast.makeText(this, "已更新", Toast.LENGTH_SHORT).show();

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        songIndexPlaying = i;//更新当前播放的歌曲索引
        msong = mSongsList.get(songIndexPlaying);
        Play(msong);
    }


    private void Play(Song song) {
        //必须强制清空
        mediaPlayer.reset();
        try {
            mediaPlayer.setDataSource(song.getFileUrl());//绝对路径，参见Song类
            mediaPlayer.prepare();
            mseekBar.setMax(mediaPlayer.getDuration());
            mTimer = new Timer();
            mTimerTask = new TimerTask() {
                @Override
                public void run() {
                    if(mediaPlayer.isPlaying())
                    {
                        if(isChanging) {
                            return;
                        }
                        mseekBar.setProgress(mediaPlayer.getCurrentPosition());
                    }
                    else
                    {

                    }
                }
            };
            mTimer.schedule(mTimerTask, 0, 10);
            mediaPlayer.start();
            mBtnPause.setText("⏸");

            //更新

            playingSongInfo.setText("No." + (songIndexPlaying + 1) + " " + song.getArtist() + " - " +  song.getTitle());
            hasSongIN = true;


        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "未知错误，播放失败！", Toast.LENGTH_SHORT).show();
        }
    }

    //设置时间格式
    public static String getFormatedDateTime(String pattern, long dateTime) {
        SimpleDateFormat sDateFormat = new SimpleDateFormat(pattern);
        return sDateFormat.format(new Date(dateTime));
    }

    public void next(boolean change) {
        //ture下一个，false上一个
        if(change)
        {
            if (mArrayAdapter.getCount() == 0) {
                Toast.makeText(this, "暂无歌曲", Toast.LENGTH_SHORT).show();
            } else {
                if (hasSonRandom) {
                    RmSongsList_num++;
                    if (RmSongsList_num > mArrayAdapter.getCount() - 1) {
                        RmSongsList_num = 0;
                    }
                    msong = RRmSongsList.get(RmSongsList_num);
                    songIndexPlaying = mSongsList.lastIndexOf(msong);
                } else {
                    songIndexPlaying++;//更新当前播放的歌曲索引
                    if (songIndexPlaying > mArrayAdapter.getCount() - 1) {
                        songIndexPlaying = 0;
                    }
                    msong = mSongsList.get(songIndexPlaying);
                }
                Play(msong);
            }
        }
        else
        {
            if (mArrayAdapter.getCount() == 0) {
                Toast.makeText(this, "暂无歌曲", Toast.LENGTH_SHORT).show();
            } else {
                if (hasSonRandom) {
                    RmSongsList_num--;
                    if (RmSongsList_num < 0) {
                        RmSongsList_num = mArrayAdapter.getCount() - 1;
                    }
                    msong = RRmSongsList.get(RmSongsList_num);
                    songIndexPlaying = mSongsList.lastIndexOf(msong);

                } else {
                    songIndexPlaying--;//更新当前播放的歌曲索引
                    if (songIndexPlaying < 0) {
                        songIndexPlaying = mArrayAdapter.getCount() - 1;
                    }
                    msong = mSongsList.get(songIndexPlaying);
                }
                Play(msong);
            }
        }
    }

    public void pauseandplay() {
            if (!hasSongIN) {
                Play(mSongsList.get(songIndexPlaying));
                hasSongIN = true;
            } else {
                if (mediaPlayer.isPlaying()) {
                    mBtnPause.setText("⏯️");
                    mediaPlayer.pause();

                } else {
                    mediaPlayer.start();
                    mBtnPause.setText("⏸");
                }
        }
    }

    private BitmapDrawable getImage(int id,String mUriAlbums)
    {
        int album_id = id;
        String albumArt = getAlbumArt(album_id,mUriAlbums);
        Bitmap bm = null;
        if (albumArt == null)
        {
            bm = BitmapFactory.decodeFile(String.valueOf(R.drawable.cover));

            return new BitmapDrawable(bm);
        }
        else
        {
            bm = BitmapFactory.decodeFile(albumArt);
            BitmapDrawable bmpDraw = new BitmapDrawable(bm);
            return new BitmapDrawable(bm);

        }
    }

    private String getAlbumArt(int album_id,String mUriAlbums)

    {
        String[] projection = new String[] { "album_art" };
        Cursor cur = this.getContentResolver().query(  Uri.parse(mUriAlbums + "/" + Integer.toString(album_id)),  projection, null, null, null);
        String album_art = null;
        if (cur.getCount() > 0 && cur.getColumnCount() > 0)
        {  cur.moveToNext();
            album_art = cur.getString(0);
        }
        cur.close();
        cur = null;
        return album_art;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mArrayAdapter = null;
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }
}



