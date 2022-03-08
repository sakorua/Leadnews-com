package com.heima.test;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @作者 itcast
 * @创建日期 2021/4/15 21:43
 **/
public class StreamTest {

    public static void main(String[] args) {
        List<Student> studentList = getStudentList();
//        // 直接通过集合获取流
//        Stream<Student> stream = studentList.stream();
//        // 基于数组创建流
//        IntStream stream1 = Arrays.stream(new int[]{1, 2, 3, 4});
//        // 基于零散的数据创建流
//        Stream<Integer> integerStream = Stream.of(2, 3, 4, 5, 1, 234, 54, 1);
        // 创建流

        //   单词出现的次数:

        //  hello  boy

        //  welcome to itcast

//        Stream.of("hello  boy","welcome to itcast")
//              .flatMap(s -> {
//
//                  String[] s1 = s.split(" ");
//                  return Arrays.stream(s1);
//              }).forEach(System.out::println);
//
//        ;


//        String collect = studentList.stream()
//                // 定义中间操作
//                .filter((student) -> student.getSalary() > 10000)
//                // 去重
//                .distinct()
////                .map(new Function<Student, Stream<?>>() {
////                    @Override
////                    public Stream<?> apply(Student student) {
////                        // Student (姓名  工资)
////
////                        return Stream.of(student.getName(),student.getSalary());
////                    }
////                })
////                .skip(2)
////                .limit(2)
//                .sorted(Comparator.comparing(Student::getAge).reversed())
//                .map(student -> student.getName() + ":" + student.getSalary())
//                // 定义终止操作
////                .forEach(System.out::println); // 关羽:21321
//                // 关羽:21321,关羽:21321,关羽:21321,关羽:21321,关羽:21321
//                .collect(Collectors.joining(","));
//
//        System.out.println(collect);

//        Map<String, List<Student>> collect = studentList.stream()
//                // 定义中间操作
//                .filter((student) -> student.getSalary() > 10000)
//                // 去重
//                .distinct()
//                .sorted(Comparator.comparing(Student::getAge).reversed())
//                .collect(Collectors.groupingBy(Student::getSex));
//        System.out.println(collect);

        //[ {名称,金额},{名称,金额},{名称,金额},{名称,金额},{名称,金额}]

        Map<String, Double> collect = studentList.stream()
                // 定义中间操作
                .filter((student) -> student.getSalary() > 10000)
                // 去重
                .distinct()
                .sorted(Comparator.comparing(Student::getAge).reversed())
                .collect(Collectors.toMap(Student::getName, Student::getSalary));

        System.out.println(collect);
    }

    public static List<Student> getStudentList() {
        return Arrays.asList(
                new Student(1, "赵云", 28, "男", 18888.0),
                new Student(2, "孙尚香", 22, "女", 23145.0),
                new Student(3, "关羽", 31, "男", 21321.0),
                new Student(4, "貂蝉", 44, "女", 9000.0),
                new Student(5, "刘备", 51, "男", 54000.0),
                new Student(6, "甄姬", 25, "女", 16888.0),
                new Student(7, "曹操", 66, "男", 17888.0),
                new Student(8, "小乔", 34, "女", 28888.0),
                new Student(8, "小乔", 34, "女", 28888.0)
        );
    }

    static class Student {
        private Integer id;
        private String name;
        private Integer age;
        private String sex;
        private Double salary;

        public Student(Integer id, String name, Integer age, String sex, Double salary) {
            this.id = id;
            this.name = name;
            this.age = age;
            this.sex = sex;
            this.salary = salary;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Student student = (Student) o;
            return Objects.equals(id, student.id) && Objects.equals(name, student.name) && Objects.equals(age, student.age) && Objects.equals(sex, student.sex) && Objects.equals(salary, student.salary);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name, age, sex, salary);
        }

        @Override
        public String toString() {
            return "Student{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", age=" + age +
                    ", sex='" + sex + '\'' +
                    ", salary=" + salary +
                    '}';
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public String getSex() {
            return sex;
        }

        public void setSex(String sex) {
            this.sex = sex;
        }

        public Double getSalary() {
            return salary;
        }

        public void setSalary(Double salary) {
            this.salary = salary;
        }
    }
}
