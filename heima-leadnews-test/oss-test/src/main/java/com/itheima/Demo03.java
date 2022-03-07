package com.itheima;

import com.itheima.pojo.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author TaoHongQiang
 * @date Created  2022/3/3 14:20
 * @Description Steam流
 */
public class Demo03 {
    public static void main(String[] args) {
//        //创建stream流的方式
//        //1 通过Collection系列提供的stream 串行或者 parallelStream 并行获取
//        ArrayList<String> list = new ArrayList<>();
//        //串行流
//        Stream<String> stream = list.stream();
//        //并行流
//        Stream<String> stringStream = list.parallelStream();

        List<User> list = new ArrayList<>();

        User user=new User(1,"a","小明");
        User user1=new User(2,"a","小红");
        User user2=new User(3,"b","小李");

        list.add(user);
        list.add(user1);
        list.add(user2);


//        Map<String, List<User>> collect = list.stream().collect(Collectors.groupingBy(User::getType));
//        System.out.println("collect = " + collect);
        Map<String, Long> collect = list.stream().collect(Collectors.groupingBy(User::getType, Collectors.counting()));
        System.out.println("collect = " + collect);
    }
}
