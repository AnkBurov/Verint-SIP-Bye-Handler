package ru.cti.regexp;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.io.*;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by e.karpov on 25.02.2016.
 */
public class ParseAndAddCalls {
    @Autowired
    private SipLayer sipLayer;
    private DB callDB;
    private HTreeMap<String, Long> callHashMap;
    private long callTerminationTimeout;
    private static String regexp;
    private String risLogsFolderPath;

    // SipLayer использует переменную regexp
    public static String getRegexp() {
        return regexp;
    }




    public ParseAndAddCalls(String regexp, String risLogsFolderPath, int callTerminationTimeout) {
        callDB = DBMaker.fileDB(new File("calls")).closeOnJvmShutdown().make();
        callHashMap = callDB.hashMapCreate("callsHashMap")
                .keySerializer(Serializer.STRING)
                .valueSerializer(Serializer.LONG)
                .makeOrGet();
        ParseAndAddCalls.regexp = regexp; // заполнение статической переменной
        this.risLogsFolderPath = risLogsFolderPath;
        this.callTerminationTimeout = callTerminationTimeout * 60000;
    }

    public HTreeMap<String, Long> getCallHashMap() {
        return callHashMap;
    }

    public int addCallsFromFiles() {
        /*директория для парсинга логов*/
        // чтобы пароль принял - нужно сохранить пароль
        File dir = new File(risLogsFolderPath);
        //todo убрать лямбду для совместимости с Java 1.6
        File[] files = dir.listFiles((d, name) -> name.endsWith(".log"));
        Pattern pattern = Pattern.compile(regexp);
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
                                callHashMap.putIfAbsent(matcher.group(), System.currentTimeMillis());
                            }
                        }
                    }
                }
                callDB.commit();
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

    public void processWhichCallsNeedToBeEnded() {
        for (Map.Entry<String, Long> call : callHashMap.entrySet()) {
            if (System.currentTimeMillis() - call.getValue() > callTerminationTimeout) {
                // отправляем SIP BYE
                try {
                    sipLayer.sendMessage(call.getKey(), "");
                    // задержка нужна, чтобы не перезагрузить адаптер
                    // в больших окружениях, полагаю, нужно ставить больше
                    try {
                        Thread.currentThread().sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (InvalidArgumentException e) {
                    e.printStackTrace();
                } catch (SipException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean removeClosedCall(String callId) {
        //todo написать в логе removed
        try {
            callHashMap.remove(callId);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean commitDbChangesAndCloseDb() {
        try {
            callDB.commit();
            callDB.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
