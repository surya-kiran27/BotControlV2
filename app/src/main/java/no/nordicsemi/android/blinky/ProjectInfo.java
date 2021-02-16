package no.nordicsemi.android.blinky;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import no.nordicsemi.android.blinky.ui.CreateProject;

public class ProjectInfo extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_info);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Button button = findViewById(R.id.update);
        TextView textView = findViewById(R.id.imageUrl);


        button.setOnClickListener(v -> {
            //create project
            Intent i = new Intent(getApplicationContext(), CreateProject.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getApplication().startActivity(i);
            finish();
        });
        Intent i = getIntent();
        String url = i.getStringExtra("url");
        textView.setText(url);
        Linkify.addLinks(textView, Linkify.WEB_URLS);

    }
}