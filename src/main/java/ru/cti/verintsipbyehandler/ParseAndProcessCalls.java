package ru.cti.verintsipbyehandler;

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

/**
 * This class parses .log files from specified directory, processes calls by adding Call-IDs in MapDB HashMap database
 * and invokes sendMessage (SIP Bye) method of SipLayer class to end each call when their time comes.
 * "Their time" is managed by callTerminationTimeout attribute. See config.properties for more information.
 * When application receives SIP Response for each SIP Bye request, special method removes completed call from
 * current call database (callDB) and adds it to completed call database.
 * Before normal exit from application, method commitDbChangesAndCloseDb removes old calls from completed call database.
 * The attribute completedCallDeletionTimer used to decide whether to remove call or it's too early.
 *
 * @author Eugeny
 */
public class ParseAndProcessCalls {
    private static final Logger logger = LogManager.getLogger(ParseAndProcessCalls.class);
    @Autowired
    private SipLayer sipLayer;
    private DB callDB;
    private HTreeMap<String, Long> callHashMap;
    private DB completedCallsDB;
    private HTreeMap<String, Long> completedCallsHashMap;
    private long callTerminationTimeout;
    private long completedCallDeletionTimer;
    private int sipByeSenderPause;
    private static String regexp;
    private String risLogsFolderPath;

    /**
     * Constructor method
     *
     * @param regexp                      used regular expression from config.properties
     * @param risLogsFolderPath           RIS log folder from config.properties (and next ones)
     * @param callTerminationTimeout      timeout before each call will be ended by sending SIP Bye request.
     *                                    measures in ms in code.
     * @param completedCallDeletionTimer: timeout before each call will be removed from completed calls DB
     *                                    measures in ms in code;
     * @param sipByeSenderPause           Number of MILLISECONDS between each outgoing SIP Bye message.
     *                                    Delay needed for not overweighting remote SIP adapter
     */
    public ParseAndProcessCalls(String regexp, String risLogsFolderPath, int callTerminationTimeout,
                                int completedCallDeletionTimer, int sipByeSenderPause) throws Exception {
        callDB = DBMaker.fileDB(new File("db/calls")).closeOnJvmShutdown().make();
        callHashMap = callDB.hashMapCreate("callsHashMap")
                .keySerializer(Serializer.STRING)
                .valueSerializer(Serializer.LONG)
                .makeOrGet();
        completedCallsDB = DBMaker.fileDB(new File("db/compltetedcalls")).closeOnJvmShutdown().make();
        completedCallsHashMap = completedCallsDB.hashMapCreate("completedcallsHashMap")
                .keySerializer(Serializer.STRING)
                .valueSerializer(Serializer.LONG)
                .makeOrGet();
        ParseAndProcessCalls.regexp = regexp; // заполнение статической переменной
        this.risLogsFolderPath = risLogsFolderPath;
        this.callTerminationTimeout = callTerminationTimeout * 60000;
        this.completedCallDeletionTimer = completedCallDeletionTimer * 86400000;
        this.sipByeSenderPause = sipByeSenderPause;
    }

    /**
     * This method parses and adds call-ids to current call DB. Each call will only be added if it absents in
     * completed call DB. It need to prevent adding and sending SIP Bye requests several time instead of one
     */
    public int addCallsFromFiles() throws Exception {
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
        for (File file : files) {
            logger.info("Trying to parse regexp in log file " + file.getAbsolutePath());
            FileReader fileReader = null;
            try {
                fileReader = new FileReader(file);
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
                                    callHashMap.putIfAbsent(regexFoundString, System.currentTimeMillis());
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
        logger.info("Log files by path " + risLogsFolderPath + " have been observed");
        logger.info(callHashMap.size() + " calls currently stored in current calls DB and " + completedCallsHashMap.size() +
                " calls in completed calls DB");
        return callHashMap.size();
    }

    /**
     * This method processes each call in current call DB and invokes sendMessage method of SipLayer class.
     * sendMessage method invokes only if call is older than specified callTerminationTimeout
     */
    public void processWhichCallsNeedToBeEnded() {
        logger.info("The set call termination timeout is " + callTerminationTimeout / 60000 + " minutes and " +
                "completed calls deletion timeout is " + completedCallDeletionTimer / +86400000 + " days");
        for (Map.Entry<String, Long> call : callHashMap.entrySet()) {
            if (!completedCallsHashMap.containsKey(call.getKey()) &&
                    System.currentTimeMillis() - call.getValue() > callTerminationTimeout) {
                // sending SIP Bye
                try {
                    logger.info("Trying to send SIP BYE message on call " + call.getKey());
                    sipLayer.sendMessage(call.getKey(), "");
                    // delay needed for not overweighting remote SIP adapter
                    try {
                        Thread.currentThread().sleep(sipByeSenderPause);
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

    /**
     * method removes call from current call DB and adds it to completed call DB
     */
    public boolean removeClosedCall(String callId) {
        try {
            completedCallsHashMap.putIfAbsent(callId, System.currentTimeMillis());
            callHashMap.remove(callId);
            logger.info("Call " + callId + " has been removed from current calls DB and added to completed calls DB");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            logger.catching(e);
            return false;
        }
    }

    /**
     * method commitDbChangesAndCloseDb removes old calls from completed call database and commites changes
     */
    public boolean commitDbChangesAndCloseDb() {
        try {
            // удаление старых записей в completed calls DB
            for (Iterator<Map.Entry<String, Long>> iterator = completedCallsHashMap.entrySet().iterator();
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
            callDB.close();
            completedCallsDB.close();
            logger.info("All DB changes have been successfully committed and DB closed");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            logger.catching(e);
            return false;
        }
    }

    public static String getRegexp() {
        return regexp;
    }
}
