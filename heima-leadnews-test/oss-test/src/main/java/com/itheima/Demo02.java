package com.itheima;

import java.util.Arrays;

/**
 * @author TaoHongQiang
 * @date Created  2022/3/2 10:52
 * @Description
 */
public class Demo02 {
    public static void main(String[] args) {
        int[] arr = {1, 2, 3, 4, 5, 6, 7, 8};
        Arrays.stream(arr)
                .filter((a) -> a != 1)
                .skip(2)
                .limit(3)
                .forEach(System.out::println);
    }
}
