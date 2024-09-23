package com.example.utils;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class FileEncryption {
    public static void main(String[] args) throws Exception {
        String originalImagePath = "/Users/yuxiang/Downloads/设计模式.pdf";
        String encryptedImagePath = "/Users/yuxiang/Downloads/s.txt";
        String decryptedImagePath = "/Users/yuxiang/Downloads/k.pdf";
        String key = "WxappEncrypteKey"; // 密钥，必须是16个字符长度的字符串
        try {
            // 读取原始文件
            byte[] originalImageBytes = Files.readAllBytes(Paths.get(originalImagePath));
            // 加密图片
            byte[] encryptedImageBytes = encrypt(originalImageBytes, key);
            // 将加密后的图片保存到文件
            Files.write(Paths.get(encryptedImagePath), encryptedImageBytes);
            // 读取加密后的图片
            byte[] encryptedImageBytesFromFile = Files.readAllBytes(Paths.get(encryptedImagePath));
            // 解密图片
            byte[] decryptedImageBytes = decrypt(encryptedImageBytesFromFile, key);
            // 将解密后的图片保存到文件
            Files.write(Paths.get(decryptedImagePath), decryptedImageBytes);
            System.out.println("图片加密解密完成！");
        } catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException |
                InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
    }

    private static byte[] encrypt(byte[] input, String key) throws Exception {
        SecretKey secretKey = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(input);
    }

    private static byte[] decrypt(byte[] input, String key) throws Exception {
        SecretKey secretKey = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        try {
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return cipher.doFinal(input);
    }
}
