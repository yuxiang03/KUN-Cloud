package com.example.utils;

<<<<<<< HEAD
import com.example.entity.dto.UserDTO;

public class UserHolder {
    private static final ThreadLocal<UserDTO> tl = new ThreadLocal<>();
=======
import com.example.entity.po.UserInfo;
>>>>>>> 59b93a7cd221e063e4ec15b94853a76ca185bdee

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
