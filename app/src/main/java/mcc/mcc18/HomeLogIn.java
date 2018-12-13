package mcc.mcc18;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import android.widget.ProgressBar;

public class HomeLogIn extends AppCompatActivity implements View.OnClickListener{

    private EditText editTextEmail;
    private EditText editTextPsw;
    private FirebaseAuth firebaseAuth;
    public static boolean startedApp = false;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        readLocalSettings();

        progressBar = (ProgressBar) findViewById(R.id.progressBar_cyclic);

        //button for register a new user
        Button btnSingIn;
        //button log in
        Button btnLogIn;

        firebaseAuth = FirebaseAuth.getInstance();


        //user already logged in
        if(firebaseAuth.getCurrentUser() != null ){
            //close thise activity
            finish();
            //STAT chatting
            startActivity(new Intent(getApplicationContext(),MainActivity.class));
        }

        editTextEmail = (EditText) findViewById(R.id.editTextEmail);
        editTextPsw = (EditText) findViewById(R.id.editTextPsw);


        btnSingIn = (Button) findViewById(R.id.btnSignIn);
        btnLogIn = (Button) findViewById(R.id.btnLogIn);

        btnSingIn.setOnClickListener(this);
        btnLogIn.setOnClickListener(this);


    }

    private void readLocalSettings(){
        if(startedApp)return;
        startedApp = true;
        Settings.readConfigFile(this);
    }

    private void userLogin(){
        String email = editTextEmail.getText().toString().trim();
        String psw = editTextPsw.getText().toString().trim();



        // check field not empty
        if (TextUtils.isEmpty(email)){
            return;
        }
        if (TextUtils.isEmpty(psw)){
            return;
        }

        firebaseAuth.signInWithEmailAndPassword(email,psw)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            //close thise activity
                            finish();
                            progressBar.setVisibility(View.INVISIBLE);
                            ////STAT Chatting
                            startActivity(new Intent(getApplicationContext(),MainActivity.class));
                        }
                        else {
                            progressBar.setVisibility(View.INVISIBLE);
                            Toast.makeText(getApplicationContext(),"wrong email or password",Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                });
    }

    @Override
    public void onClick(View view) {
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(getApplication().INPUT_METHOD_SERVICE);

        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
        int i = view.getId();
        if (i == R.id.btnSignIn) {
            // start a new registration
            Intent intent = new Intent(getApplicationContext(),SignIn.class);
            startActivity(intent);

        } else if (i == R.id.btnLogIn) {
            // start log in
            progressBar.setVisibility(View.VISIBLE);
            userLogin();
        }
    }
}
