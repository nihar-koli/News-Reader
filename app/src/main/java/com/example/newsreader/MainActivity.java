package com.example.newsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {


    ArrayList<String> title = new ArrayList<String>();
    ArrayList<String> url = new ArrayList<String>();
    ArrayAdapter<String> arrayAdapter;
    ProgressBar progressBar;

    SQLiteDatabase articlesDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = findViewById(R.id.progressBar);

        articlesDB = this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);
        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY ,articleId INTEGER , title VARCHAR , articleUrl VARCHAR)");

        articlesDB.execSQL("DELETE FROM articles");
        ListView listView = findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,title);
        listView.setAdapter(arrayAdapter);

        updateListView();

        DownloadTask task = new DownloadTask();
        try {
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        }catch (Exception e){
            e.printStackTrace();
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getApplicationContext(),NewsActivity.class);
                intent.putExtra("url",url.get(i));
                startActivity(intent);
            }
        });
    }

    public void updateListView(){
        arrayAdapter.notifyDataSetChanged();

        Cursor c = articlesDB.rawQuery("SELECT * FROM articles",null);

        int titleIndex = c.getColumnIndex("title");
        int urlIndex = c.getColumnIndex("articleUrl");

        if(c.moveToFirst()){
            title.clear();
            url.clear();

            do{

                title.add(c.getString(titleIndex));
                url.add(c.getString(urlIndex));
            }while (c.moveToNext());

            arrayAdapter.notifyDataSetChanged();
        }
    }

    public class DownloadTask extends AsyncTask<String,Void,String>{
        @Override
        protected String doInBackground(String... strings) {
            try {
                String result="";
                URL url = new URL(strings[0]);
                HttpURLConnection httpURLConnection =(HttpURLConnection) url.openConnection();
                InputStream in = httpURLConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                int data =reader.read();

                while(data != -1){
                    char current = (char) data;
                    result += current;
                    data = reader.read();
                }

                JSONArray jsonArray = new JSONArray(result);
                int noOfItems = 20;
                if(jsonArray.length() < 20){
                    noOfItems = jsonArray.length();
                }

                Log.i("data",result);

                articlesDB.execSQL("DELETE FROM articles");

                for(int i = 0 ; i < noOfItems ; i++){
                    String articleId = jsonArray.getString(i);

                    Log.i("articleId",articleId);

                    String articleInfo = "";

                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" +articleId+ ".json?print=pretty");
                    httpURLConnection =(HttpURLConnection) url.openConnection();

                    in = httpURLConnection.getInputStream();
                    reader = new InputStreamReader(in);
                    data =reader.read();

                    while(data != -1){
                        char current = (char) data;
                        articleInfo += current;
                        data = reader.read();
                    }

                    //Log.i("articleInfo",articleInfo);

                    JSONObject jsonObject = new JSONObject(articleInfo);

                    if(!jsonObject.isNull("title") && !jsonObject.isNull("url")){
                        String articleTitle = jsonObject.getString("title");
                        String articleUrl = jsonObject.getString("url");

                        Log.i("article Title",articleTitle);
                        Log.i("article Url",articleUrl);

                        String sql = "INSERT INTO articles (articleID , title , articleUrl) VALUES (? , ? , ?)";
                        SQLiteStatement statement = articlesDB.compileStatement(sql);
                        statement.bindString(1,articleId);
                        statement.bindString(2,articleTitle);
                        statement.bindString(3,articleUrl);

                        statement.execute();
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            progressBar.setVisibility(View.GONE);
            updateListView();
        }
    }
}
