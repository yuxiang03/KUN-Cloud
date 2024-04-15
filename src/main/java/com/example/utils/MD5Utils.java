package com.example.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Utils {
        public static String encrypt(String input) {
            try {
                // 创建MessageDigest对象，指定使用MD5算法
                MessageDigest md = MessageDigest.getInstance("MD5");

                // 将输入字符串转换为字节数组
                byte[] inputBytes = input.getBytes();

                // 对字节数组进行加密
                byte[] encryptedBytes = md.digest(inputBytes);

                // 将加密后的字节数组转换为十六进制字符串
                StringBuilder sb = new StringBuilder();
                for (byte b : encryptedBytes) {
                    sb.append(String.format("%02x", b));
                }

                return sb.toString();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

            return null;
        }
}
