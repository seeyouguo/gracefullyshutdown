package com.guo.demo.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.CountDownLatch;

/**
 * 文件名称： ThreadTest
 * 包路径： com.kylin.demo.test
 * author：guoxiaoxu
 * 创建时间： 2019/10/14 - 11:47
 */
public class ThreadTest {

    public static void main(String[] args) throws Exception {
        System.out.println("main 开始执行");
        new Thread(()->{
            jdbcOperation();
//            System.out.println("执行测试线程");
//            try{
//                //其他操作
//                Thread.sleep(10000);
//                System.out.println("测试线程阻塞结束");
//            }catch (InterruptedException ex){
//                System.out.println("测试线程 interrupted");
//                ex.printStackTrace();
//            }
        }).start();
        System.out.println("main 方法继续执行");
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }

    private static void jdbcOperation(){
        System.out.println("jdbc operation start =====");
        for(int i = 0;i < 1000000; i++){
            volience();
        }
    }

    private static void volience(){
        System.out.println("jdbc operation start =====");
        String driver = "com.mysql.cj.jdbc.Driver";
        String url = "jdbc:mysql://127.0.0.1:3306/mbk_chariot";
        String user = "root";
        String password = "123456";
        String sql = "select * from mbk_upgrade_1002";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            //注册数据库驱动
            Class.forName(driver);
            //取得数据库连接
            conn = DriverManager.getConnection(url, user, password);
            //进行编译
            rs = pstmt.executeQuery();
            while (rs.next()) {
                System.out.println("get result ");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
