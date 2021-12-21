package com.ice.annotationlib_2;

import android.app.Activity;
import android.os.strictmode.WebViewMethodCalledOnWrongThreadViolation;
import android.view.View;

import androidx.fragment.app.Fragment;

import com.ice.annotation.lib_interface.Unbinder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public class BindingView {
    public static Unbinder init(Activity activity, View view){
        try {
            Class aClass = Class.forName(activity.getClass().getCanonicalName() + "ViewBinding");
            Constructor constructor = aClass.getDeclaredConstructor(activity.getClass(), View.class);
            return (Unbinder) constructor.newInstance(activity, view);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return Unbinder.EMPTY;
    }

    public static Unbinder init(Fragment fragment, View view){
        try {
            Class aClass = Class.forName(fragment.getClass().getCanonicalName() + "ViewBinding");
            Constructor constructor = aClass.getDeclaredConstructor(fragment.getClass(), View.class);
            return (Unbinder) constructor.newInstance(fragment, view);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return Unbinder.EMPTY;
    }
}
