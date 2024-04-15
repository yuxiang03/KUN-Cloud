package com.example.utils;

import java.util.Random;

public class CreateCodeUtils {

    public static String creatCode(int n) {
        String code = "";
        Random r = new Random();
        for (int i = 0; i < n; i++) {
            int type = r.nextInt(3)
            switch (type) {
                case 0:
                    char ch = (char) (r.nextInt(26) + 65);
                    code += ch;
                    break;
                case 1:
                    char ch1 = (char) (r.nextInt(26) + 97);
                    code += ch1;
                    break;
                case 2:
                    code += r.nextInt(10);
                    break;
            }
        }
        return code;
    }

}
