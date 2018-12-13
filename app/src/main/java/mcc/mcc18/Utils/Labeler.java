package mcc.mcc18.Utils;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.cloud.FirebaseVisionCloudDetectorOptions;
import com.google.firebase.ml.vision.cloud.label.FirebaseVisionCloudLabel;
import com.google.firebase.ml.vision.cloud.label.FirebaseVisionCloudLabelDetector;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class Labeler {
    private FirebaseVisionCloudDetectorOptions options = null;
    private Bitmap bitmap;
    private Uploader uploader;

    public Labeler(Bitmap bitmap, Uploader uploader){
        this.bitmap = bitmap;
        this.uploader = uploader;
        updateOptionsLatest();
    }

    private void updateOptionsLatest(){
        options = new FirebaseVisionCloudDetectorOptions.Builder()
                        .setModelType(FirebaseVisionCloudDetectorOptions.LATEST_MODEL)
                        .setMaxResults(15)
                        .build();
    }

    public void mlImageLabeler(final StorageReference storage, final String key){
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionCloudLabelDetector detector = FirebaseVision.getInstance()
                .getVisionCloudLabelDetector(options);

        Task<List<FirebaseVisionCloudLabel>> result =
                detector.detectInImage(image)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<FirebaseVisionCloudLabel>>() {
                                    @Override
                                    public void onSuccess(List<FirebaseVisionCloudLabel> labels) {
                                        ArrayList<String> labelsText = new ArrayList<>();
                                        for (FirebaseVisionCloudLabel label: labels) {
                                            labelsText.add(label.getLabel());
                                        }
                                        uploader.putBitmapInStorage(storage,bitmap,key,labelsText);
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        System.out.println("Failed to label the image\n" + e.toString());
                                    }
                                });
    }
}
