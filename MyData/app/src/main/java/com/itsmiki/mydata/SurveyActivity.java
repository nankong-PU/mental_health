package com.itsmiki.mydata;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class SurveyActivity extends AppCompatActivity {
    private final String TAG = "SurveyActivity";
    private ProgressBar progBar;
    private int progress = 0;
    private TextView textView;
    int[] values;
    int[] questionIds;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey);
        progBar = findViewById(R.id.progressBar);
        textView = findViewById(R.id.questionTV);
        progBar.setProgress(0);
        textView.setText(R.string.q1);
        values = new int[8];
        questionIds = new int[]{R.string.q1,R.string.q2,R.string.q3,R.string.q4,R.string.q5,R.string.q6,R.string.q7,R.string.q8};
    }
    public void ans0L(View view){
        values[progress] = 0;
        if(progress == 7) {
            disable();
            write();
        } else {
            textView.setText(questionIds[progress]);
            progBar.setProgress(progress+1);
            progress++;
        }
    }
    public void ans1L(View view){
        values[progress] = 1;
        if(progress == 7) {
            disable();
            write();
        } else {
            textView.setText(questionIds[progress]);
            progBar.setProgress(progress+1);
            progress++;
        }
    }
    public void ans2L(View view){
        values[progress] = 2;
        if(progress == 7) {
            disable();
            write();
        } else {
            textView.setText(questionIds[progress]);
            progBar.setProgress(progress+1);
            progress++;
        }
    }
    public void ans3L(View view){
        values[progress] = 3;
        if(progress == 7) {
            disable();
            write();
        } else {
            textView.setText(questionIds[progress]);
            progBar.setProgress(progress+1);
            progress++;
        }
    }
    private void write(){
        Toast.makeText(this,"end reached",Toast.LENGTH_SHORT).show();
        String str = DateFormat.format("dd-MM-yyyy hh:mm:ss", new java.util.Date()).toString()+":";
        for(int i = 0; i < 7; i++){
            str += i+","+values[i]+" ";
        }
        str += 7+","+values[7]+" ";
        writeFile(str);
        finish();
    }
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
    public void writeFile(String input) {
        if (isExternalStorageWritable()) {
            File file = new File(getExternalFilesDir(null), "survey.txt");
            try {
                Log.d(TAG, file.getAbsolutePath());
                Log.d(TAG, input);
                if (!file.exists()) {
                    file.createNewFile();
                    Log.d(TAG, "trying to create file");
                }
                FileOutputStream fos = new FileOutputStream(file, true);
                fos.write((input+"\n").getBytes());
                fos.flush();
                fos.close();
                Toast.makeText(this, "file saved to: " + getExternalFilesDir(null), Toast.LENGTH_SHORT).show();
                if (fos != null) {
                    fos.close();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
    public void disable(){
        findViewById(R.id.not).setEnabled(false);
        findViewById(R.id.several).setEnabled(false);
        findViewById(R.id.more).setEnabled(false);
        findViewById(R.id.nearly).setEnabled(false);
    }
}
