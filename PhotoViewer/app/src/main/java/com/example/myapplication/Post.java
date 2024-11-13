package com.example.myapplication;

import android.graphics.Bitmap;

public class Post {
    private final String title;
    private final String text;
    private final String author;
    private final Bitmap imageBitmap;

    public Post(String title, String text, String author, Bitmap imageBitmap) {
        this.title = title;
        this.text = text;
        this.author = author;
        this.imageBitmap = imageBitmap;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public String getAuthor() {
        return author;
    }

    public Bitmap getImageBitmap() {
        return imageBitmap;
    }
}
