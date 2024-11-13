package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UploadActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_SELECT_IMAGE = 2;
    private static final int REQUEST_CODE_EDIT_IMAGE = 3;

    private static final String TEMP_IMAGE_FILE_NAME = "temp_image.jpg";
    private static final String PREFS_NAME = "UploadPrefs";

    private EditText titleEditText, textEditText, authorIdEditText;
    private ImageView imageView;
    private Bitmap selectedImageBitmap;
    private Button uploadButton, saveButton, loadButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        titleEditText = findViewById(R.id.titleEditText);
        textEditText = findViewById(R.id.textEditText);
        authorIdEditText = findViewById(R.id.authorIdEditText);
        imageView = findViewById(R.id.imageView);
        uploadButton = findViewById(R.id.uploadButton);
        saveButton = findViewById(R.id.saveButton);
        loadButton = findViewById(R.id.loadButton);

        // 이미지 선택
        imageView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
        });

        // 업로드 버튼 클릭 시
        uploadButton.setOnClickListener(v -> {
            String title = titleEditText.getText().toString();
            String text = textEditText.getText().toString();
            String authorId = authorIdEditText.getText().toString();

            if (selectedImageBitmap != null && !title.isEmpty() && !text.isEmpty() && !authorId.isEmpty()) {
                new PutPost().execute(title, text, authorId, selectedImageBitmap);
            } else {
                Toast.makeText(this, "모든 필드를 입력하고 이미지를 선택하세요.", Toast.LENGTH_SHORT).show();
            }
        });

        // 로컬에 임시 저장
        saveButton.setOnClickListener(v -> saveDataLocally());

        // 로컬에서 불러오기
        loadButton.setOnClickListener(v -> loadDataLocally());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK && data != null) {
            // 이미지 선택 시 ImageEditActivity로 전환
            Uri imageUri = data.getData();
            Intent editIntent = new Intent(this, ImageEditActivity.class);
            editIntent.putExtra("imageUri", imageUri);
            startActivityForResult(editIntent, REQUEST_CODE_EDIT_IMAGE);
        } else if (requestCode == REQUEST_CODE_EDIT_IMAGE && resultCode == RESULT_OK && data != null) {
            // ImageEditActivity에서 편집된 이미지를 받아서 ImageView에 표시
            selectedImageBitmap = data.getParcelableExtra("editedImage");
            imageView.setImageBitmap(selectedImageBitmap);
        }
    }

    // 데이터 로컬에 저장
    private void saveDataLocally() {
        String title = titleEditText.getText().toString();
        String text = textEditText.getText().toString();
        String authorId = authorIdEditText.getText().toString();

        // 텍스트 데이터 SharedPreferences에 저장
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putString("title", title)
                .putString("text", text)
                .putString("authorId", authorId)
                .apply();

        // 이미지 내부 저장소에 저장
        if (selectedImageBitmap != null) {
            try (FileOutputStream fos = openFileOutput(TEMP_IMAGE_FILE_NAME, MODE_PRIVATE)) {
                selectedImageBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                Toast.makeText(this, "데이터가 로컬에 저장되었습니다.", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "데이터 저장에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 데이터 로컬에서 불러오기
    private void loadDataLocally() {
        // 텍스트 데이터 불러오기
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        titleEditText.setText(prefs.getString("title", ""));
        textEditText.setText(prefs.getString("text", ""));
        authorIdEditText.setText(prefs.getString("authorId", ""));

        // 이미지 데이터 불러오기
        try (FileInputStream fis = openFileInput(TEMP_IMAGE_FILE_NAME)) {
            selectedImageBitmap = BitmapFactory.decodeStream(fis);
            imageView.setImageBitmap(selectedImageBitmap);
            Toast.makeText(this, "데이터가 로컬에서 불러와졌습니다.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "로컬에 저장된 데이터가 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private class PutPost extends AsyncTask<Object, Void, Void> {
        @Override
        protected Void doInBackground(Object... params) {
            try {
                String title = (String) params[0];
                String text = (String) params[1];
                String authorId = (String) params[2];
                Bitmap bitmap = (Bitmap) params[3];

                String token = "dc2370468c357d774045c432acd5936362161aa7";
                String apiUrl = "https://meongju0o0.pythonanywhere.com/api_root/Post/";

                // 현재 날짜와 시간을 ISO 8601 형식으로 설정
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                String publishedDate = sdf.format(new Date());

                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=boundary");

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes("--boundary\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"title\"\r\n\r\n" + title + "\r\n");

                dos.writeBytes("--boundary\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"text\"\r\n\r\n" + text + "\r\n");

                dos.writeBytes("--boundary\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"author\"\r\n\r\n" + authorId + "\r\n");

                // 자동으로 현재 날짜 설정
                dos.writeBytes("--boundary\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"published_date\"\r\n\r\n" + publishedDate + "\r\n");

                dos.writeBytes("--boundary\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"upload.jpg\"\r\n");
                dos.writeBytes("Content-Type: image/jpeg\r\n\r\n");

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
                byte[] imageData = byteArrayOutputStream.toByteArray();
                dos.write(imageData);

                dos.writeBytes("\r\n");
                dos.writeBytes("--boundary--\r\n");
                dos.flush();
                dos.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d("PutPost", "Post uploaded successfully.");
                } else {
                    Log.d("PutPost", "Post upload failed.");
                }

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Toast.makeText(UploadActivity.this, "Image and post uploaded successfully", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
