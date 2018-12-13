package mcc.mcc18;

import android.content.DialogInterface;
import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Vector;

import mcc.mcc18.Notifications.MessageService;
import mcc.mcc18.Objects.ChatItem;
import mcc.mcc18.Objects.FriendlyMessage;
import mcc.mcc18.Objects.UserItem;

public class AddUsers extends AppCompatActivity {
    private Button searchBtn;
    private Button nextBtn;
    private String textInput;
    private RecyclerView recView;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference mFirebaseDatabaseReference;
    private LinearLayoutManager mLinearLayoutManager;
    SnapshotParser<UserItem> parser;

    private Vector<String> usersToAdd;
    private Vector<String> userNamesToAdd;
    private boolean addMoreUsers;
    private String existingChatID;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firebaseAuth = FirebaseAuth.getInstance();
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();

        String addM = getIntent().getStringExtra("addMoreUsrs");
        addMoreUsers = addM.equals("true");

        usersToAdd = new Vector<>();
        userNamesToAdd = new Vector<>();
        if(addMoreUsers){
            existingChatID = getIntent().getStringExtra("chatID");
            DatabaseReference db = mFirebaseDatabaseReference.child("chats").child(existingChatID).child("users");
            db.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String users = dataSnapshot.getValue().toString();
                    String[] us = users.split("&");
                    for (String user: us){
                        usersToAdd.add(user);
                    }
                    proceed();
                }

