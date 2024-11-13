package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private static final int REQUEST_CODE_SELECT_IMAGE = 2;

    TextView textView;
    String site_url = "https://meongju0o0.pythonanywhere.com/api_root/Post/";
    JSONObject post_json;
    String imageUrl = null;

    CloadImage taskDownload;
    PutPost taskUpload;

    RecyclerView recyclerView;
    ImageAdapter imageAdapter;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
        recyclerView = findViewById(R.id.recyclerView); // RecyclerView 초기화

        // RecyclerView에 어댑터 설정
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        imageAdapter = new ImageAdapter(new ArrayList<>());
        recyclerView.setAdapter(imageAdapter);
    }

    public void onClickDownload(View v) {
        if (taskDownload != null && taskDownload.getStatus() == AsyncTask.Status.RUNNING) {
            taskDownload.cancel(true);
        }

        taskDownload = new CloadImage();
        taskDownload.execute(site_url);
        Toast.makeText(getApplicationContext(), "Download", Toast.LENGTH_LONG).show();
    }

    public void onClickUpload(View v) {
        Intent intent = new Intent(this, UploadActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK && data != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(data.getData());
                Bitmap selectedImage = BitmapFactory.decodeStream(inputStream);
                inputStream.close();

                taskUpload = new PutPost();
                taskUpload.execute(selectedImage);
                Toast.makeText(this, "Uploading Image...", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class CloadImage extends AsyncTask<String, Integer, List<Post>> {
        @Override
        protected List<Post> doInBackground(String... urls) {
            List<Post> postList = new ArrayList<>();

            try {
                String token = "dc2370468c357d774045c432acd5936362161aa7";
                URL urlAPI = new URL(urls[0]);
                HttpURLConnection conn = (HttpURLConnection) urlAPI.openConnection();
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    is.close();

                    String strJson = result.toString();
                    JSONArray aryJson = new JSONArray(strJson);

                    for (int i = 0; i < aryJson.length(); i++) {
                        JSONObject postJson = aryJson.getJSONObject(i);
                        String title = postJson.getString("title");
                        String text = postJson.getString("text");
                        String author = postJson.getString("author");
                        String imageUrl = postJson.getString("image");

                        Bitmap imageBitmap = null;
                        if (!imageUrl.isEmpty()) {
                            URL myImageUrl = new URL(imageUrl);
                            conn = (HttpURLConnection) myImageUrl.openConnection();
                            InputStream imgStream = conn.getInputStream();
                            imageBitmap = BitmapFactory.decodeStream(imgStream);
                            imgStream.close();
                        }

                        Post post = new Post(title, text, author, imageBitmap);
                        postList.add(post);
                    }
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            return postList;
        }

        @Override
        protected void onPostExecute(List<Post> postList) {
            if (postList != null && !postList.isEmpty()) {
                imageAdapter = new ImageAdapter(postList);
                recyclerView.setAdapter(imageAdapter);
                Toast.makeText(getApplicationContext(), "Images and posts downloaded", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "다운로드된 이미지가 없습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class PutPost extends AsyncTask<Bitmap, Void, Void> {
        @Override
        protected Void doInBackground(Bitmap... bitmaps) {
            if (bitmaps.length == 0) return null;

            Bitmap bitmap = bitmaps[0];
            String title = "android_test1";
            String text = "android_test1";
            int authorId = 1; // Django에서 'admin' 사용자의 실제 ID(PK)로 변경
            String token = "dc2370468c357d774045c432acd5936362161aa7";

            // 현재 날짜와 시간을 지정된 형식으로 가져오기
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            String currentDateAndTime = sdf.format(new Date());

            try {
                URL url = new URL(site_url);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=boundary");

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

                // 제목 작성
                dos.writeBytes("--boundary\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"title\"\r\n\r\n" + title + "\r\n");

                // 내용 작성
                dos.writeBytes("--boundary\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"text\"\r\n\r\n" + text + "\r\n");

                // 작성자 ID 작성
                dos.writeBytes("--boundary\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"author\"\r\n\r\n" + authorId + "\r\n");

                // 생성 날짜 전송
                dos.writeBytes("--boundary\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"created_date\"\r\n\r\n" + currentDateAndTime + "\r\n");

                // 게시 날짜 전송
                dos.writeBytes("--boundary\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"published_date\"\r\n\r\n" + currentDateAndTime + "\r\n");

                // 이미지 파일 업로드
                dos.writeBytes("--boundary\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"upload.jpg\"\r\n");
                dos.writeBytes("Content-Type: image/jpeg\r\n\r\n");

                // Bitmap 이미지를 JPEG로 압축하여 전송
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
                byte[] imageData = byteArrayOutputStream.toByteArray();
                dos.write(imageData);

                dos.writeBytes("\r\n");
                dos.writeBytes("--boundary--\r\n");
                dos.flush();
                dos.close();

                // 서버 응답 코드 확인
                int responseCode = conn.getResponseCode();
                Log.d("PutPost", "Response Code: " + responseCode);
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d("PutPost", "Post uploaded successfully.");
                } else {
                    Log.d("PutPost", "Post upload failed.");
                    InputStream errorStream = conn.getErrorStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
                    StringBuilder errorMessage = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorMessage.append(line);
                    }
                    reader.close();
                    Log.e("PutPost", "Error Message: " + errorMessage.toString());
                }

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Toast.makeText(getApplicationContext(), "Image and post uploaded successfully", Toast.LENGTH_SHORT).show();
        }
    }
}
