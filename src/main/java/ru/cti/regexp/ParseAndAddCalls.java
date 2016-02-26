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
        File file = new File("C:\\drivers\\integrationservice_2015_02_04_0097.log");
        FileReader fileReader = null;
        long before = System.currentTimeMillis();
        try {
            fileReader = new FileReader(file);
            char[] buffer = new char[(int) file.length()];
            // считаем файл полностью
            fileReader.read(buffer);
            // разбиваем на массив строк по каждой строчке файла
            String[] strings = new String(buffer).split("\\r\\n");
            // если строчка является SIP INVITE'ом, то проверить регэкспом и при совпадении добавить регэксп в set
            for (int i = 0; i < strings.length; i++) {
                if (strings[i].contains("Request<INVITE>")) {
                    Pattern pattern = Pattern.compile("[a-z0-9-]{32}\\@(?:\\d{1,3}\\.){3}\\d{1,3}");
                    Matcher matcher = pattern.matcher(strings[i].substring(200));
                    if (matcher.find()) {
                        calls.add(new Call(System.currentTimeMillis(), matcher.group()));
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
        System.out.println(System.currentTimeMillis() - before);
        return calls.size();
    }
}
