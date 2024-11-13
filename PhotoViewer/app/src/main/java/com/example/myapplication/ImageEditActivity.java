package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;

public class ImageEditActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_CROP_IMAGE = 1;
    private ImageView imageView;
    private Bitmap selectedImageBitmap;
    private Button rotateButton, cropButton, saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_edit);

        imageView = findViewById(R.id.imageView);
        rotateButton = findViewById(R.id.rotateButton);
        cropButton = findViewById(R.id.cropButton);
        saveButton = findViewById(R.id.saveButton);

        // 선택한 이미지를 받아와서 ImageView에 표시
        Uri imageUri = getIntent().getParcelableExtra("imageUri");
        try {
            selectedImageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            imageView.setImageBitmap(selectedImageBitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 회전 버튼 기능
        rotateButton.setOnClickListener(v -> rotateImage());

        // 자르기 버튼 기능
        cropButton.setOnClickListener(v -> cropImage(imageUri));

        // 저장 버튼 기능
        saveButton.setOnClickListener(v -> saveImage());
    }

    private void rotateImage() {
        Matrix matrix = new Matrix();
        matrix.postRotate(90);  // 90도 회전
        selectedImageBitmap = Bitmap.createBitmap(selectedImageBitmap, 0, 0,
                selectedImageBitmap.getWidth(), selectedImageBitmap.getHeight(), matrix, true);
        imageView.setImageBitmap(selectedImageBitmap);
    }

    private void cropImage(Uri imageUri) {
        // 이미지 자르기 기능
        Intent cropIntent = new Intent("com.android.camera.action.CROP");
        cropIntent.setDataAndType(imageUri, "image/*");
        cropIntent.putExtra("crop", "true");
        cropIntent.putExtra("aspectX", 1);
        cropIntent.putExtra("aspectY", 1);
        cropIntent.putExtra("return-data", true);
        startActivityForResult(cropIntent, REQUEST_CODE_CROP_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CROP_IMAGE && resultCode == RESULT_OK && data != null) {
            Bundle extras = data.getExtras();
            if (extras != null) {
                selectedImageBitmap = extras.getParcelable("data");
                imageView.setImageBitmap(selectedImageBitmap);
            }
        }
    }

    private void saveImage() {
        // MainActivity로 편집한 이미지를 전달하고 종료
        Intent resultIntent = new Intent();
        resultIntent.putExtra("editedImage", selectedImageBitmap);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
