package ru.cti.regexp;

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
    Set<Call> calls;

    public ParseAndAddCalls() {
        this.calls = new LinkedHashSet<Call>();
    }

    public int addCallsFromFiles() {
        /*директория для парсинга логов*/
        File dir = new File("C:\\drivers\\");
        File[] files = dir.listFiles((d, name) -> name.endsWith(".log"));
        long before = System.currentTimeMillis();
        for (File file : files) {
            System.out.println(file.getAbsolutePath());
        }
        for (File file : files) {
            FileReader fileReader = null;
            try {
                fileReader = new FileReader(file);
//                char[] buffer = new char[(int) file.length()];
                // считаем файл полностью
//                fileReader.read(buffer);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                String buffer;
                // todo итератор?
                while ((buffer = bufferedReader.readLine()) != null) {
                    if (buffer.length() > 200) {
                        if (buffer.contains("Request<INVITE>")) {
                            Pattern pattern = Pattern.compile("[a-f0-9-]{30,40}\\@(?:\\d{1,3}\\.){3}\\d{1,3}");
                            Matcher matcher = pattern.matcher(buffer.substring(200));
                            if (matcher.find()) {
                                calls.add(new Call(System.currentTimeMillis(), matcher.group()));
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
        System.out.println(calls.size());
        return calls.size();
    }
}
