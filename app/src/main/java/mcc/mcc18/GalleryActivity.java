package mcc.mcc18;

import android.app.Activity;
import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import mcc.mcc18.Objects.UserItem;
import mcc.mcc18.Utils.MLOptions;
import mcc.mcc18.Utils.PicResolution;

import static android.view.View.GONE;

public class GalleryActivity extends AppCompatActivity {

    public class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView categoryName;
        ImageView categoryImageView;

        Activity coiso = null;

        String url = "";

        public CategoryViewHolder(View v) {
            super(v);

            categoryName = (TextView) itemView.findViewById(R.id.categoryName);
            categoryImageView = (ImageView) itemView.findViewById(R.id.galleryImage);

            imageClickListener();

        }

        public void imageClickListener(){
            categoryImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(url.equals("")){
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

    private RadioGroup galleryMode;
    private RadioButton defaultMode;
    private RadioButton usersMode;
    private RadioButton categoryMode;

    private Activity thisActivity = this;

    private RecyclerView galleryRecyclerView;


    private RecyclerView.LayoutManager categoryManager;
    private RecyclerView.Adapter<CategoryViewHolder> categoryViewHolderAdapter;

    private Vector<String> Images;
    private Vector<String> noneImagesVector;
    private Vector<String> orderImages;
    private Vector<String> usersImagesVector;
    private Vector<String> categoryImagesVector;
    private HashMap<String, Vector<String>> ImagesUser;
    private HashMap<String, Vector<String>> ImagesCategory;

    private String chatID;

    private DatabaseReference mFirebaseDatabaseReference;

    private String Mode;

    private void instantiateImagesCategory(){
        ImagesCategory = new HashMap<>();
        for(String string: MLOptions.availableLabels){
            ImagesCategory.put(string, new Vector<String>());
        }
        ImagesCategory.put("others", new Vector<String>());
    }

    private String getUrl(String url){
        if(Settings.imageQuality.first == 0){
            return url;
        }
        String imgUrl = url;
        String tmp = url.split(Settings.imgRefPath)[1].split("\\?alt=media&")[0];
        //get the file name %2f = /
        String tmp2 = tmp.split("%2F")[2];
        if(Settings.imageQuality.equals(PicResolution.HIGHRES)){
            imgUrl = url.replace(tmp2,"high@_" + tmp2);
        }
        else if(Settings.imageQuality.equals(PicResolution.LOWRES)){
            imgUrl = url.replace(tmp2,"low@_" + tmp2);
        }
        return imgUrl;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if ((keyCode == KeyEvent.KEYCODE_BACK))
        {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    public void updateUsers(final String image, final String userName){
        noneImagesVector.add(image);
        Vector<String> newImages = new Vector<>();
        if (ImagesUser.containsKey(userName)){
            newImages = ImagesUser.get(userName);
        }
        newImages.add(image);
        ImagesUser.put(userName, newImages);
    }

    public void updateLabels(final String image){
        String tmp = image.split(mcc.mcc18.Settings.imgRefPath)[1].split("\\?alt=media&")[0];
        try {
            String imgUrl = URLDecoder.decode(tmp, "UTF-8");
            StorageReference db = FirebaseStorage.getInstance().getReference().child(imgUrl);
            db.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                @Override
                public void onSuccess(StorageMetadata storageMetadata) {
                    // Metadata for each pic
                    String labelText = storageMetadata.getCustomMetadata("labels");
                    ArrayList<String> labels = new ArrayList<String>(Arrays.asList(labelText.split(",")));;
                    for(String label: labels){
                        Vector<String> newLabels = ImagesCategory.get(label);
                        newLabels.add(image);
                        ImagesCategory.put(label,newLabels);
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Uh-oh, an error occurred!
                    System.out.println("Error getting image metadata! \n" + exception.toString());
                    Vector<String> newLabels = ImagesCategory.get("others");
                    newLabels.add(image);
                    ImagesCategory.put("others",newLabels);
                }
            });

        } catch (UnsupportedEncodingException e) {
            System.err.println("Lol " + e.toString());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.gallery_activity);

        defaultMode = findViewById(R.id.modeNone);
        usersMode = findViewById(R.id.modeUsers);
        categoryMode = findViewById(R.id.modeCategory);
        Mode = "DEFAULT";

        galleryRecyclerView = findViewById(R.id.galleryRecyclerView);

        defaultMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Mode = "DEFAULT";
                initRecyclerView();

            }
        });

        usersMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Mode = "USERS";
                initRecyclerView();

            }
        });

        categoryMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Mode = "CATEGORY";
                initRecyclerView();

            }
        });

        chatID = getIntent().getStringExtra("chatID");

        FirebaseAuth auth = FirebaseAuth.getInstance();
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mFirebaseDatabaseReference.child("chats").child(chatID).child("messages").orderByChild(auth.getCurrentUser().getUid()).equalTo("authored")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        noneImagesVector = new Vector<String>();
                        orderImages = new Vector<String>();
                        ImagesUser = new HashMap<>();
                        instantiateImagesCategory();
                        for (DataSnapshot uniqueKeySnapshot: dataSnapshot.getChildren()) {
                            if (((HashMap)uniqueKeySnapshot.getValue()).get("imageUrl") != null) {
                                final String userName = (String)((HashMap)uniqueKeySnapshot.getValue()).get("name");
                                final String imageT = (String)((HashMap)uniqueKeySnapshot.getValue()).get("imageUrl").toString();
                                if(imageT.split(Settings.imgRefPath).length < 2){
                                    System.out.println("Not a database image");
                                    updateUsers(imageT, userName);
                                    Vector<String> newLabels = ImagesCategory.get("others");
                                    newLabels.add(imageT);
                                    orderImages.add(imageT);
                                    ImagesCategory.put("others",newLabels);
                                    return;
                                }
                                final String image = getUrl(imageT);
                                String tmpImgId = imageT.split(Settings.imgRefPath)[1].split("\\?alt=media&")[0];
                                orderImages.add(URLDecoder.decode(tmpImgId).split("/")[1]);
                                String tmp = URLDecoder.decode(image.split(Settings.imgRefPath)[1].split("\\?alt=media&")[0]);
                                StorageReference storage = FirebaseStorage.getInstance().getReference();
                                storage.child(tmp).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        updateUsers(image, userName);
                                        updateLabels(image);
                                        initRecyclerView();
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {
                                        updateUsers(imageT, userName);
                                        updateLabels(imageT);
                                        initRecyclerView();
                                    }
                                });
                            }
                            initRecyclerView();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });


    }



    private void initRecyclerView() {
        galleryRecyclerView.setHasFixedSize(true);
        categoryManager = new LinearLayoutManager(this);
        galleryRecyclerView.setLayoutManager(categoryManager);

        Vector<String> orderedImages = new Vector<>();
        Vector<String> unorderedImages = (Vector<String>) noneImagesVector.clone();

        for(String s : orderImages){
            for(String unordered: unorderedImages){
                if(unordered.contains(s)){
                    orderedImages.add(unordered);
                    unorderedImages.remove(unordered);
                    break;
                }
            }
        }
        Collections.reverse(orderedImages);
        Images = new Vector<>();
        if (Mode == "DEFAULT"){
            Images = orderedImages;
        }

        if (Mode == "USERS"){
            Set users = ImagesUser.keySet();
            usersImagesVector = new Vector<>();

            for (Object el: users) {
                usersImagesVector.add((String) el + ":");
                for (String image : ImagesUser.get(el)){
                    usersImagesVector.add(image);
                }
            }
            Images = usersImagesVector;
        }

        if (Mode == "CATEGORY"){
            Set labels = ImagesCategory.keySet();
            categoryImagesVector = new Vector<>();

            for (Object el: labels) {
                categoryImagesVector.add((String) el + ":");
                for (String image : ImagesCategory.get(el)){
                    categoryImagesVector.add(image);
                }
            }
            Images = categoryImagesVector;
        }

        categoryViewHolderAdapter = new RecyclerView.Adapter<CategoryViewHolder>() {
            @Override
            public CategoryViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_gallery
                        , viewGroup, false);
                return new CategoryViewHolder(view);
            }

            @Override
            public void onBindViewHolder(@NonNull CategoryViewHolder viewHolder, int i) {
                viewHolder.coiso = thisActivity;
                if (URLUtil.isHttpUrl(Images.get(i)) ||
                        URLUtil.isHttpsUrl(Images.get(i))){

                    viewHolder.categoryImageView.setVisibility(ImageView.VISIBLE);
                    viewHolder.categoryName.setVisibility(ImageView.GONE);

                    Glide.with(viewHolder.categoryImageView.getContext())
                            .load(Images.get(i))
                            .into(viewHolder.categoryImageView);
                    viewHolder.url = Images.get(i);
                }

                else {
                    viewHolder.categoryImageView.setVisibility(ImageView.GONE);
                    viewHolder.categoryName.setVisibility(ImageView.VISIBLE);
                    viewHolder.categoryName.setText(Images.get(i));
                }
            }

            @Override
            public int getItemCount() {
                return Images.size();
            }

        };
        galleryRecyclerView.setAdapter(categoryViewHolderAdapter);

    }
}
