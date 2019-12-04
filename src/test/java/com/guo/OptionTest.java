package com.kylin;

import jdk.nashorn.internal.objects.annotations.Getter;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

/**
 * 文件名称： OptionTeset
 * 包路径： com.kylin
 * 类描述：
 * author：guoxiaoxu
 * 创建时间： 2019/11/07 - 14:14
 * 修改记录：
 */
public class OptionTest {

    @Test
    public void test01(){
        try{
//            getCity(null);
            User user1 = new User();
            User user2 = Optional.ofNullable(user1).orElse(new User());
            User user3 = Optional.ofNullable(user1).orElseGet(()->new User());
            Assert.assertTrue(true);
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public String getCity(User user) throws Exception{
        return Optional.ofNullable(user)
                .map(u-> u.getAddress())
                .map(a->a.getCity())
                .orElseThrow(()->new Exception("取指错误"));
    }

}

class User{
    private Address address;

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }
}

class Address{
    private String city;

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
