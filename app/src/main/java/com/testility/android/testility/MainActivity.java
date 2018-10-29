package com.testility.android.testility;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.OpenableColumns;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    public static final int PICK_IMAGE = 1;
    String mFileName = null;
    String mFilePath = null;
    EditText editText;

    /** URL for JIRA Request data from the JIRA API */
    private static final String JIRA_REQUEST_URL = "http://jira.hm.com/rest/api/2/issue/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main1);

        editText = findViewById(R.id.editText);
        editText.setText("HMCOM-",TextView.BufferType.EDITABLE);
        editText.setSelection(6);

        Button attach = findViewById(R.id.attachButton);
        attach.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openImage();
            }
    });

        Button show = findViewById(R.id.showButton);
        show.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // updateTitle();
                JiraApiRequest jiraApiRequestURL= new JiraApiRequest();
                String jiraIssue = editText.getText().toString();
                jiraApiRequestURL.execute(JIRA_REQUEST_URL+jiraIssue);

            }
        });

}

    private void updateTitle(String title){
    TextView titleTextView = (TextView) findViewById(R.id.titletextView);
    titleTextView.setText(title);
}

    private void openImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_PICK);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE && resultCode == RESULT_OK
                && data != null && data.getData() != null )
        {
        Uri returnUri = data.getData();
        String mimeType = getContentResolver().getType(returnUri);

            Cursor returnCursor = getContentResolver().query(returnUri, null, null, null, null);
            int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
            returnCursor.moveToFirst();

            String fileName = returnCursor.getString(nameIndex);
            mFileName = fileName;
            mFilePath = FileUtils.getPath(this, returnUri);

           JiraUploadRequest jiraUploadRequest = new JiraUploadRequest();
           String jiraIssue = editText.getText().toString();
           jiraUploadRequest.execute(JIRA_REQUEST_URL+jiraIssue+"/attachments");
        }
    }

    private class JiraApiRequest extends AsyncTask <String, Void, String>{
        @Override
        protected String doInBackground(String... urls) {
            if (urls.length < 1 || urls[0] == null) {
                return null;
            }
            @SuppressLint("WrongConstant") SharedPreferences sh1 = getSharedPreferences("com.testility.android.testility.userData", MODE_APPEND);
            String username = sh1.getString("username","");
            String password = sh1.getString("password","");

            String title = Utils.fetchJiraAPIResponse(urls[0],username,password);
                   return title;
        }

        @Override
        protected void onPostExecute(String titleText) {
            super.onPostExecute(titleText);
            if (titleText ==null){
                return;
            }
            updateTitle(titleText);
        }
    }

    private class JiraUploadRequest extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            String msgCode = null;
            @SuppressLint("WrongConstant") SharedPreferences sh1 = getSharedPreferences("com.testility.android.testility.userData", MODE_APPEND);
            String username = sh1.getString("username","");
            String password = sh1.getString("password","");
            try {
                msgCode = Utils.uploadToJira(urls[0],username,password, mFilePath, mFileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return msgCode;
        }

        @Override
        protected void onPostExecute(String msgCode) {
                if(msgCode.equals("")) {
                Toast.makeText(MainActivity.this, "Image Uploaded successfully ", Toast.LENGTH_SHORT).show();
            }
            else Toast.makeText(MainActivity.this, "Image Upload Failed Check if you have connected to the HM VPN ", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.option_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menuLogin:
        }
        return true;
    }

}
