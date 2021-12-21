package com.ice.annotationlib_1;

import android.app.Activity;
import java.lang.reflect.Field;

public class BindingView {
    public static void init(Activity activity){
        Field[] fields = activity.getClass().getDeclaredFields();
        for(Field field:fields){
            BindView annotation = field.getAnnotation(BindView.class);
            if(annotation!=null){
                int viewId = annotation.value();
                field.setAccessible(true);
                try {
                    field.set(activity, activity.findViewById(viewId));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
