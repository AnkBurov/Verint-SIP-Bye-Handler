package ru.cti.regexp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mapdb.*;
import org.springframework.beans.factory.annotation.Autowired;

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
    private DB completedCallsDB;
    private HTreeMap<String, Long> completedCallsHashMap;
    private long callTerminationTimeout;
    private long completedCallDeletionTimer;
    private static String regexp;
    private String risLogsFolderPath;

    // SipLayer использует переменную regexp
    public static String getRegexp() {
        return regexp;
    }

    // todo НЕ ЗАБЫТЬ COMMIT ЭТОЙ БД!
    public ParseAndAddCalls(String regexp, String risLogsFolderPath, int callTerminationTimeout,
                            int completedCallDeletionTimer) throws Exception {
        callDB = DBMaker.fileDB(new File("calls")).closeOnJvmShutdown().make();
        callHashMap = callDB.hashMapCreate("callsHashMap")
                .keySerializer(Serializer.STRING)
                .valueSerializer(Serializer.LONG)
                .makeOrGet();
        completedCallsDB = DBMaker.fileDB(new File("compltetedcalls")).closeOnJvmShutdown().make();
        completedCallsHashMap = completedCallsDB.hashMapCreate("completedcallsHashMap")
                .keySerializer(Serializer.STRING)
                .valueSerializer(Serializer.LONG)
                .makeOrGet();
        ParseAndAddCalls.regexp = regexp; // заполнение статической переменной
        this.risLogsFolderPath = risLogsFolderPath;
        this.callTerminationTimeout = callTerminationTimeout * 60000;
        this.completedCallDeletionTimer = completedCallDeletionTimer * 86400000;
    }

    public HTreeMap<String, Long> getCallHashMap() {
        return callHashMap;
    }

    public int addCallsFromFiles() throws Exception {
        System.out.println(callHashMap.size());
        System.out.println(completedCallsHashMap.size());
        for (int i = 0; i < completedCallsHashMap.size(); i++) {
        }
        /*директория для парсинга логов*/
        // чтобы пароль принял - нужно сохранить пароль
        File dir = new File(risLogsFolderPath);
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.endsWith(".log")) {
                    return true;
                }
                return false;
            }
        });
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
                                String regexFoundString = matcher.group();
                                if (!completedCallsHashMap.containsKey(regexFoundString)) {
                                    if (!callHashMap.containsKey(regexFoundString)) {
                                        logger.info("Call " + regexFoundString + " has been added to current calls DB");
                                    }
                                    callHashMap.putIfAbsent(matcher.group(), System.currentTimeMillis());
                                }
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
        logger.info(callHashMap.size() + " calls currently stored in current calls DB and " + completedCallsHashMap +
                " calls in completed calls DB");
        return callHashMap.size();
    }

    public void processWhichCallsNeedToBeEnded() {
        logger.info("The set call termination timeout is " + callTerminationTimeout / 60000 + " minutes");
        for (Map.Entry<String, Long> call : callHashMap.entrySet()) {
            if (!completedCallsHashMap.containsKey(call.getKey()) &&
                    System.currentTimeMillis() - call.getValue() > callTerminationTimeout) {
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
            completedCallsHashMap.putIfAbsent(callId, System.currentTimeMillis());
            callHashMap.remove(callId);
            completedCallsDB.commit();
            logger.debug("Call " + callId + " has been removed from current calls DB and added to completed calls DB");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            logger.catching(e);
            return false;
        }
    }


    public boolean commitDbChangesAndCloseDb() {
        try {
            // удаление старых записей в completed calls DB
            for(Iterator<Map.Entry<String, Long>> iterator = completedCallsHashMap.entrySet().iterator();
                iterator.hasNext(); ) {
                Map.Entry<String, Long> entry = iterator.next();
                if (System.currentTimeMillis() - entry.getValue() > completedCallDeletionTimer) {
                    long markedForRemovalCallAge = (System.currentTimeMillis() - entry.getValue()) / 86400000;
                    logger.debug("Call " + entry.getKey() + " with age " + markedForRemovalCallAge + " marked for deletion");
                    iterator.remove();
                }
            }
            completedCallsDB.commit();
            callDB.commit();
            logger.info("All DB changes have been successfully committed and DB closed");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            logger.catching(e);
            return false;
        }
    }
}
