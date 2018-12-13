package mcc.mcc18;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.request.RequestOptions;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import mcc.mcc18.Notifications.MessageService;
import mcc.mcc18.Objects.FriendlyMessage;
import mcc.mcc18.Utils.PicResolution;
import mcc.mcc18.Utils.Uploader;
import mcc.mcc18.editProfile.PopActivity;
import mcc.mcc18.editProfile.PopPicture;


public class SingleChatActivity extends AppCompatActivity {
    // Firebase instance variables

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        ImageView messageImageView;
        TextView messengerTextView;
        ImageView messengerImageView; // circleimageview
        String url = "";
        Activity coiso = null;

        public MessageViewHolder(View v) {
            super(v);
            messageTextView = (TextView) itemView.findViewById(R.id.messageTextView);
            messageImageView = (ImageView) itemView.findViewById(R.id.messageImageView);
            messengerTextView = (TextView) itemView.findViewById(R.id.messengerTextView);
            messengerImageView = (ImageView) itemView.findViewById(R.id.messengerImageView);
            imageClickListener();
        }

        public void imageClickListener(){
            messageImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(url.equals("") || coiso == null){
                        System.out.println("Wrong url?!");
                        return;
                    }
                    Intent fullimage = new Intent(coiso,ImageDisplay.class);
                    fullimage.putExtra("url",url);

                    coiso.startActivity(fullimage);
                }
            });
        }
    }

    private DatabaseReference mFirebaseDatabaseReference;
    private FirebaseRecyclerAdapter<FriendlyMessage, MessageViewHolder> mFirebaseAdapter;
    private Button mSendButton;
    private EditText mMessageEditText;
    private SharedPreferences mSharedPreferences;
    private String mUsername;
    private String mUserID;
    private String mPhotoUrl;
    private ImageView mAddMessageImageView;
    private ProgressBar mProgressBar;
    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private FirebaseUser mFirebaseUser;
    private FirebaseAnalytics mFirebaseAnalytics;
    private Activity currentActivity = this;
    private Uri photoUri;
    private ContentValues values;


    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int CAPTURE_PHOTO = 104;
    static final int STATIC_INTEGER_VALUE=1;


    private String chatID;

    public static final String TAG = "MainActivity";
    private static final String TABLE_NAME = "chats";
    public static final String MESSAGES_CHILD = "messages";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 240;
    private static final int REQUEST_IMAGE = 2;
    private static final String LOADING_IMAGE_URL = "https://i.imgur.com/Qk25QoR.gif";

    private Vector<String> chatImages;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.chat_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.addMoreUsrs:
                Intent addMoreUsersIntent = new Intent(this,AddUsers.class);
                addMoreUsersIntent.putExtra("chatID",chatID);
                addMoreUsersIntent.putExtra("addMoreUsrs","true");
                startActivity(addMoreUsersIntent);
                return true;

            case R.id.chatGallery:
                Intent galleryIntent = new Intent(this,GalleryActivity.class);
                galleryIntent.putExtra("chatID",chatID);
                startActivity(galleryIntent);
                return true;

            case R.id.leaveChat:
                mFirebaseDatabaseReference.child(TABLE_NAME).child(chatID).child(mUserID).removeValue();
                DatabaseReference db = mFirebaseDatabaseReference.child("chats").child(chatID).child("users");
                db.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String users = dataSnapshot.getValue().toString();
                        String[] us = users.split("&");
                        Vector<String> newUsers = new Vector<>();
                        for (String user: us){
                            if(!user.equals(mUserID))
                                newUsers.add(user);
                        }
                        String newString = newUsers.get(0);
                        for (int i = 1; i < newUsers.size(); i++){
                            newString += "&" + newUsers.get(i);
                        }
                        mFirebaseDatabaseReference.child("chats").child(chatID).child("users").setValue(newString);
                    }
                    @Override
                    public void onCancelled(DatabaseError error) {
                    }
                });

                FriendlyMessage friendlyMessage = new
                        FriendlyMessage(mUsername + " has exited the chat room.",
                        mUsername,
                        mPhotoUrl,
                        null /* no image */);
                DatabaseReference tmpUpload = mFirebaseDatabaseReference.child(TABLE_NAME).child(chatID).child(MESSAGES_CHILD)
                        .push();
                tmpUpload.setValue(friendlyMessage, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError,
                                           DatabaseReference databaseReference) {
                        final String uniqueKey = databaseReference.getKey();

                        DatabaseReference db = mFirebaseDatabaseReference.child("chats").child(chatID).child("users");
                        db.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                String users = dataSnapshot.getValue().toString();
                                String[] us = users.split("&");
                                for (String user : us) {
                                    mFirebaseDatabaseReference.child("chats").child(chatID).child("messages").child(uniqueKey).child(user).setValue("authored");
                                }
                                mFirebaseDatabaseReference.child(TABLE_NAME).child(chatID).child("lastMessage").setValue(mUsername + " has exited the chat room.");
                                mFirebaseDatabaseReference.child(TABLE_NAME).child(chatID).child("username").setValue(mUsername);
                                mFirebaseDatabaseReference.child(TABLE_NAME).child(chatID).child("timeStamp").setValue(System.currentTimeMillis() + "");
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {
                            }
                        });
                    }
                });
                Intent intent = new Intent(this,MainActivity.class);
                startActivity(intent);
                return true;
            case R.id.chatInfo:
                Intent chatInfoIntent = new Intent(this,ChatInfoActivity.class);
                chatInfoIntent.putExtra("chatID",chatID);
                //addMoreUsersIntent.putExtra("addMoreUsrs","true");
                startActivity(chatInfoIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.single_chat);

        MessageService.coiso = this;

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = firebaseAuth.getCurrentUser();
        mUsername = mFirebaseUser.getDisplayName();
        mPhotoUrl = mFirebaseUser.getPhotoUrl().toString();
        mUserID = mFirebaseUser.getUid();

        this.chatID = getIntent().getStringExtra("chatID");
        chatImages = new Vector<String>();

        values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
        photoUri = getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        mProgressBar = findViewById(R.id.progressBar);
        mMessageRecyclerView = findViewById(R.id.galleryRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);

        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Initialize Firebase Measurement.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // New child entries
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        SnapshotParser<FriendlyMessage> parser = new SnapshotParser<FriendlyMessage>() {
            @Override
            public FriendlyMessage parseSnapshot(DataSnapshot dataSnapshot) {
                FriendlyMessage friendlyMessage = dataSnapshot.getValue(FriendlyMessage.class);
                if (friendlyMessage != null) {
                    friendlyMessage.setId(dataSnapshot.getKey());
                }
                return friendlyMessage;
            }
        };

        Query messagesRef = mFirebaseDatabaseReference.child(TABLE_NAME).child(chatID).child(MESSAGES_CHILD).orderByChild(mUserID).equalTo("authored");
        FirebaseRecyclerOptions<FriendlyMessage> options =
                new FirebaseRecyclerOptions.Builder<FriendlyMessage>()
                        .setQuery(messagesRef, parser)
                        .build();


        mFirebaseAdapter = new FirebaseRecyclerAdapter<FriendlyMessage, MessageViewHolder>(options) {

            @Override
            public MessageViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
                LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
                return new MessageViewHolder(inflater.inflate(R.layout.item_message, viewGroup, false));
            }

            private void setTextMessage(final MessageViewHolder viewHolder, FriendlyMessage friendlyMessage){
                viewHolder.messageTextView.setText(friendlyMessage.getText());
                viewHolder.messageTextView.setVisibility(TextView.VISIBLE);
                viewHolder.messageImageView.setVisibility(ImageView.GONE);
            }

            private void displayImageStorage(final MessageViewHolder viewHolder, String imageUrl){
                StorageReference storageReference = FirebaseStorage.getInstance()
                        .getReferenceFromUrl(imageUrl);
                storageReference.getDownloadUrl().addOnCompleteListener(
                        new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                if (task.isSuccessful()) {
                                    String downloadUrl = task.getResult().toString();
                                    viewHolder.url = downloadUrl;
                                    viewHolder.coiso = currentActivity;
                                    Glide.with(viewHolder.messageImageView.getContext())
                                            .load(downloadUrl)
                                            .into(viewHolder.messageImageView);
                                } else {
                                    Log.w(TAG, "Getting download url was not successful.",
                                            task.getException());
                                }
                            }
                        });
            }

            private void displayImageURL(final MessageViewHolder viewHolder, String imageUrl){
                viewHolder.url = imageUrl;
                viewHolder.coiso = currentActivity;
                Glide.with(viewHolder.messageImageView.getContext())
                        .load(imageUrl)
                        .into(viewHolder.messageImageView);
            }

            private void setImageMessage(final MessageViewHolder viewHolder, String imageUrl){
                if (imageUrl.startsWith("gs://")) {
                    displayImageStorage(viewHolder, imageUrl);
                } else {
                    System.out.println("Image Url " + imageUrl);
                    displayImageURL(viewHolder, imageUrl);
                }
                viewHolder.messageImageView.setVisibility(ImageView.VISIBLE);
                viewHolder.messageTextView.setVisibility(TextView.GONE);
            }

            private void updateUrl(final MessageViewHolder viewHolder,final String url){
                if(Settings.imageQuality.first == 0){
                    setImageMessage(viewHolder, url);
                    return;
                }
                String imgUrl = "";
                String tmp = url.split(Settings.imgRefPath)[1].split("\\?alt=media&")[0];
                String googleUrl = tmp.replace("%2F","/").replace("%3A",":");
                //get the file name %2f = /
                String tmp2 = tmp.split("%2F")[2];
                String tmp3 = googleUrl.split("/")[2];
                if(Settings.imageQuality.equals(PicResolution.HIGHRES)){
                    imgUrl = url.replace(tmp2,"high@_" + tmp2);
                    googleUrl = googleUrl.replace(tmp3,"high@_" + tmp3);
                }
                else if(Settings.imageQuality.equals(PicResolution.LOWRES)){
                    imgUrl = url.replace(tmp2,"low@_" + tmp2);
                    googleUrl = googleUrl.replace(tmp3,"low@_" + tmp3);
                }
                else{
                    setImageMessage(viewHolder, url);
                    return;
                }
                final String fUrl = imgUrl;
                StorageReference storage = FirebaseStorage.getInstance().getReference();
                storage.child(googleUrl).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        setImageMessage(viewHolder, fUrl);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        setImageMessage(viewHolder, url);
                    }
                });
            }

            @Override
            protected void onBindViewHolder(final MessageViewHolder viewHolder,
                                            int position,
                                            FriendlyMessage friendlyMessage) {
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                if (friendlyMessage.getText() != null) {
                    setTextMessage(viewHolder, friendlyMessage);
                } else if (friendlyMessage.getImageUrl() != null) {
                    String imgUrl = friendlyMessage.getImageUrl();
                    String[] parsedUrl = imgUrl.split(Settings.imgRefPath);
                    if(parsedUrl.length == 1){
                        setImageMessage(viewHolder, imgUrl);
                    }
                    else{
//                        String[] tmp = parsedUrl[1].split("\\?alt=media&");
//                        String lowVersionUrl = Settings.getConvertedImgUrl(tmp[0]);
                        updateUrl(viewHolder, imgUrl);
//                        setImageMessage(viewHolder, imgUrl);
                    }
                }

                viewHolder.messengerTextView.setText(friendlyMessage.getName());
                if (friendlyMessage.getPhotoUrl() == null) {
                    viewHolder.messengerImageView.setImageDrawable(ContextCompat.getDrawable(SingleChatActivity.this,
                            R.drawable.ic_account_circle_black_36dp));
                } else {
                    Glide.with(SingleChatActivity.this)
                            .load(friendlyMessage.getPhotoUrl())
                            .apply(RequestOptions.circleCropTransform())
                            .into(viewHolder.messengerImageView);
                }

            }
        };

        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = mFirebaseAdapter.getItemCount();
                Log.d(TAG, "The Item Count is: " + friendlyMessageCount);
                int lastVisiblePosition =
                        mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (friendlyMessageCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    mMessageRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        mMessageRecyclerView.setAdapter(mFirebaseAdapter);

        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(mSharedPreferences
                .getInt("friendly_msg_length", DEFAULT_MSG_LENGTH_LIMIT))});

        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });



        mAddMessageImageView = (ImageView) findViewById(R.id.addMessageImageView);
        mAddMessageImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // popUp decide between camera and gallery

                Intent intent = new Intent(getApplicationContext(),PopPicture.class);
                startActivityForResult(intent, STATIC_INTEGER_VALUE);

            }
        });

        mSendButton = (Button) findViewById(R.id.sendButton);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FriendlyMessage friendlyMessage = new
                        FriendlyMessage(mMessageEditText.getText().toString(),
                        mUsername,
                        mPhotoUrl,
                        null /* no image */);
                DatabaseReference tmpUpload = mFirebaseDatabaseReference.child(TABLE_NAME).child(chatID).child(MESSAGES_CHILD)
                        .push();
                tmpUpload.setValue(friendlyMessage, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError,
                                           DatabaseReference databaseReference) {
                        final String uniqueKey = databaseReference.getKey();

                        addAuthorityToMessage(uniqueKey);
                    }
                });
                mFirebaseDatabaseReference.child(TABLE_NAME).child(chatID).child("lastMessage").setValue(mMessageEditText.getText().toString());
                mFirebaseDatabaseReference.child(TABLE_NAME).child(chatID).child("username").setValue(mUsername);
                mFirebaseDatabaseReference.child(TABLE_NAME).child(chatID).child("timeStamp").setValue(System.currentTimeMillis() + "");
                mMessageEditText.setText("");
            }
        });

        loadingTimer();
    };

    public void addAuthorityToMessage(final String uniqueKey){
        DatabaseReference db = mFirebaseDatabaseReference.child(TABLE_NAME).child(chatID).child("users");
        db.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String users = dataSnapshot.getValue().toString();
                String[] us = users.split("&");
                for (String user: us){
                    mFirebaseDatabaseReference.child(TABLE_NAME).child(chatID).child(MESSAGES_CHILD).child(uniqueKey).child(user).setValue("authored");
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
            }
        });
    }

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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 0) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                mAddMessageImageView.setEnabled(true);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        switch(requestCode) {
            case (STATIC_INTEGER_VALUE) : {
                if (resultCode == Activity.RESULT_OK) {

                    if (data.getStringExtra("decision").equals("gallery")){

                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("image/*");
                        startActivityForResult(intent, REQUEST_IMAGE);


                    }
                    if (data.getStringExtra("decision").equals("camera")){

                        //request for permission
                        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            mAddMessageImageView.setEnabled(false);
                            ActivityCompat.requestPermissions(SingleChatActivity.this, new String[] { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE }, 0);
                        }
                        takephoto();


                    }

                }
                break;
            }
        }




        if (resultCode == RESULT_OK) {
            if (requestCode == CAPTURE_PHOTO){
//                Bundle extras = data.getExtras();
                final Bitmap imageBitmap;
                try {
                    imageBitmap = MediaStore.Images.Media.getBitmap(
                            getContentResolver(), photoUri);
                } catch (IOException e) {
                    System.out.println("Couldn't load photo taken :(" + e.toString());
                    return;
                }
                ;
//                final Uri uri = saveImageToGallery(imageBitmap);
                Random generator=new Random();
                int n = 10000;
                n = generator.nextInt(n);
                final String imageName="image:"+n+".jpg";

                FriendlyMessage tempMessage = new FriendlyMessage(null, mUsername, mPhotoUrl,
                        LOADING_IMAGE_URL);
                DatabaseReference tmpUpload = mFirebaseDatabaseReference.child(TABLE_NAME).child(chatID).child(MESSAGES_CHILD).push();
                tmpUpload.setValue(tempMessage, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError,
                                           DatabaseReference databaseReference) {
                        if (databaseError == null) {
                            final String key = databaseReference.getKey();
                            addAuthorityToMessage(key);
                            StorageReference storageReference =
                                    FirebaseStorage.getInstance()
                                            .getReference("-LRlF8RnT78YXBlFelPy")
                                            .child(key)
                                            .child(imageName);

                            Uploader uploader = new Uploader(getContentResolver(),mUsername,mPhotoUrl,mFirebaseDatabaseReference.child(TABLE_NAME).child(chatID).child(MESSAGES_CHILD));
                            uploader.uploadImage2(storageReference,imageBitmap,key);
//                                        putBitmapInStorage(storageReference, uri, key);
                        } else {
                            Log.w(TAG, "Unable to write message to database.",
                                    databaseError.toException());
                        }
                    }
                });
                mFirebaseDatabaseReference.child(TABLE_NAME).child(chatID).child("lastMessage").setValue("image sent by " + mUsername);
                mFirebaseDatabaseReference.child(TABLE_NAME).child(chatID).child("username").setValue(mUsername);
                mFirebaseDatabaseReference.child(TABLE_NAME).child(chatID).child("timeStamp").setValue(System.currentTimeMillis() + "");
            }

        }

        if (requestCode == REQUEST_IMAGE) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    final Uri uri = data.getData();

                    FriendlyMessage tempMessage = new FriendlyMessage(null, mUsername, mPhotoUrl,
                            LOADING_IMAGE_URL);
                    DatabaseReference tmpUpload = mFirebaseDatabaseReference.child(TABLE_NAME).child(chatID).child(MESSAGES_CHILD).push();
                    tmpUpload.setValue(tempMessage, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError databaseError,
                                                       DatabaseReference databaseReference) {
                                    if (databaseError == null) {
                                        final String key = databaseReference.getKey();
                                        addAuthorityToMessage(key);
                                        StorageReference storageReference =
                                                FirebaseStorage.getInstance()
                                                        .getReference("-LRlF8RnT78YXBlFelPy")
                                                        .child(key)
                                                        .child(uri.getLastPathSegment());

                                        Uploader uploader = new Uploader(getContentResolver(),mUsername,mPhotoUrl,mFirebaseDatabaseReference.child(TABLE_NAME).child(chatID).child(MESSAGES_CHILD));
                                        uploader.uploadImage(storageReference,uri,key);
//                                        putBitmapInStorage(storageReference, uri, key);
                                    } else {
                                        Log.w(TAG, "Unable to write message to database.",
                                                databaseError.toException());
                                    }
                                }
                            });
                    mFirebaseDatabaseReference.child(TABLE_NAME).child(chatID).child("lastMessage").setValue("image sent by " + mUsername);
                    mFirebaseDatabaseReference.child(TABLE_NAME).child(chatID).child("username").setValue(mUsername);
                    mFirebaseDatabaseReference.child(TABLE_NAME).child(chatID).child("timeStamp").setValue(System.currentTimeMillis() + "");

                }
            }
        }
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

    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(this,MainActivity.class);
        startActivity(intent);
    }

    public void takephoto(){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        startActivityForResult(intent, CAPTURE_PHOTO);

//        Intent cameraPict=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        startActivityForResult(cameraPict,CAPTURE_PHOTO);
    }

}