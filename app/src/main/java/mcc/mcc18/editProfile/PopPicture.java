package mcc.mcc18.editProfile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import mcc.mcc18.R;

public class PopPicture extends Activity {


    private Button btnGallery;
    private Button btnCamera;
    public static String PUBLIC_STATIC_STRING_IDENTIFIER="decision";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pop_picture);


        DisplayMetrics dm =new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int w =dm.widthPixels;
        int h =dm.heightPixels;


        getWindow().setLayout((int)(w*0.6),(int)(h*0.45));

        WindowManager.LayoutParams params=  getWindow().getAttributes();
        params.gravity = Gravity.CENTER;
        params.x=0;
        params.y=-20;
        getWindow().setAttributes(params);





        btnGallery=(Button) findViewById(R.id.btnGallery);
        btnGallery.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v){
                Intent resultIntent = new Intent();
                resultIntent.putExtra(PUBLIC_STATIC_STRING_IDENTIFIER, "gallery");
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
                System.out.println("in");
            }
        });

        btnCamera=(Button) findViewById(R.id.btnCamera);
        btnCamera.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v){
                Intent resultIntent = new Intent();
                resultIntent.putExtra(PUBLIC_STATIC_STRING_IDENTIFIER, "camera");
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
                System.out.println("in");
            }
        });
    }


}
