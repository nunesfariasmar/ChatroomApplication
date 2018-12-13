package mcc.mcc18.Utils;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import mcc.mcc18.Objects.FriendlyMessage;
import mcc.mcc18.Settings;

import static mcc.mcc18.SingleChatActivity.TAG;

/**
 * Class responsible for uploading user generated files (images) to the cloud server
 */
public class Uploader {
    private Bitmap bitmap = null;
    private ContentResolver resolver;
    private String mUsername;
    private String mPhotoUrl;
    private DatabaseReference mFirebaseDatabaseReference = null;

    public Uploader(ContentResolver contentResolver, String mUsername, String mPhotoUrl, DatabaseReference db){
        this.resolver = contentResolver;
        this.mUsername = mUsername;
        this.mPhotoUrl = mPhotoUrl;
        this.mFirebaseDatabaseReference = db;
    }

    private void updateBitmap(Uri targetUri){
        int width = Settings.imageQuality.first;
        int height = Settings.imageQuality.second;
        try{
            bitmap = BitmapFactory.decodeStream(resolver.openInputStream(targetUri));
            if (width == 0 || (width + height) > (bitmap.getHeight() + bitmap.getWidth())) return;
            bitmap = Bitmap.createScaledBitmap(bitmap,width,height,false);
        } catch (FileNotFoundException e) {
            System.err.println("File not found! " + e.toString());
        }
    }

    public void uploadImage2(StorageReference storage, Bitmap bitmap, final String key){
        int width = Settings.imageQuality.first;
        int height = Settings.imageQuality.second;
        this.bitmap = bitmap;
        if (!(width == 0 || (width + height) > (bitmap.getHeight() + bitmap.getWidth())))
            this.bitmap = Bitmap.createScaledBitmap(bitmap,width,height,false);
        new Labeler(this.bitmap,this).mlImageLabeler(storage,key);
    }

    public void uploadImage(StorageReference storage, Uri uri, final String key){
        //getImageBitmap(uri);
        updateBitmap(uri);
        new Labeler(bitmap,this).mlImageLabeler(storage,key);

    }

    private StorageMetadata getFileMetadata(Bitmap bitmap, ArrayList<String> labels){
        StorageMetadata.Builder builder = new StorageMetadata.Builder();
        builder.setCustomMetadata("width", "" + bitmap.getWidth());
        builder.setCustomMetadata("height", "" + bitmap.getHeight());
        String labelText = MLOptions.defaultLabel;

        for(String label: labels){
            if(MLOptions.availableLabels.contains(label)){
                labelText = (labelText.equals(MLOptions.defaultLabel)) ? label : labelText + "," + label;
            }
        }
        builder.setCustomMetadata("labels",labelText);

        return builder.build();
    }

    public void putBitmapInStorage(StorageReference storage, Bitmap bitmap, final String key, ArrayList<String> labels){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        StorageMetadata meta = getFileMetadata(bitmap, labels);

        storage.putBytes(data, meta).addOnCompleteListener(
                new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()) {
                            FriendlyMessage friendlyMessage =
                                    new FriendlyMessage(null, mUsername, mPhotoUrl,
                                            task.getResult().getDownloadUrl()
                                                    .toString());
                            mFirebaseDatabaseReference.child(key)
                                    .child("imageUrl").setValue(task.getResult().getDownloadUrl()
                                    .toString());
                        } else {
                            Log.w(TAG, "Image upload task was not successful.",
                                    task.getException());
                        }
                    }
                });
    }
}
