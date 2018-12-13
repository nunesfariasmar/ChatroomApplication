package mcc.mcc18;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.Timer;
import java.util.TimerTask;

import mcc.mcc18.Notifications.MessageService;
import mcc.mcc18.Notifications.MyFirebaseService;
import mcc.mcc18.Objects.ChatItem;

public class MainActivity extends AppCompatActivity {
    // Firebase instance variables
    public class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView lastMessageTextView;
        TextView messengerTextView;
        TextView timeStampTextView;
        TextView chatNameTextView;
        ImageView messengerImageView;
        TextView idtextView;
        LinearLayout chatLinearLayout;


        public ChatViewHolder(View v) {
            super(v);
            chatNameTextView = (TextView) itemView.findViewById(R.id.messageNameTextView);
            lastMessageTextView = (TextView) itemView.findViewById(R.id.messageTextView);
            messengerTextView = (TextView) itemView.findViewById(R.id.messengerTextView);
            timeStampTextView = (TextView) itemView.findViewById(R.id.messageTimeStamp);
            messengerImageView = (ImageView) itemView.findViewById(R.id.messengerImageView);
            chatLinearLayout = (LinearLayout) itemView.findViewById(R.id.chatLinearLayout);
            idtextView = (TextView) itemView.findViewById(R.id.idtextView);

            chatLinearLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent chatIntent = new Intent(v.getContext(), SingleChatActivity.class);
                    MessageService.activeChat = chatNameTextView.getText().toString();
                    chatIntent.putExtra("chatID", idtextView.getText().toString());
                    startActivity(chatIntent);
                }
            });
        }
    }

    private DatabaseReference mFirebaseDatabaseReference;
    private FirebaseRecyclerAdapter<ChatItem, ChatViewHolder> mFirebaseAdapter;
    private SharedPreferences mSharedPreferences;
    private String mUsername;
    private String mUserID;

    private String mPhotoUrl;
    private ImageView mAddMessageImageView;
    private ProgressBar mProgressBar;
    private TextView errorMessageView;
    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;

    private Button addChatButton;

    private static final String TAG = "MainActivity";
    public static final String MESSAGES_CHILD = "messages";
    public static final String CHAT_CHILD = "chats";
    private static final int REQUEST_IMAGE = 2;
    private static final String LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif";

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.setting:
                startActivity(new Intent(this,EditProfile.class));
                return true;
            case R.id.logOut:
                logout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void logout(){
        MyFirebaseService.sendRegistrationToServer("offline");
        finish();
        FirebaseAuth session=FirebaseAuth.getInstance();
        session.signOut();
        startActivity(new Intent(this,HomeLogIn.class));


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[] { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE }, 0);
        }

        mProgressBar = findViewById(R.id.progressBar);
        errorMessageView = findViewById(R.id.no_chats);

        addChatButton = findViewById(R.id.addChatBtn);

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser user = firebaseAuth.getCurrentUser();
        mUsername = user.getDisplayName();
        mUserID = user.getUid();

        MessageService.activeChat = null;
        MessageService.coiso = this;

        addChatButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(final View v) {
                Intent addUsrIntent = new Intent(getApplicationContext(), AddUsers.class);
                addUsrIntent.putExtra("addMoreUsrs","false");
                startActivity(addUsrIntent);

            }
        });

        mMessageRecyclerView = findViewById(R.id.galleryRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(false);

        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        //FCM TOKEN
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "getInstanceId failed", task.getException());
                            return;
                        }

                        // Get new Instance ID token
                        String mFCMToken = task.getResult().getToken();

                        // Log and toast
//                        String msg = getString(R.string.msg_token_fmt, mFCMToken);
                        MyFirebaseService.sendRegistrationToServer(mFCMToken);
//                        System.out.println(msg);
//                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                });

        // New child entries
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();

        SnapshotParser<ChatItem> parser = new SnapshotParser<ChatItem>() {
            @Override
            public ChatItem parseSnapshot(DataSnapshot dataSnapshot) {
                ChatItem chatItem = dataSnapshot.getValue(ChatItem.class);

                if (chatItem != null) {
                    chatItem.setId(dataSnapshot.getKey());
                }

                return chatItem;
            }
        };

        Query chatsRef = mFirebaseDatabaseReference.child(CHAT_CHILD).orderByChild(mUserID).equalTo("authored");
        FirebaseRecyclerOptions<ChatItem> options =
                new FirebaseRecyclerOptions.Builder<ChatItem>()
                        .setQuery(chatsRef, parser)
                        .build();

        mFirebaseAdapter = new FirebaseRecyclerAdapter<ChatItem, ChatViewHolder>(options) {

            @Override
            public ChatViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
                LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
                return new ChatViewHolder(inflater.inflate(R.layout.item_chat, viewGroup, false));
            }

            @Override
            protected void onBindViewHolder(final ChatViewHolder viewHolder,
                                            int position,
                                            ChatItem chatItem) {
                errorMessageView.setVisibility(TextView.INVISIBLE);
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                if (chatItem.getLastMessage() != null) {
                    viewHolder.lastMessageTextView.setText(chatItem.getLastMessage());
                    viewHolder.lastMessageTextView.setVisibility(TextView.VISIBLE);
                    //TEST ANTONIO
                    //viewHolder.messengerImageView.setImageURI(mPhotoUrl);
                }


                viewHolder.messengerTextView.setText(chatItem.getUsername());

                if(chatItem.getChatName() != null)
                    viewHolder.chatNameTextView.setText(chatItem.getChatName());
                if (chatItem.getId() != null) {
                    viewHolder.messengerImageView.setImageDrawable(ContextCompat.getDrawable(MainActivity.this,
                            R.drawable.chat));
                    viewHolder.idtextView.setText(chatItem.getId());

                }

            }
        };

        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int chatItemCount = mFirebaseAdapter.getItemCount();
                Log.d(TAG, "The Item Count is: " + chatItemCount);
                int lastVisiblePosition =
                        mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
            }
        });

        loadingTimer();
        mMessageRecyclerView.setAdapter(mFirebaseAdapter);
    };

    public void loadingTimer(){
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
            }
        }, 5*1000);
    }

    @Override
    public void onPause() {
        mFirebaseAdapter.stopListening();
        super.onPause();

    }

    @Override
    public void onResume() {
        super.onResume();
        mFirebaseAdapter.startListening();
    }
}