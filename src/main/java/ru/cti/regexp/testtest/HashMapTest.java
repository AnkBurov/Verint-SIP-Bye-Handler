package ru.cti.regexp.testtest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by 134 on 28.02.2016.
 */
public class HashMapTest {
    public static void main(String[] args) {
        Map<Integer, String> map = new HashMap<>();
        map.put(1, "one");
        map.put(2, "two");
        System.out.println(map.toString());

        map.put(1, "ddd");
        System.out.println(map.toString());


        Set<TestObject> set = new HashSet<>();
        set.add(new TestObject(1, "one"));
        set.add(new TestObject(2, "two"));
        set.add(new TestObject(10, "ten"));
        for (TestObject testObject : set) {
            System.out.println(testObject);
        }

        if (set.add(new TestObject(1, "ddd"))) {
            System.out.println("Успех");
        } else {
            System.out.println("Неуспех");
        }
        for (TestObject testObject : set) {
            System.out.println(testObject);
        }
    }
}
