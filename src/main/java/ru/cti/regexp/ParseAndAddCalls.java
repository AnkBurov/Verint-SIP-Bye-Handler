package ru.cti.regexp;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    DB callDB;
    private HTreeMap<String, Long> callHashMap;

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

    public int processWhichCallsNeedToBeEnded() {
        for (Map.Entry<String, Long> call : callHashMap.entrySet()) {
            if (System.currentTimeMillis() - call.getValue() > 10) {
                //todo отправляем SIP BYE
                try {
                    sipLayer.sendMessage(call.getKey(), "");
                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (InvalidArgumentException e) {
                    e.printStackTrace();
                } catch (SipException e) {
                    e.printStackTrace();
                }
            }
        }


        
        
        
        return 1;
    }

    public long removeClosedCall(String callId) {
        //todo написать в логе removed
        return callHashMap.remove(callId);
    }
}
