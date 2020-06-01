package com.imageli;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.transition.Slide;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.imageli.databinding.ActivityExtractTextBinding;
import com.theartofdev.edmodo.cropper.CropImage;

import java.util.List;

public class ExtractTextActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityExtractTextBinding binding;
    private Bitmap myBitmap;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Slide slide = new Slide();
            slide.setSlideEdge(Gravity.RIGHT);
            getWindow().setEnterTransition(slide);
        }
        super.onCreate(savedInstanceState);

        //binding the layout activity_main
        binding = ActivityExtractTextBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setTitle(getString(R.string.extract_text));
        binding.btnTakeImage.setOnClickListener(this);
        binding.btnExtractText.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnTakeImage:
                CropImage.activity().start(this);
                break;
            case R.id.btnExtractText:
                binding.tvShowResult.setText("");
                if (myBitmap != null) {
                    binding.tvResult.setText(getString(R.string.please_wait));
                    runTextRecognition();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                if (result != null) {
                    Uri uri = result.getUri(); //path of image in phone
                    binding.ivImage.setImageURI(uri); //set image in image view
                    myBitmap = getBitmapFromUri(uri);
                }
            }
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) {
        try {
            int rotate = 0;
            String imgPath = FileUtil.getPath(this, uri);

            ExifInterface exif = new ExifInterface(imgPath);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }

            Log.i("==========", "Exif orientation: " + orientation);

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = false;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            opts.inDither = true;

            Bitmap initialBitmap = BitmapFactory.decodeFile(imgPath, opts);
            Matrix matrix = new Matrix();
            matrix.postRotate(rotate);
            return Bitmap.createBitmap(initialBitmap, 0, 0,
                    initialBitmap.getWidth(), initialBitmap.getHeight(), matrix, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void runTextRecognition() {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(myBitmap);
        FirebaseVisionTextRecognizer recognizer = FirebaseVision.getInstance().getCloudTextRecognizer();
        // Disabling 2 buttons
        binding.btnExtractText.setEnabled(false);
        binding.btnTakeImage.setEnabled(false);
        recognizer.processImage(image)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText firebaseVisionText) {

                        // Enabling 2 buttons
                        binding.btnExtractText.setEnabled(true);
                        binding.btnTakeImage.setEnabled(true);
                        processTextRecognitionResult(firebaseVisionText);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Task failed with an exception
                        // Enabling 2 buttons
                        binding.btnExtractText.setEnabled(true);
                        binding.btnTakeImage.setEnabled(true);
                        e.printStackTrace();
                    }
                });
    }

    private void processTextRecognitionResult(FirebaseVisionText texts) {
        binding.tvResult.setText(getString(R.string.extracted_text));

        StringBuilder t = new StringBuilder();

        List<FirebaseVisionText.TextBlock> blocks = texts.getTextBlocks();
        if (blocks.size() == 0) {
            Toast.makeText(this, "No text found", Toast.LENGTH_SHORT).show();
            return;
        }

        for (int i = 0; i < blocks.size(); i++) {
            t.append(" ").append(blocks.get(i).getText());
        }

        binding.tvShowResult.setText(t.toString());

        binding.scrollView.post(new Runnable() {
            public void run() {
                binding.scrollView.smoothScrollTo(0, binding.tvResult.getBottom());
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}