                @Override
                public void onCancelled(DatabaseError error) {
                }
            });
        } else {
            usersToAdd.add(firebaseAuth.getCurrentUser().getUid());
            proceed();
        }
    }

    private void proceed() {
        setContentView(R.layout.activity_add_users);

        recView = findViewById(R.id.recyclerViewAddUsr);
        searchBtn = findViewById(R.id.searchBtn);
        nextBtn = findViewById(R.id.nextBtn);

        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(false);

        recView.setLayoutManager(mLinearLayoutManager);

        searchBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditText et = (EditText)findViewById(R.id.textInput);
                textInput = et.getText().toString();
                searchUsers(textInput);
            }
        });

        nextBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(final View v) {
                if(!addMoreUsers){
                    AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                    builder.setTitle("Create a chat");

                    final EditText input = new EditText(v.getContext());
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    input.setHint("Enter your Chat Name");
                    builder.setView(input);

                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final String chatName = input.getText().toString();
                            ChatItem item = new
                                    ChatItem(null,
                                    "None",
                                    "None" ,
                                    "None",
                                    chatName);
                            mFirebaseDatabaseReference.child("chats")
                                    .push().setValue(item, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError databaseError,
                                                       DatabaseReference databaseReference) {
                                    String uniqueKey = databaseReference.getKey();
                                    MessageService.activeChat = chatName;
                                    String usrs = usersToAdd.get(0);
                                    for (int i = 0; i < usersToAdd.size(); i++) {
                                        mFirebaseDatabaseReference.child("chats").child(uniqueKey).child(usersToAdd.get(i)).setValue("authored");
                                        if(i != 0)
                                            usrs += "&" + usersToAdd.get(i);
                                    }
                                    mFirebaseDatabaseReference.child("chats").child(uniqueKey).child("users").setValue(usrs);
                                    mFirebaseDatabaseReference.child("chats").child(uniqueKey).child("timeStamp").setValue(System.currentTimeMillis() + "");
                                    Intent chatIntent = new Intent(v.getContext(), SingleChatActivity.class);
                                    chatIntent.putExtra("chatID",uniqueKey);
                                    startActivity(chatIntent);
                                }
                            });

                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder.show();
                } else {
                    String usrs = usersToAdd.get(0);
                    for (int i = 0; i < usersToAdd.size(); i++) {
                        mFirebaseDatabaseReference.child("chats").child(existingChatID).child(usersToAdd.get(i)).setValue("authored");
                        if(i != 0)
                            usrs += "&" + usersToAdd.get(i) ;
                    }
                    mFirebaseDatabaseReference.child("chats").child(existingChatID).child("users").setValue(usrs);
                    String names = "";
                    for (int i = 0; i < userNamesToAdd.size(); i++){
                        names += userNamesToAdd.get(i);
                        if(i+1 != userNamesToAdd.size())
                            names += ", ";
                    }
                    FriendlyMessage friendlyMessage = new
                            FriendlyMessage(names + " joined the chat room.",
                            "",
                            null,
                            null);
                    DatabaseReference tmp = mFirebaseDatabaseReference.child("chats").child(existingChatID).child("messages")
                            .push();
                    final String fnames = names;
                    tmp.setValue(friendlyMessage, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError,
                                               DatabaseReference databaseReference) {
                            final String uniqueKey = databaseReference.getKey();

                            DatabaseReference db = mFirebaseDatabaseReference.child("chats").child(existingChatID).child("users");
                            db.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    String users = dataSnapshot.getValue().toString();
                                    String[] us = users.split("&");
                                    for (String user : us) {
                                        mFirebaseDatabaseReference.child("chats").child(existingChatID).child("messages").child(uniqueKey).child(user).setValue("authored");
                                    }
                                    mFirebaseDatabaseReference.child("chats").child(existingChatID).child("lastMessage").setValue(fnames + " joined the chat room.");
                                    mFirebaseDatabaseReference.child("chats").child(existingChatID).child("timeStamp").setValue(System.currentTimeMillis() + "");
                                    mFirebaseDatabaseReference.child("chats").child(existingChatID).child("username").setValue(fnames);
                                }

                                @Override
                                public void onCancelled(DatabaseError error) {
                                }
                            });
                        }
                    });
                    Intent chatIntent = new Intent(v.getContext(), SingleChatActivity.class);
                    chatIntent.putExtra("chatID",existingChatID);
                    startActivity(chatIntent);
                }

            }
        });

        parser = new SnapshotParser<UserItem>() {
            @Override
            public UserItem parseSnapshot(DataSnapshot dataSnapshot) {
                return dataSnapshot.getValue(UserItem.class);
            }
        };
    }

    private void searchUsers(final String query){
        DatabaseReference db = mFirebaseDatabaseReference.child("users");
        db.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Vector<UserItem> userSearch = new Vector<>();
                for (DataSnapshot postSnapshot: dataSnapshot.getChildren()) {
                    UserItem userItem = postSnapshot.getValue(UserItem.class);
                    String name = postSnapshot.getValue(UserItem.class).getName().toLowerCase();
                    String uid = postSnapshot.getValue(UserItem.class).getId();
                    if(name.indexOf(query.toLowerCase()) != -1 && !uid.equals(firebaseAuth.getCurrentUser().getUid())){
                        userSearch.add(userItem);
                    }
                }
                SearchRecyclerAdpater adapter = new SearchRecyclerAdpater(userSearch);
                recView.setAdapter(adapter);
            }

            @Override
            public void onCancelled(DatabaseError error) {

            }
        });
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

    public class UserViewHolder extends RecyclerView.ViewHolder {
        TextView nameView;
        TextView mailView;
        TextView idView;
        ImageView imageView;
        final Button addBtn;

        public void hideButton(){
            addBtn.setVisibility(Button.INVISIBLE);
        }

        public UserViewHolder(View v) {
            super(v);
            nameView = (TextView) itemView.findViewById(R.id.nameView);
            mailView = (TextView) itemView.findViewById(R.id.mailView);
            idView = (TextView) itemView.findViewById(R.id.idView);
            imageView = (ImageView) itemView.findViewById(R.id.messengerImageView);
            addBtn = itemView.findViewById(R.id.addUserBtn);

            addBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String id = idView.getText().toString();
                    if(usersToAdd.contains(id)){
                        usersToAdd.remove(id);
                        userNamesToAdd.remove(nameView.getText().toString());
                        addBtn.setText("Add");
                    } else {
                        usersToAdd.add(id);
                        userNamesToAdd.add(nameView.getText().toString());
                        addBtn.setText("Rmv");
                    }
                }
            });
        }
    }

    public class SearchRecyclerAdpater extends RecyclerView.Adapter<UserViewHolder> {
        private Vector<UserItem> users;

        public SearchRecyclerAdpater(Vector<UserItem> users){
            this.users = users;
        }

        @Override
        public UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new AddUsers.UserViewHolder(inflater.inflate(R.layout.item_user, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            holder.idView.setText(users.get(position).getId());
            holder.nameView.setText(users.get(position).getName());
            holder.mailView.setText(users.get(position).getMail());
            if (users.get(position).getId().equals(firebaseAuth.getCurrentUser().getUid()) || usersToAdd.contains(users.get(position).getId())){
                holder.hideButton();
            }
            if (users.get(position).getPhotoUrl() == null) {
                holder.imageView.setImageDrawable(ContextCompat.getDrawable(AddUsers.this,
                        R.drawable.ic_account_circle_black_36dp));
            } else {
                Glide.with(AddUsers.this)
                        .load(users.get(position).getPhotoUrl())
                        .apply(RequestOptions.circleCropTransform())
                        .into(holder.imageView);
            }
        }

        @Override
        public int getItemCount() {
            return users.size();
        }
    }
}
