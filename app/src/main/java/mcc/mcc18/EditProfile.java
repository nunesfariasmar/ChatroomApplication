package mcc.mcc18;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.UUID;

import mcc.mcc18.Utils.MLOptions;
import mcc.mcc18.Utils.PicResolution;
import mcc.mcc18.editProfile.*;


public class EditProfile extends AppCompatActivity implements View.OnClickListener {

    private EditText editName;
    private EditText editMail;
    private Button btnSubmit;
    private Button btnChangePsw;
    private int GALLERY = 1;
    private ImageView picProfile;
    FirebaseAuth firebaseAuth;
    private DatabaseReference mFirebaseDatabaseReference;
    private String profileImageURl;
    private Spinner spinner;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        editMail=(EditText) findViewById(R.id.editEmail);
        editName=(EditText) findViewById(R.id.editName);
        btnSubmit=(Button) findViewById(R.id.btnSubmit);
        btnChangePsw=(Button) findViewById(R.id.changePassword);
        picProfile=(ImageView) findViewById(R.id.imgPicProfile);
        mProgressBar = findViewById(R.id.progressBar);
        mProgressBar.setVisibility(ProgressBar.VISIBLE);
        spinner = findViewById(R.id.quality_dropdown);
        spinner.setSelection(mcc.mcc18.Settings.getCorrespondentQuality());

        firebaseAuth = FirebaseAuth.getInstance();
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();

        btnSubmit.setOnClickListener(this);
        btnChangePsw.setOnClickListener(this);

        retriveInfo();
    }

    public void retriveInfo(){
        FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
        editMail.setText(user.getEmail());
        editName.setText(user.getDisplayName());


        Glide.with(this)
                .load(user.getPhotoUrl().toString()).into(picProfile);
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

    }

    public void updateData(){
        mProgressBar.setVisibility(ProgressBar.VISIBLE);
        String name=editName.getText().toString().trim();
        String mail=editMail.getText().toString().trim();

        FirebaseUser user = firebaseAuth.getCurrentUser();
        user.updateEmail(mail);
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            finish();
                            Toast.makeText(getApplicationContext(),"Update successfull",Toast.LENGTH_SHORT).show();
                            ////STAT CHATTING
                            startActivity(new Intent(getApplicationContext(),MainActivity.class));
                        }
                    }
                });
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.btnSubmit){
            // change data in database
            mcc.mcc18.Settings.updateImageQuality(spinner.getSelectedItem().toString(), this);
            updateData();
        }
        if (i == R.id.changePassword){
            Intent intent = new Intent(getApplicationContext(),PopActivity.class);
            startActivity(intent);
        }
    }

    private String getFileName(Uri imageUri) {
        String fileName = null;
        if (imageUri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(imageUri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (fileName == null) {
            fileName = imageUri.getPath();
            int cut = fileName.lastIndexOf('/');
            if (cut != -1) {
                fileName = fileName.substring(cut + 1);
            }
        }

        String[] tmp = fileName.split("\\.");
        return "img:" + System.currentTimeMillis() + "." + tmp[tmp.length-1];
    }

    private void deleteOldFile(String url){
        if(url.split(mcc.mcc18.Settings.imgRefPath).length < 2) return;
        String tmp = url.split(mcc.mcc18.Settings.imgRefPath)[1].split("\\?alt=media&")[0];
        try {
            String imgUrl = URLDecoder.decode(tmp, "UTF-8");
//            System.out.println(imgUrl);
//            String[] tmp2 = imgUrl.split("/");
//            String tmp3 = tmp2[tmp2.length-1];
//
//            String highUrl = imgUrl.replace(tmp3,"high@_" + tmp3);
//            String lowUrl = imgUrl.replace(tmp3,"low@_" + tmp3);
            StorageReference profileImageReference = FirebaseStorage.getInstance().getReference();

            profileImageReference.child(imgUrl).delete();
//            profileImageReference.child(highUrl).delete();
//            profileImageReference.child(lowUrl).delete();
        } catch (UnsupportedEncodingException e) {
            System.err.println("Lol " + e.toString());
        }
    }

    private StorageMetadata getFileMetadata(Bitmap bitmap){
        StorageMetadata.Builder builder = new StorageMetadata.Builder();
        builder.setCustomMetadata("width", "" + bitmap.getWidth());
        builder.setCustomMetadata("height", "" + bitmap.getHeight());
        return builder.build();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri filePath = data.getData();
        FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
        FirebaseStorage  storage = FirebaseStorage.getInstance();
        StorageReference  storageReference = storage.getReference();
        mProgressBar.setVisibility(ProgressBar.VISIBLE);
        Bitmap bitmap = null;

        // set the image in the current activity
        if(resultCode == RESULT_OK && data != null && data.getData() != null )
        {
            int width = 400; //new constraint since profile pic doesn't need to be that big
            int height = 400;
            try{
                bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(filePath));
                if (!(width == 0 || (width + height) > (bitmap.getHeight() + bitmap.getWidth())))
                    bitmap = Bitmap.createScaledBitmap(bitmap,width,height,false);
            } catch (FileNotFoundException e) {
                System.err.println("File not found! " + e.toString());
            }
            picProfile.setImageBitmap(bitmap);
        }

        // store the image in the DB
        if(filePath != null){
//            deleteOldFile(user.getPhotoUrl().toString());
            final StorageReference profileImageReference =
                    FirebaseStorage.getInstance().getReference("profilePict/"+ user.getEmail() + "/" + getFileName(filePath));

            profileImageReference.putFile(filePath,getFileMetadata(bitmap)).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    profileImageURl=taskSnapshot.getDownloadUrl().toString();
                    upadteProfileImg();
                }
            });
        }
    }

    public void upadteProfileImg(){
        FirebaseUser user=firebaseAuth.getCurrentUser();

        if (user != null && profileImageURl != null){
            /// save url on firebaseauth

            UserProfileChangeRequest profile =new UserProfileChangeRequest.Builder()
                    .setPhotoUri(Uri.parse(profileImageURl)).build();

            user.updateProfile(profile).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()){
                        Toast.makeText(EditProfile.this, "Image correctly updated", Toast.LENGTH_SHORT).show();

                        mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                    }
                }
            });
            mFirebaseDatabaseReference.child("users").child(user.getUid()).child("photoUrl").setValue(profileImageURl);

        }
    }


    // change image profile
    public void imageClick(View view) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, GALLERY);
    }
}
