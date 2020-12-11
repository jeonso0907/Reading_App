package com.example.readingapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.google.firebase.ml.vision.text.RecognizedLanguage;

import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    ImageView imageView;
    EditText editText;
    TextView textView;
    String currentPhotoPath;
    private File file;
    private Uri photoUri;
    FirebaseVisionImage firebaseVisionImage;
    FirebaseFirestore db;
    String s;
    String title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        File sdCard = Environment.getExternalStorageDirectory();
        file = new File(sdCard, "capture.jpg");

        imageView = findViewById(R.id.imageId);
        editText = (EditText) findViewById(R.id.editText);
        textView = (TextView) findViewById(R.id.textId);

        db = FirebaseFirestore.getInstance();

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 101);
        }
    }

    // 사진찍기 실행
    public void doProcess(View view) {
        // Low quality image
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }

            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this, getPackageName(), photoFile);

                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(intent, 101);
            }
        }
    }

    // 비트맵 이미지를 파일로 변환
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    // 찍은 사진을 가져오기 및 OCR 실행
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Regular bitmap
//        Bundle bundle = data.getExtras();
//        Bitmap bitmap = (Bitmap) bundle.get("data");
//        imageView.setImageBitmap(bitmap);

        // Original Image
        if (requestCode == 101 && resultCode == Activity.RESULT_OK) {
            imageView.setImageURI(photoUri);
        }


        // 1. Create a FireBaseVisionImage Object from a Bitmap object
        try {
            firebaseVisionImage = FirebaseVisionImage.fromFilePath(this, photoUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 2. Get an instance of FireBaseVision
        FirebaseVision firebaseVision = FirebaseVision.getInstance();
        // 3. Create an instance of FireBaseVisionTextRecognizer
        FirebaseVisionTextRecognizer firebaseVisionTextRecognizer = firebaseVision.getOnDeviceTextRecognizer();
        // 4. Create a task to process the image
        Task<FirebaseVisionText> task = firebaseVisionTextRecognizer.processImage(firebaseVisionImage);
        // 5. If task is success
        task.addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
            @Override
            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                s = firebaseVisionText.getText();
                textView.setText(s);

            }
        });
        // 6. if task is failure
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void save(View view) {
        if (!s.equals(null)) {
            Map<String, String> reading = new HashMap<>();
            reading.put("Text", s);

            title = editText.getText().toString();
            // Add a new document with a generated ID
            db.collection("readings").document(title)
                    .set(reading)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            startToast("Saved!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            startToast("Failed to save....");
                        }
                    });
        } else {
            startToast("Type the title to save");
        }
    }

    @Override
    public void onBackPressed() {
        if (!title.equals(null)) {
            intentData(title);
        }
        finish();
    }

    public void startToast(String toS) {
        Toast.makeText(this, toS, Toast.LENGTH_SHORT).show();
    }

    public void intentData(String title) {
        Intent intent = new Intent(this, ListActivity.class);
        intent.putExtra("Title", title);
        startActivity(intent);
    }
}