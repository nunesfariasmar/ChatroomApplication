package mcc.mcc18.editProfile;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
import com.google.firebase.database.DatabaseReference;

import mcc.mcc18.R;

public class PopActivity extends Activity {

    private EditText old;
    private EditText nuova;
    private EditText nuovaConf;
    private Button btn;

    FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pop);

        firebaseAuth = FirebaseAuth.getInstance();

        DisplayMetrics dm =new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int w =dm.widthPixels;
        int h =dm.heightPixels;


        getWindow().setLayout((int)(w*0.8),(int)(h*0.7));

        WindowManager.LayoutParams params=  getWindow().getAttributes();
        params.gravity = Gravity.CENTER;
        params.x=0;
        params.y=-20;
        getWindow().setAttributes(params);


        old=(EditText) findViewById(R.id.editOldPsw);
        nuova=(EditText) findViewById(R.id.editPsw);
        nuovaConf=(EditText) findViewById(R.id.editPswConf);


        btn=(Button) findViewById(R.id.btnChangePassword);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {

                String sold=old.getText().toString().trim();
                final String snew=nuova.getText().toString().trim();
                String snewConf=nuovaConf.getText().toString().trim();

                if (verifyPsw(snew,snewConf)){
                    final FirebaseUser user=firebaseAuth.getCurrentUser();
                    AuthCredential credential = EmailAuthProvider
                            .getCredential(user.getEmail(), sold);

                    user.reauthenticate(credential)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        user.updatePassword(snew).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(getApplicationContext(),"Password changed",Toast.LENGTH_LONG).show();
                                                    finish();
                                                } else {
                                                    Toast.makeText(getApplicationContext(),"Password NOT changed",Toast.LENGTH_LONG).show();
                                                }
                                            }
                                        });
                                    } else {
                                        Toast.makeText(getApplicationContext(),"Wrong old passwords",Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                }



            }
        });
    }

    public boolean verifyPsw(String s1,String s2){
        if (s1.equals(s2)){
            return true;
        }else {
            Toast.makeText(getApplicationContext(),"Passwords don't match",Toast.LENGTH_LONG).show();
            return false;
        }
    }
}
