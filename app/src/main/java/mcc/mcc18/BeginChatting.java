package mcc.mcc18;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class BeginChatting extends AppCompatActivity implements View.OnClickListener{

    private FirebaseAuth firebaseAuth;
    private Button logOut;
    private Button btnSetting;
    private TextView test3;
    private TextView test1;


    // write db
    private DatabaseReference databaseReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_begin_chatting);

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();




        if(firebaseAuth.getCurrentUser() == null){
            finish();
            startActivity(new Intent(this,HomeLogIn.class));
        }

        FirebaseUser user = firebaseAuth.getCurrentUser();
        test3 = (TextView) findViewById(R.id.test3);
        test3.setText("Welcome "+user.getDisplayName());
        test1 = (TextView) findViewById(R.id.test1);
        test1.setText("Welcome "+user.getEmail());


        logOut =(Button) findViewById(R.id.btnlogOut);
        logOut.setOnClickListener(this);

        btnSetting=(Button) findViewById(R.id.btnSetting);
        btnSetting.setOnClickListener(this);



    }



    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.btnlogOut){
            // LOG OUT
            firebaseAuth.signOut();
            finish();
            startActivity(new Intent(this,HomeLogIn.class));
        }
        if (i == R.id.btnSetting){
            startActivity(new Intent(this,EditProfile.class));
        }
    }
}
