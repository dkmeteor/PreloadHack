package com.dk_exp.preloadhack;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.LongSparseArray;
import android.util.TypedValue;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.lang.reflect.Field;

public class MainActivity extends AppCompatActivity {

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });



        fab.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkPreload();
            }
        },1000);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void checkPreload(){

        Resources resource = getApplicationContext().getResources();

        Field field = null;
        try {
            field = Resources.class.getDeclaredField("sPreloadedDrawables");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        field.setAccessible(true);

        LongSparseArray<Drawable.ConstantState>[]    sPreloadedDrawables = null;
        try {
            sPreloadedDrawables = (LongSparseArray<Drawable.ConstantState>[] )field.get(resource);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        LongSparseArray<Drawable.ConstantState> l = sPreloadedDrawables[0];


        TypedValue value = new TypedValue();
        resource.getValue(R.drawable.charming,value,true );


        long  key = -1;
        if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT
                && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
//            isColorDrawable = true;
//            caches = mColorDrawableCache;
            key = value.data;
        } else {
//            isColorDrawable = false;
//            caches = mDrawableCache;
            key = (((long) value.assetCookie) << 32) | value.data;
        }

//        key = (((long) 1) << 32) | value.data;

        Drawable drawable  = resource.getDrawable(R.drawable.charming, null);

        Drawable.ConstantState cs = l.get(key);
        System.out.println(cs);
    }
}
