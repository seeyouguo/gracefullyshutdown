package com.kylin;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.io.*;
import java.util.*;

/**
 * 文件名称： LogTest
 * 包路径： com.kylin
 * 类描述：
 * author：guoxiaoxu
 * 创建时间： 2019/11/08 - 14:27
 * 修改记录：
 */
public class LogTest {

    @Test
    public void outTest() {
        List<String> resultList = new ArrayList<>();
        List<String> userIds = readLinesFromFile("/Users/guoxiaoxu/Downloads/userId.log");

        File file = new File("/Users/guoxiaoxu/Downloads/out.log");
        if (file != null) {
            try {
                BufferedReader buf = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                String line = null;
                while ((line = buf.readLine()) != null) {
                    if (StringUtils.isNoneBlank(line)) {
                        //-2019-11-04 00:00:00.002 ,3734418610224572708864839368, longitude:122.998671, latitude:41.11413, speed:0.0
                        String b = StringUtils.substringBefore(line, "anshan") + ",";
                        String c = StringUtils.substringAfter(line, "= ");
                        String[] arr = StringUtils.split(b+c, ",");
                        String userId = arr[1].trim();
                        if(userIds.contains(userId)){
                            StringBuilder sb = new StringBuilder();
                            String time = arr[0].trim().substring(1);
                            String longitude = arr[2].split(":")[1];
                            String latitude = arr[3].split(":")[1];
                            sb.append(time).append(";").append(userId).append(";").append(longitude).append(",").append(latitude);
                            resultList.add(sb.toString());
                        }
                    }
                }
                buf.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }


        try{
            File writename = new File("/Users/guoxiaoxu/Downloads/request/request"); // 相对路径，如果没有则要建立一个新的output。txt文件
            writename.createNewFile(); // 创建新文件
            BufferedWriter out = new BufferedWriter(new FileWriter(writename));
            resultList.stream().forEach(s->{
                try{
                    out.write(s + "\r\n"); // \r\n即为换行
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            });
            out.flush(); // 把缓存区内容压入文件
            out.close(); // 最后记得关闭文件
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }


    public static List<String> readLinesFromFile(String uri) {
        File file = new File(uri);
        if (file != null) {
            List<String> lines = new ArrayList<>();
            try {
                BufferedReader buf = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                String line = null;
                while ((line = buf.readLine()) != null) {
                    if (StringUtils.isNoneBlank(line)) lines.add(line.trim());
                }
                buf.close();
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
            return lines;
        } else return null;
    }


    @Test
    public void test01(){
        String a = "-2019-11-04 00:00:00.002 anshan taxi-nfs 1 INFO -  [1002,,] --- [http-nio-8847-exec-17] c.m.t.n.c.DriverServiceController        : driver report, userId = 3734418610224572708864839368, longitude:122.998671, latitude:41.11413, speed:0.0 ";
        String b = StringUtils.substringBefore(a, "anshan") + ",";
        String c = StringUtils.substringAfter(a, "= ");
        String d = "-2019-11-04 00:00:00.002".substring(1);
        System.out.println(d);
    }
}
