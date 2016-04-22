package ru.cti.verintsipbyehandler.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.UncategorizedSQLException;
import ru.cti.verintsipbyehandler.controller.dao.DAOFacade;
import ru.cti.verintsipbyehandler.model.fabric.CallsFabric;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CallParser {
    private static final Logger logger = LogManager.getLogger(CallParser.class);
    @Autowired
    DAOFacade daoFacade;
    @Autowired
    CallsFabric callsFabric;
    private String risLogsFolderPath;
    private String regexp;

    public CallParser(String risLogsFolderPath, String regexp) {
        this.risLogsFolderPath = risLogsFolderPath;
        this.regexp = regexp;
    }

    public String getRegexp() {
        return regexp;
    }

    public void addCallsFromFiles() throws Exception {
        daoFacade.getCallDAO().createTable();
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
                                try {
                                    daoFacade.getCallDAO().create(callsFabric.create(regexFoundString));
                                    logger.info("Call " + regexFoundString + " has been added to current calls DB");
                                } catch (UncategorizedSQLException e) {
                                    // db already has this call
                                }
                                /*if (!completedCallsHashMap.containsKey(regexFoundString)) {
                                    if (!callHashMap.containsKey(regexFoundString)) {
                                        logger.info("Call " + regexFoundString + " has been added to current calls DB");
                                    }
                                    callHashMap.putIfAbsent(regexFoundString, System.currentTimeMillis());
                                }*/
                            }
                        }
                    }
                }
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
    }
}
