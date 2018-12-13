package mcc.mcc18;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Vector;
import mcc.mcc18.Objects.UserItem;


public class ChatInfoActivity extends AppCompatActivity {

    public class UserViewHolder extends RecyclerView.ViewHolder {
        TextView nameView;
        TextView mailView;
        TextView idView;
        ImageView imageView;
        final Button addBtn;

        public UserViewHolder(View v) {
            super(v);

            nameView = (TextView) itemView.findViewById(R.id.nameView);
            mailView = (TextView) itemView.findViewById(R.id.mailView);
            idView = (TextView) itemView.findViewById(R.id.idView);
            imageView = (ImageView) itemView.findViewById(R.id.messengerImageView);
            addBtn = itemView.findViewById(R.id.addUserBtn);

            addBtn.setVisibility(Button.INVISIBLE);
        }
    }

    private static final String TABLE_NAME = "chats";

    private TextView chatName;
    private ImageView chatImage;
    private RecyclerView chatUsers;

    private RecyclerView.LayoutManager mLayoutManager;
    private RecyclerView.Adapter<UserViewHolder> mAdapter;

    private String chatID;

    private HashMap<String, UserItem> users;
    private Vector<UserItem> chatUsersVector;


    private DatabaseReference mFirebaseDatabaseReference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.chat_info);

        chatName = findViewById(R.id.chatName);
        chatImage = findViewById(R.id.chatImage);
        chatUsers = findViewById(R.id.chatUsers);

        chatID = getIntent().getStringExtra("chatID");

        users = new HashMap<String, UserItem>();
        chatUsersVector = new Vector<>();

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();

        mFirebaseDatabaseReference.child("users").orderByChild("name")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        for (DataSnapshot uniqueKeySnapshot : dataSnapshot.getChildren()) {
                            String userId = uniqueKeySnapshot.getKey();
                            String userName = uniqueKeySnapshot.child("name").getValue(String.class);
                            String userEmail = uniqueKeySnapshot.child("mail").getValue(String.class);
                            String userphoto = uniqueKeySnapshot.child("photoUrl").getValue(String.class);

                            UserItem item = new UserItem(userName, userEmail, userId, userphoto);
                            users.put(userId, item);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

        mFirebaseDatabaseReference.child("chats").child(chatID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        chatName.setText(dataSnapshot.child("chatName").getValue(String.class));
                        chatUsersVector = new Vector<>();

                        for (DataSnapshot uniqueKeySnapshot : dataSnapshot.getChildren()) {
                            if (users.containsKey(uniqueKeySnapshot.getKey())) {
                               String userId = uniqueKeySnapshot.getKey();

                                chatUsersVector.add(users.get(userId));
                            }
                        }


                        initRecyclerView();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });


    }

    private void initRecyclerView() {
        chatUsers.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        chatUsers.setLayoutManager(mLayoutManager);


        mAdapter = new RecyclerView.Adapter<UserViewHolder>() {
            @Override
            public UserViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_user
                        , viewGroup, false);
                return new UserViewHolder(view);
            }

            @Override
            public void onBindViewHolder(UserViewHolder viewHolder, int i) {
                viewHolder.nameView.setText(chatUsersVector.get(i).getName());
                viewHolder.idView.setText(chatUsersVector.get(i).getId());
                viewHolder.mailView.setText(chatUsersVector.get(i).getMail());
                if (chatUsersVector.get(i).getPhotoUrl() == null) {
                    viewHolder.imageView.setImageDrawable(ContextCompat.getDrawable(ChatInfoActivity.this,
                            R.drawable.ic_account_circle_black_36dp));
                } else {
                    Glide.with(ChatInfoActivity.this)
                            .load(chatUsersVector.get(i).getPhotoUrl())
                            .apply(RequestOptions.circleCropTransform())
                            .into(viewHolder.imageView);
                }
            }

            @Override
            public int getItemCount() {
                return chatUsersVector.size();
            }

        };
        chatUsers.setAdapter(mAdapter);

    }





    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


}
