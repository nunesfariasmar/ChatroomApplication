package mcc.mcc18;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class SignIn extends AppCompatActivity implements View.OnClickListener{

    EditText nameEdit;
    EditText mailEdit;
    EditText pswEdit;
    EditText confpswEdit;
    Button register;
    private int GALLERY = 1;
    private ImageView picProfile;
    private String profileImageURl;

    FirebaseAuth firebaseAuth;
    DatabaseReference databaseReference;

    private ProgressBar progressBar;
    private Bitmap bitmap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        profileImageURl="https://previews.123rf.com/images/jazzzzzvector/jazzzzzvector1707/jazzzzzvector170700008/81232760-funny-logo-design-template-with-monkey-in-glasses-and-hat-vector-illustration-.jpg";
        progressBar = (ProgressBar) findViewById(R.id.progressBar_cyclic);

        nameEdit = findViewById(R.id.editName);
        mailEdit = findViewById(R.id.editEmail);
        pswEdit = findViewById(R.id.editPsw);
        confpswEdit = findViewById(R.id.editConfPsw);
        picProfile=(ImageView) findViewById(R.id.imageView);
        Glide.with(this)
                .load(profileImageURl).into(picProfile);

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();


        register = (Button) findViewById(R.id.btnSubmit);
        register.setOnClickListener(this);


    }

    @Override
    public void onClick(View view) {
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(getApplication().INPUT_METHOD_SERVICE);

        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);

        int i = view.getId();
        if (i == R.id.btnSubmit){
            progressBar.setVisibility(View.VISIBLE);
            registerUser();
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri filePath = data.getData();
        FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference();


        // set the image in the current activity
        if(resultCode == RESULT_OK && data != null && data.getData() != null )
        {
            bitmap = null;
            int width = 400; //new constraint since profile pic doesn't need to be that big
            int height = 400;
            try{
                bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(filePath));
                if (!(width == 0 || (width + height) > (bitmap.getHeight() + bitmap.getWidth())))
                    bitmap = Bitmap.createScaledBitmap(bitmap,width,height,false);
                picProfile.setImageBitmap(bitmap);
            } catch (FileNotFoundException e) {
                System.err.println("File not found! " + e.toString());
            }
        }



    }

    private StorageMetadata getFileMetadata(Bitmap bitmap){
        StorageMetadata.Builder builder = new StorageMetadata.Builder();
        builder.setCustomMetadata("width", "" + bitmap.getWidth());
        builder.setCustomMetadata("height", "" + bitmap.getHeight());
        return builder.build();
    }

    public void finishRegister(String name){
        FirebaseUser user = firebaseAuth.getCurrentUser();
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .setPhotoUri(Uri.parse(profileImageURl))
                .build();
        databaseReference.child("users").child(user.getUid()).child("photoUrl").setValue(profileImageURl);
        user.updateProfile(profileUpdates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(getApplicationContext(),"Registration successfull",Toast.LENGTH_SHORT).show();
                            //close thise activity
                            finish();

                            startActivity(new Intent(getApplicationContext(),MainActivity.class));
                        }
                    }
                });
    }

    public void loadOnDB(String email, final String name){

        // store the image in the DB
        if(bitmap != null){
            final StorageReference profileImageReference =
                    FirebaseStorage.getInstance().getReference("profilePict/" + email + "/img:" + System.currentTimeMillis() + ".jpg");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] data = baos.toByteArray();

            profileImageReference.putBytes(data,getFileMetadata(bitmap)).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    profileImageURl=taskSnapshot.getDownloadUrl().toString();
                    finishRegister(name);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    System.out.println("Error uploading photo " + e.toString());
                }
            });
        } else{
            finishRegister(name);
        }
    }

    public void registerUser(){
        final String name = nameEdit.getText().toString();
        final String mail = mailEdit.getText().toString();
        String psw = pswEdit.getText().toString();
        String pswConf = confpswEdit.getText().toString();

        if (validityPsw(psw,pswConf)){
            firebaseAuth.createUserWithEmailAndPassword(mail,psw)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            // Users REGISYERED
                            progressBar.setVisibility(View.INVISIBLE);
                            Toast.makeText(SignIn.this,"register OK",Toast.LENGTH_SHORT).show();

                            // store info in firebase user
                            FirebaseUser user = firebaseAuth.getCurrentUser();

                            databaseReference.child("users").child(user.getUid()).child("name").setValue(name);
                            databaseReference.child("users").child(user.getUid()).child("mail").setValue(mail);
                            databaseReference.child("users").child(user.getUid()).child("id").setValue(user.getUid());

                            final String email = user.getEmail();
                            loadOnDB(email, name);

                            ////STAT CHATTING
                        }else{
                            Toast.makeText(SignIn.this,"Not correctly registered",Toast.LENGTH_LONG).show();
                            progressBar.setVisibility(ProgressBar.INVISIBLE);
                        }
                    }
                });
        }
        else{
            Toast toast = Toast.makeText(getApplicationContext(), "wrong password", Toast.LENGTH_SHORT);
            toast.show();
        }

    }

    public boolean validityPsw(String psw,String confpsw){
        if (psw.equals(confpsw)){
            return true;
        }
        else {
            return false;
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
