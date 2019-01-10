/*
 * Copyright (c) 2019. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package net.renren.common;


import android.text.TextUtils;

import com.hbsdk.annotations.HbApiName;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Created by GCTang on 2019/1/4.
 * Description: 适用当前业务，不要用于其他用途
 */

public class HsonUtil {

    private static String[] list2 = {"wi1"};
    private static final String LIST2_TAG = "java.util.List<";

    public static <T> T toBean(String json, final Class<T> clazz){
        Object target = newInstance(clazz);
        try{
            if(TextUtils.isEmpty(json)){
                return (T)target;
            }
            Class<?> tempClazz = clazz;
            List<Field> fieldList = new ArrayList<>() ;
            while (tempClazz != null) {
                fieldList.addAll(Arrays.asList(tempClazz .getDeclaredFields()));
                tempClazz = tempClazz.getSuperclass();
            }

            Field[] fields = new Field[fieldList.size()];
            fieldList.toArray(fields);
            JSONObject jsonObj = new JSONObject(json);
            for(int i = 0 , len = fields.length; i < len; i++) {
                if (fields[i].isAnnotationPresent(HbApiName.class)) {
                    String jsonName = fields[i].getAnnotation(HbApiName.class).value();
                    String varType = fields[i].getType().getSimpleName();
                    if(isPrimitive(varType)){
                        setValue(target,fields[i],getValue(jsonObj,varType,jsonName));
                    }else if(List.class.getSimpleName().equals(varType)){
                        ParameterizedType pType = null;
                        Object actualTypeClazz;
                        try{
                            pType = (ParameterizedType) fields[i].getGenericType();
                            actualTypeClazz =  pType.getActualTypeArguments()[0];
                            if(Arrays.asList(list2).contains(jsonName)){
                                setValue(target,fields[i],to2Beans(jsonObj.optString(jsonName),actualTypeClazz));
                            }else{
                                setValue(target,fields[i],toBeans(jsonObj.optString(jsonName),actualTypeClazz));
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }else{
                        setValue(target,fields[i],toBean(jsonObj.optString(jsonName),fields[i].getType()));
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return (T)target;
    }

    //此方法太搓，待以后优化
    private static <T> List<List<T>> to2Beans(String json, final Object clazz){
        List<List<T>> rlts = new ArrayList<List<T>>();
        List<T> tRlts = toBeans(json,clazz);
        rlts.add(tRlts);
        return rlts;
    }

    public static <T> List<T> toBeans(String json, final Object clazz){
        List<T> rlts = new ArrayList<>();
        try {
            if(TextUtils.isEmpty(json)){
                return rlts;
            }
            JSONArray jsonArry = new JSONArray(json);
            int size = jsonArry.length();
            Object jsonTokener;
            String jsonStr;
            for(int i = 0 ; i < size ; i++){
                jsonStr = jsonArry.opt(i).toString();
                jsonTokener = new JSONTokener(jsonStr).nextValue();
                if(jsonTokener instanceof JSONObject) {
                    rlts.add(toBean(jsonStr,(Class<T>)clazz));
                }else if(jsonTokener instanceof JSONArray){
                    String clazzName = clazz.toString().replace(LIST2_TAG,"");
                    clazzName = clazzName.substring(0,clazzName.length() - 1);
                    return toBeans(jsonStr,Class.forName(clazzName));
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return rlts;
    }


    private static Object getValue(JSONObject jsonObj,String varType,String key){
        try {
            if (String.class.getSimpleName().equals(varType)) {
                return String.valueOf(jsonObj.opt(key));
            } else if (int.class.getSimpleName().equals(varType)) {
                return Integer.valueOf(jsonObj.opt(key).toString());
            } else if (long.class.getSimpleName().equals(varType)) {
                return Long.valueOf(jsonObj.opt(key).toString());
            } else if (double.class.getSimpleName().equals(varType)) {
                return Double.valueOf(jsonObj.opt(key).toString());
            } else if (float.class.getSimpleName().equals(varType)) {
                return Float.valueOf(jsonObj.opt(key).toString());
            } else if (char.class.getSimpleName().equals(varType)) {
                return jsonObj.opt(key);
            } else if (byte.class.getSimpleName().equals(varType)) {
                return Byte.valueOf(jsonObj.opt(key).toString());
            } else if (short.class.getSimpleName().equals(varType)) {
                return Short.valueOf(jsonObj.opt(key).toString());
            } else if (boolean.class.getSimpleName().equals(varType)) {
                return Boolean.valueOf(jsonObj.opt(key).toString());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return jsonObj.opt(key);
    }

    private static void setValue(Object instance,Field field,Object value){
        try {
            boolean accessFlag = field.isAccessible();
            field.setAccessible(true);
            field.set(instance, value);
            field.setAccessible(accessFlag);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private static boolean isPrimitive(String varType){
        if(String.class.getSimpleName().equals(varType) ||
                int.class.getSimpleName().equals(varType) ||
                long.class.getSimpleName().equals(varType) ||
                double.class.getSimpleName().equals(varType) ||
                float.class.getSimpleName().equals(varType) ||
                char.class.getSimpleName().equals(varType) ||
                byte.class.getSimpleName().equals(varType) ||
                short.class.getSimpleName().equals(varType) ||
                boolean.class.getSimpleName().equals(varType)){
            return true;
        }
        return false;
    }

    private static Object newInstance(Class<?> clazz){
        Object instance = null;
        try {
            Constructor<?> cons = clazz.getConstructor();
            instance = cons.newInstance();
        } catch (NoSuchMethodException e){
            e.printStackTrace();
        }catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return instance;
    }



    private static JSONArray toArryJson(Object obj) {

        JSONArray rltArr = new JSONArray();
        if (obj instanceof List) {
            List list = (List) obj;
            int size = list.size();
            for (int i = 0; i < size; i++) {
                try {
                    if(list.get(i) instanceof List){
                        toArryJson(obj);
                    }
                    rltArr.put(i, toJson(list.get(i)));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return rltArr;
    }

    public static JSONObject toJson(Object obj){
        JSONObject rlt = new JSONObject();
        Class<?> tempClazz = obj.getClass();
        List<Field> fieldList = new ArrayList<>() ;
        while (tempClazz != null) {
            fieldList.addAll(Arrays.asList(tempClazz .getDeclaredFields()));
            tempClazz = tempClazz.getSuperclass();
        }
        Field[] fields = new Field[fieldList.size()];
        fieldList.toArray(fields);
        for(int i = 0 , len = fields.length; i < len; i++) {
            if(fields[i].isAnnotationPresent(HbApiName.class)){
                String varName = fields[i].getAnnotation(HbApiName.class).value();
                String varType = fields[i].getType().getSimpleName();
                try {
                    boolean accessFlag = fields[i].isAccessible();
                    fields[i].setAccessible(true);
                    Object o;
                    try {
                        o = fields[i].get(obj);
                        if(isPrimitive(varType)){
                            rlt.put(varName,o);
                        }else if(List.class.getSimpleName().equals(varType)){
                            rlt.put(varName,toArryJson(o));
                        }else{
                            rlt.put(varName,toJson(o));
                        }
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    fields[i].setAccessible(accessFlag);
                } catch (IllegalArgumentException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return rlt;
    }

}
