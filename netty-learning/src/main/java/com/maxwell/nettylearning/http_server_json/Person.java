package com.maxwell.nettylearning.http_server_json;

/************************************************************************************
 * 功能描述：
 * 创建人：岳增存  yueyuemax@gmail.com
 * 创建时间： 2018年02月04日 --  下午3:52 
 * 其他说明：
 * 修改时间：
 * 修改人：
 *************************************************************************************/
public class Person {

    private String name;
    private int age;
    private String address;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return this.age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getAddress() {
        return this.address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
