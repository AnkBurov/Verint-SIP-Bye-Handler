package ru.cti.regexp;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by e.karpov on 25.02.2016.
 */
public class ParseAndAddCalls {
    DB callDB;
    HTreeMap<String, Long> callHashMap;

    @Deprecated
    Set<Call> calls;

    public ParseAndAddCalls() {
        this.calls = new LinkedHashSet<Call>();
        callDB = DBMaker.fileDB(new File("calls")).closeOnJvmShutdown().make();
        callHashMap = callDB.hashMapCreate("callsHashMap")
                .keySerializer(Serializer.STRING)
                .valueSerializer(Serializer.LONG)
                .makeOrGet();
    }

    public Set<Call> getCalls() {
        return calls;
    }

    public int addCallsFromFiles() {
        /*директория для парсинга логов*/
        File dir = new File("C:\\drivers\\");
        File[] files = dir.listFiles((d, name) -> name.endsWith(".log"));
        Pattern pattern = Pattern.compile("[a-f0-9-]{30,40}\\@(?:\\d{1,3}\\.){3}\\d{1,3}");
        long before = System.currentTimeMillis();
        for (File file : files) {
            System.out.println(file.getAbsolutePath());
        }
        for (File file : files) {
            FileReader fileReader = null;
            try {
                fileReader = new FileReader(file);
                // считаем файл полностью
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                String buffer;
                while ((buffer = bufferedReader.readLine()) != null) {
                    if (buffer.length() > 200) {
                        if (buffer.contains("Request<INVITE>")) {
                            Matcher matcher = pattern.matcher(buffer.substring(200));
                            if (matcher.find()) {
//                                calls.add(new Call(System.currentTimeMillis(), matcher.group()));
                                callHashMap.putIfAbsent(matcher.group(), System.currentTimeMillis());
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    fileReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println(System.currentTimeMillis() - before);
//        System.out.println(calls.size());
//        return calls.size();
        System.out.println(callHashMap.size());
        return callHashMap.size();
    }
}
