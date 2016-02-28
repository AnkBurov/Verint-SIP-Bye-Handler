package ru.cti.regexp.testtest;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

import java.io.File;
import java.util.Iterator;
import java.util.concurrent.ConcurrentNavigableMap;

/**
 * Created by 134 on 28.02.2016.
 */
public class MainTest {
    public static void main(String[] args) {
        // import org.mapdb.*;

// configure and open database using builder pattern.
// all options are available with code auto-completion.
        DB db = DBMaker.fileDB(new File("testdb"))
                .closeOnJvmShutdown()
                .encryptionEnable("password")
                .make();

// open existing an collection (or create new)
        ConcurrentNavigableMap<Integer, String> map = db.treeMap("collectionName");

        map.put(1, "one");
        map.put(2, "two");
// map.keySet() is now [1,2]

        db.commit();  //persist changes into disk

        map.put(3, "three");
// map.keySet() is now [1,2,3]

        db.rollback(); //revert recent changes
// map.keySet() is now [1,2]

        map.putIfAbsent(3, "adf");
        db.commit();

        System.out.println(map.toString());
        db.close();

        DB db1 = DBMaker.fileDB(new File("testremove")).closeOnJvmShutdown().make();
        HTreeMap hTreeMap = db1.hashMapCreate("hashMap")
                .keySerializer(Serializer.STRING)
                .valueSerializer(Serializer.STRING)
                .makeOrGet();
        hTreeMap.putIfAbsent("2", "apple");
        db1.commit();

        System.out.println(hTreeMap.toString());
        db1.close();
    }
}
