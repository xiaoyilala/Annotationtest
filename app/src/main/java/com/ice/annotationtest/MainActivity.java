package com.ice.annotationtest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.ice.annotation.lib_interface.BindView;
import com.ice.annotation.lib_interface.Unbinder;
import com.ice.annotationlib_2.BindingView;

//import com.ice.annotationlib_1.BindView;
//import com.ice.annotationlib_1.BindingView;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.tv)
    TextView tv;

    @BindView(R.id.bt)
    Button bt;

    Unbinder unbinder;

    boolean isResumed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        unbinder = BindingView.init(this, getWindow().getDecorView());
        OneFragment oneFragment = new OneFragment();
        getSupportFragmentManager().beginTransaction().add(R.id.fl, oneFragment).commitAllowingStateLoss();

        com.ice.annotationtest.test.OneFragment oneFragment2 = new com.ice.annotationtest.test.OneFragment ();
        getSupportFragmentManager().beginTransaction().add(R.id.fl2, oneFragment2).commitAllowingStateLoss();

        tv.setText("注解getDecorView");

        bt.setOnClickListener(v->{
            Intent intent = new Intent(MainActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(isResumed) {
            tv.setText("onResume");
        }
        isResumed = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
        Log.d("unbind", "onDestroy");
    }
}