package com.qy.qmusicplayer;




import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import androidx.annotation.NonNull;



public class Song {
//    private int  num;
//    private String year;
//    private String title;
//    private int duration;
//    private String artist;
//    private float size;
//    private String fileUrl;
//    private String displayName;
//    private BitmapDrawable image;

    private int  num;
    private String year;
    private String title;
    private int duration;
    private String artist;
    private float size;
    private String fileUrl;
    private String displayName;



    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getnum() {
        return num;
    }

    public String getArtist() {
        return artist;
    }

    public float getSize() {
        return size;
    }

    public String getSizeStr() {

        return String.format("%.2f", size / 1024 / 1024) + "MB";
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setSize(int size) {
        this.size = size;
    }


    //播放音乐要用
    public String getFileUrl() {
        return fileUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getDuration() {
        return duration;
    }

    public String getDurationStr() {
        int d = duration / 1000;//总秒数
        String minute = "0" + (d / 60);
        String second = d % 60 < 10 ? "0" + (d % 60) : d % 60 + "";
        return minute + ":" + second;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }


    public Song() {
        super();
    }

//    public Song(int num,String title, String artist, int duration, float size, String displayName, String fileUrl ,BitmapDrawable image) {
//        super();
//        this.num = num;
//        this.displayName = displayName;
//        this.title = title;
//        this.duration = duration;
//        this.artist = artist;
//        this.size = size;
//        this.fileUrl = fileUrl;
//        this.image = image;
//
//    }

    public Song(int num,String title, String artist, int duration, float size, String displayName, String fileUrl ) {
        super();
        this.num = num;
        this.displayName = displayName;
        this.title = title;
        this.duration = duration;
        this.artist = artist;
        this.size = size;
        this.fileUrl = fileUrl;

    }

    @NonNull
    @Override

    public String toString() {

//        String s = "歌曲:" + getTitle() +
//                "\n歌手:" + getArtist() +
//                "\n路径:" + getFileUrl()+
//                "\n大小:" + getSizeStr() +
//                "\n时长: " +getDurationStr();
//
        String s =
                "\nNo."+getnum()+"\n"+getArtist() + " - " +  getTitle() +"\n" + getSizeStr() + "  " + getDurationStr() + "\n" + getFileUrl();
        return s;
    }

}
