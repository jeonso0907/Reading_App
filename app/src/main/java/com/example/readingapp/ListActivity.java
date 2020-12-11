package com.example.readingapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Map;

public class ListActivity extends AppCompatActivity {

    FloatingActionButton addImage;
    ArrayAdapter adapter;
    ListView readingList;
    FirebaseFirestore db;
    ArrayList<String> readingArray = new ArrayList<>();
    String title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reading_list);

        addImage = (FloatingActionButton) findViewById(R.id.addImage);
        readingList = (ListView) findViewById(R.id.readingList);

        addImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }

        });

        db = FirebaseFirestore.getInstance();

        db.collection("readings")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            int i = 0;
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d("DB " + i, document.getData().toString());
                                Map<String, Object> m = document.getData();
                                readingArray.add(i, textOnly(m.values().toString()));
                                i++;
                            }
                            readingListSet(readingArray);
                        } else {
                            // Failed
                        }

                    }
                });

        Intent intent = getIntent();
        if (intent.hasExtra("Title")) {
            title = intent.getStringExtra("Title");
        }



        final DocumentReference docRef = db.collection("readings").document("title");
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    // Failed
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    String text = snapshot.getData().get("Text").toString();
                    readingArray.add(readingArray.size(), text);
                    readingListSet(readingArray);
                } else {
                    // Null
                }
            }
        });


        // Make the list clickable
        readingList.setClickable(true);

        readingList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String courseName = (readingList.getItemAtPosition(position)).toString();
            }
        });
    }

    public void readingListSet(ArrayList<String> readings) {
        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1,
                readingArray);

        // Display the course name in the list view
        readingList.setAdapter(adapter);
    }

    public static String textOnly(String text) {

        String newText = text.substring(1, text.length() - 1);

        return newText;
    }

}
