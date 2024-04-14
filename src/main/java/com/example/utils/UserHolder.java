package com.example.utils;

import com.example.entity.po.UserInfo;

public class UserHolder {
    private static final ThreadLocal<UserInfo> tl = new ThreadLocal<>();

    public static void saveUser(UserInfo user){
        tl.set(user);
    }

    public static UserInfo getUser(){
        return tl.get();
    }

    public static void removeUser(){
        tl.remove();
    }
}
