package ru.cti.regexp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mapdb.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.io.*;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParseAndAddCalls {
    private static final Logger logger = LogManager.getLogger(ParseAndAddCalls.class);
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


    public ParseAndAddCalls(String regexp, String risLogsFolderPath, int callTerminationTimeout) throws Exception {
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

    public int addCallsFromFiles() throws Exception {
        /*директория для парсинга логов*/
        // чтобы пароль принял - нужно сохранить пароль
        File dir = new File(risLogsFolderPath);
        //todo убрать лямбду для совместимости с Java 1.6
        File[] files = dir.listFiles((d, name) -> name.endsWith(".log"));
        Pattern pattern = Pattern.compile(regexp);
        long before = System.currentTimeMillis();
        // todo убрать из консоли
        for (File file : files) {
            System.out.println(file.getAbsolutePath());
        }
        for (File file : files) {
            logger.info("Trying to parse regexp in log file " + file.getAbsolutePath());
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
                                String tempFoundString = matcher.group();
                                if (!callHashMap.containsKey(tempFoundString)) {
                                    logger.info("Call " + tempFoundString + " has been added to DB");
                                }
                                callHashMap.putIfAbsent(matcher.group(), System.currentTimeMillis());
                            }
                        }
                    }
                }
                callDB.commit();
            } catch (IOException e) {
                e.printStackTrace();
                logger.catching(e);
            } finally {
                try {
                    fileReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    logger.catching(e);
                }
            }
        }
        System.out.println(System.currentTimeMillis() - before);
        System.out.println(callHashMap.size());
        logger.info("Log files by path " + risLogsFolderPath + " have been observed");
        logger.info(callHashMap.size() + " calls currently stored in DB.");
        return callHashMap.size();
    }

    public void processWhichCallsNeedToBeEnded() {
        logger.info("The set call termination timeout is " + callTerminationTimeout / 60000 + " minutes");
        for (Map.Entry<String, Long> call : callHashMap.entrySet()) {
            long current = System.currentTimeMillis();
            long test2 = call.getValue();
            long test = System.currentTimeMillis() - call.getValue();
            if (System.currentTimeMillis() - call.getValue() > callTerminationTimeout) {
                // отправляем SIP BYE
                try {
                    logger.debug("Trying to send SIP BYE message on call " + call.getKey());
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
                    logger.catching(e);
                } catch (InvalidArgumentException e) {
                    e.printStackTrace();
                    logger.catching(e);
                } catch (SipException e) {
                    e.printStackTrace();
                    logger.catching(e);
                }
            }
        }
    }

    public boolean removeClosedCall(String callId) {
        //todo написать в логе removed
        try {
            callHashMap.remove(callId);
            logger.debug("Call " + callId + " has been removed from DB");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            logger.catching(e);
            return false;
        }
    }

    public boolean commitDbChangesAndCloseDb() {
        try {
            callDB.commit();
            callDB.close();
            logger.info("All DB changes have been successfully committed and DB closed");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            logger.catching(e);
            return false;
        }
    }
}
