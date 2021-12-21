package com.ice.annotationtest;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ice.annotation.lib_interface.BindView;
import com.ice.annotation.lib_interface.Unbinder;
import com.ice.annotationlib_2.BindingView;

public class OneFragment extends Fragment {

    @BindView(R.id.tv_1)
    TextView tv;

    Unbinder unbinder;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_one, container, false);
        unbinder = BindingView.init(this, view);

        tv.setText("OneFragment getDecorView");
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
        Log.d("unbind", "onDestroyView");
    }
}
