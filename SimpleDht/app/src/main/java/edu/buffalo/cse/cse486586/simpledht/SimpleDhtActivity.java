package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class SimpleDhtActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_dht_main);

        TextView tv = (TextView) findViewById(R.id.textView1);

        tv.setMovementMethod(new ScrollingMovementMethod());
        findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("testing","plsss");
                ContentResolver mContentResolver = getContentResolver();
                Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider");
                Cursor resultCursor = mContentResolver.query(mUri, null,
                        "@", null, null);
                if (resultCursor == null) {
                    Log.e("ldump", "Result null");
                }else{
                    Log.e("testing", ""+resultCursor.getCount());
                }
                //int count = mContentResolver.delete(mUri, "@", null);
            }
        });
        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("testing2","sssssss");
                ContentResolver mContentResolver = getContentResolver();
                Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider");
                Cursor resultCursor = mContentResolver.query(mUri, null,
                        "*", null, null);
                if (resultCursor == null) {
                    Log.e("ldump", "Result null");
                }else{
                    Log.e("testing2", ""+resultCursor.getCount());
                }
               // int count = mContentResolver.delete(mUri, "*", null);
            }
        });

        findViewById(R.id.button3).setOnClickListener(
                new OnTestClickListener(tv, getContentResolver()));




    }
    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_simple_dht_main, menu);
        return true;
    }

}
