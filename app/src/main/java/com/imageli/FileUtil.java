package com.imageli;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;

public class FileUtil {

    public static String getPath(Context context, Uri uri) throws URISyntaxException {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { "_data" };
            Cursor cursor = null;

            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Eat it
            }
        }
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    // Copy Image from one Folder to another
    public static void copyFile(File sourceLocation, File targetLocation)
            throws FileNotFoundException, IOException {

        String destDirPath = targetLocation.getParent();
        File destDir = new File(destDirPath);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        InputStream in = new FileInputStream(sourceLocation);
        OutputStream out = new FileOutputStream(targetLocation);

        // Copy the bits from instream to outstream
        byte[] buf = new byte[1024 * 512];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
            System.out.println(">");
        }
        System.out.println(".");
        in.close();
        out.close();
    }

}