package com.vjcet.crop;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentText;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.google.firebase.ml.vision.text.RecognizedLanguage;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.vjcet.crop.Helper.PlateModel;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_REQ_CODE =123;
    private RecyclerView MainRecyclerView;
    private List<PlateModel> PlateList;
    private Button UploadBtn;
    private FirebaseAuth mAuth;

    private DatabaseReference PlateRef;
    private Bitmap PlateBitmap=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        PlateList = new ArrayList<>();
        MainRecyclerView = findViewById(R.id.main_recyclerview);
        MainRecyclerView.setHasFixedSize(true);
        MainRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        UploadBtn = findViewById(R.id.main_upload_btn);

        PlateRef = FirebaseDatabase.getInstance().getReference();

        ShowPlates();


        UploadBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
            @Override
            public void onClick(View v) {

                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"Pick image"),PICK_REQ_CODE);

            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_REQ_CODE && resultCode == RESULT_OK && data != null && data.getData() != null) {

            StorageReference storageReference= FirebaseStorage.getInstance().getReference().child("plates");
            final StorageReference ref = storageReference.child("image_"+System.currentTimeMillis()+".jpg");

            ref.putFile(data.getData()).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {

                            String pushid = PlateRef.child("Plates").push().getKey();
                            PlateRef.child("Plates").child(pushid).child("url").setValue(uri.toString());
                            PlateRef.child("Plates").child(pushid).child("time").setValue(ServerValue.TIMESTAMP);

                        }
                    });

                }
            });



        }
    }

    private void ShowPlates() {

       try
       {
           PlateRef.child("Plates").addValueEventListener(new ValueEventListener() {
               @Override
               public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                   PlateList.clear();
                   MainRecyclerView.removeAllViews();
                   for(DataSnapshot snapshot: dataSnapshot.getChildren())
                   {
                       String url="na",time="na";


                       try
                       {
                           url = snapshot.child("url").getValue().toString();
                           time = snapshot.child("time").getValue().toString();
                           PlateList.add(new PlateModel(url,time));

                       }
                       catch (Exception e)
                       {
                           e.printStackTrace();
                       }

                   }

                   PlatesAdapter adapter = new PlatesAdapter();
                   MainRecyclerView.setAdapter(adapter);

               }

               @Override
               public void onCancelled(@NonNull DatabaseError databaseError) {

               }
           });
       }
       catch (Exception e)
       {
           e.printStackTrace();
       }

    }


    public class PlatesAdapter extends RecyclerView.Adapter<PlatesAdapter.PlatesViewHolder>
    {

        @NonNull
        @Override
        public PlatesAdapter.PlatesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View plateview = LayoutInflater.from(getApplicationContext()).inflate(R.layout.plate_item_layout,parent,false);
            return new PlatesAdapter.PlatesViewHolder(plateview);
        }

        @Override
        public void onBindViewHolder(@NonNull final PlatesAdapter.PlatesViewHolder holder, int position) {

            final ImageView imageView = new ImageView(getApplicationContext());

            holder.Platepbar.setVisibility(View.VISIBLE);

            Glide.with(getApplicationContext()).load(PlateList.get(position).getUrl()).into(holder.PlateImageView);

            long time = Long.parseLong(PlateList.get(position).getTime());
            CharSequence Time = DateUtils.getRelativeDateTimeString(getApplication(), time, DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL);
            String timesubstring = Time.toString().substring(Time.length()-8);
            Date date = new Date(time);
            String dateformat = DateFormat.format("dd-MMM-yyyy",date).toString();
            holder.PlateDate.setText(dateformat+", "+timesubstring);

            Glide.with(getApplicationContext()).load(PlateList.get(position).getUrl()).into(holder.PlateImageView);

            Picasso.with(getApplicationContext()).load(PlateList.get(position).getUrl()).into(imageView, new Callback() {
                @Override
                public void onSuccess() {

                    BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
                    PlateBitmap= drawable.getBitmap();
                    holder.Platepbar.setVisibility(View.GONE);
                    if(PlateBitmap != null)
                    {
                        TextRecognizer recognizer = new TextRecognizer.Builder(getApplicationContext()).build();
                        if(!recognizer.isOperational())
                        {
                            Toast.makeText(MainActivity.this, "Doesnt support recognition.", Toast.LENGTH_SHORT).show();

                        }
                        else
                        {
                            Frame frame  = new Frame.Builder().setBitmap(PlateBitmap).build();
                            SparseArray<TextBlock> items = recognizer.detect(frame);
                            StringBuilder sb = new StringBuilder();


                            for(int i=0;i<items.size();i++)
                            {
                                if(sb.toString().toLowerCase().contains("k") || items.valueAt(i).getValue().toLowerCase().contains("k"))
                                {
                                    sb.append(items.valueAt(i).getValue());
                                }
                            }

                            String plate = sb.toString().replace("IND","");

                            holder.PlateNumber.setText("Plate : "+plate);

                        }



                    }

                }

                @Override
                public void onError() {

                }
            });



        }

        @Override
        public int getItemCount() {
            return PlateList.size();
        }

        public class PlatesViewHolder extends RecyclerView.ViewHolder {

            ImageView PlateImageView;
            TextView PlateNumber,PlateDate;
            ProgressBar Platepbar;

            public PlatesViewHolder(@NonNull View itemView) {
                super(itemView);

                PlateImageView = itemView.findViewById(R.id.plate_item_imageview);
                PlateNumber = itemView.findViewById(R.id.plate_item_number);
                PlateDate = itemView.findViewById(R.id.plate_item_date);
                Platepbar = itemView.findViewById(R.id.plate_item_pbar);
            }
        }
    }


}
