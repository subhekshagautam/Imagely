package com.imageli;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import np.com.vikashparajuli.imagetotext.R;
import np.com.vikashparajuli.imagetotext.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final int MULTIPLE_PERMISSIONS = 10;
    String[] permissions = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA};
    private ActivityMainBinding binding;
    private Bitmap myBitmap;
    private String mCameraFileName = "/sdcard/atmc_project.jpg";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //binding the layout activity_main
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnTakeImage.setOnClickListener(this);
        binding.btnShowText.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnTakeImage:
                if (checkPermissions())
                    showImageChooserAlertDialog();
                break;
            case R.id.btnShowText:
                binding.etResult.setText("");
                if (myBitmap != null) {
                    binding.tvResult.setText(getString(R.string.please_wait));
                    runTextRecognition();
                }
                break;
        }
    }

    private boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    private void showImageChooserAlertDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set dialog message
        alertDialogBuilder
                .setMessage("Choose picture method")
                .setCancelable(true)
                .setPositiveButton("Camera",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                                StrictMode.setVmPolicy(builder.build());
                                Intent intent = new Intent();
                                intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);

                                File outFile = new File(mCameraFileName);

                                Uri outUri = Uri.fromFile(outFile);
                                intent.putExtra(MediaStore.EXTRA_OUTPUT, outUri);
                                startActivityForResult(intent, 102);
                            }
                        })
                .setNegativeButton("Gallery",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                startActivityForResult(pickPhoto, 101);
                            }
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        switch (requestCode) {
            case 101: //From Gallery
                if (resultCode == RESULT_OK && intent != null && intent.getData() != null) {
                    try {
                        Uri selectedImageUri = intent.getData();
                        myBitmap = getBitmapFromUri(selectedImageUri);
                        showImage(myBitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error:1 while getting image from Gallery", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Error:2 while getting image from Gallery", Toast.LENGTH_SHORT).show();
                }
                break;

            case 102://From Camera
                try {
                    Uri uri = Uri.fromFile(new File(mCameraFileName));
                    myBitmap = getBitmapFromUri(uri);
                    showImage(myBitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
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

    private void showImage(Bitmap bitmap) {
        if (myBitmap != null)
            binding.ivImage.setImageBitmap(bitmap);
        else
            Toast.makeText(this, "Error:3 while showing image on ImageView", Toast.LENGTH_SHORT).show();
    }

    private void runTextRecognition() {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(myBitmap);
        FirebaseVisionTextRecognizer recognizer = FirebaseVision.getInstance().getCloudTextRecognizer();
        // Disabling 2 buttons
        binding.btnShowText.setEnabled(false);
        binding.btnTakeImage.setEnabled(false);
        recognizer.processImage(image)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText firebaseVisionText) {

                        // Enabling 2 buttons
                        binding.btnShowText.setEnabled(true);
                        binding.btnTakeImage.setEnabled(true);
                        processTextRecognitionResult(firebaseVisionText);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Task failed with an exception
                        // Enabling 2 buttons
                        binding.btnShowText.setEnabled(true);
                        binding.btnTakeImage.setEnabled(true);
                        e.printStackTrace();
                    }
                });
    }

    private void processTextRecognitionResult(FirebaseVisionText texts) {
        binding.tvResult.setText(getString(R.string.detected_text));

        StringBuilder t = new StringBuilder();

        List<FirebaseVisionText.TextBlock> blocks = texts.getTextBlocks();
        if (blocks.size() == 0) {
            Toast.makeText(this, "No text found", Toast.LENGTH_SHORT).show();
            return;
        }

        for (int i = 0; i < blocks.size(); i++) {
            t.append(" ").append(blocks.get(i).getText());
        }

        binding.etResult.setText(t.toString());

        binding.scrollView.post(new Runnable() {
            public void run() {
                binding.scrollView.smoothScrollTo(0, binding.tvResult.getBottom());
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissionsList[], int[] grantResults) {
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS: {
                if (grantResults.length > 0) {
                    String permissionsDenied = "";
                    for (String per : permissionsList) {
                        if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                            permissionsDenied += "\n" + per;

                        }

                    }
                    // Show permissionsDenied
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                if (checkPermissions())
                    startActivity(new Intent(this, ClassificationActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}