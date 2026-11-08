package uws.ac.uk.bookmark;


import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class CurrentReading extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current);

        //Connect to the Recycler View in activity_current.xml
        RecyclerView recyclerView = findViewById(R.id.currentBooksRecyclerView);

        //set the list to display book vertically
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        //create a temporary list of books
        List <CurrentBook> bookList = new ArrayList<>();

        //add sample books to test the screen
        bookList.add(new CurrentBook("The Cruel Prince", "Holly Black", "", 45));
        bookList.add(new CurrentBook("It Ends With Us", "Colleen Hoover", "", 70));

        //connect the adapter to the RecylcerView
        CurrentBooksAdapter adapter = new CurrentBooksAdapter(bookList);
        recyclerView.setAdapter(adapter);
    }
}