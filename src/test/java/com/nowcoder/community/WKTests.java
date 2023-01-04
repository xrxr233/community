package com.nowcoder.community;

import java.io.IOException;

public class WKTests {
    public static void main(String[] args) {
        //本地命令
        String cmd = "E:/develop/wkhtmltopdf/bin/wkhtmltoimage --quality 75 www.baidu.com E:/develop/wkhtmltopdf/wk-image/baidu.png";
        try {
            Runtime.getRuntime().exec(cmd);  //操作系统执行，与当前主程序并发执行
            System.out.println("ok");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
