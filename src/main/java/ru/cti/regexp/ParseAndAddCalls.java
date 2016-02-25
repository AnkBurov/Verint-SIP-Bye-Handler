package ru.cti.regexp;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by e.karpov on 25.02.2016.
 */
public class ParseAndAddCalls {
    Set<Call> calls;

    public ParseAndAddCalls() {
        this.calls = new LinkedHashSet<Call>();
    }

    public int addCallsFromFile() {
//        File file = new File("C:\\drivers\\1.txt");
        File file = new File("C:\\drivers\\integrationservice_2015_02_04_0097.log");
        File file2 = new File("C:\\drivers\\test.txt");
        FileWriter fileWriter = null;
        FileReader fileReader = null;

//        Date date = new Date(System.currentTimeMillis());
        long before = System.currentTimeMillis();
//        System.out.println(before);
        try {
            fileReader = new FileReader(file);
            char[] buffer = new char[(int) file.length()];
            // считаем файл полностью
            fileReader.read(buffer);
//            System.out.println(new String(buffer));

            Pattern pattern1 = Pattern.compile("[a-z0-9-]{32}\\@(?:\\d{1,3}\\.){3}\\d{1,3}");
            Matcher matcher = pattern1.matcher(new String(buffer));
            while (matcher.find()) {
//                System.out.println(matcher.group());
                calls.add(new Call(System.currentTimeMillis(), matcher.group()));
            }
            fileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fileReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println(System.currentTimeMillis() - before);
//        System.out.println(calls.size());
        return calls.size();
    }
}
