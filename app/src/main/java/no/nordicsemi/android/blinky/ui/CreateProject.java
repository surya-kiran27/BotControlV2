package no.nordicsemi.android.blinky.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import no.nordicsemi.android.blinky.R;

public class CreateProject extends AppCompatActivity {
    private Button button;
    private Spinner spinner;
    private EditText editText;
    private String selected;
    ArrayAdapter<String> adapter;
    SharedPreferences sharedpreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_project);
        sharedpreferences = getSharedPreferences("settings", Context.MODE_PRIVATE);
        button=findViewById(R.id.next);
        spinner=findViewById(R.id.spinner);
        editText=findViewById(R.id.carNumber);
        String[] items = new String[]{"36", "24", "18","12"};
        selected="36";
        adapter  = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // your code here
                selected=adapter.getItem(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here

            }

        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String carNumber="";

                if (sharedpreferences.contains("carNumber")){
                    carNumber=sharedpreferences.getString("carNumber","");
                }else{
                    carNumber=editText.getText().toString();
                }
                if(carNumber.length()==0){
                    showMessage("Please enter a valid car number");
                    return;
                }
                     Intent i = new Intent(getApplicationContext(), CameraSettings.class);
                     i.putExtra("carNumber",carNumber);
                     i.putExtra("noImage",selected);
                     i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                     getApplication().startActivity(i);
                     finish();
            }
        });
    }
    private void showMessage(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

}