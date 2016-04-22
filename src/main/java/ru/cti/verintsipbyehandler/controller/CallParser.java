package ru.cti.verintsipbyehandler.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.UncategorizedSQLException;
import ru.cti.verintsipbyehandler.controller.dao.DAOFacade;
import ru.cti.verintsipbyehandler.model.factory.CallsFactory;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class parses .log files from specified directory and processes calls by adding Call-IDs in SQLite3 database
 *
 * @author Eugeny
 */
public class CallParser {
    private static final Logger logger = LogManager.getLogger(CallParser.class);
    @Autowired
    DAOFacade daoFacade;
    @Autowired
    CallsFactory callsFabric;
    private String risLogsFolderPath;
    private String regexp;

    /**
     * Constructor method
     *
     * @param regexp            used regular expression from config.properties
     * @param risLogsFolderPath RIS log folder from config.properties (and next ones)
     */
    public CallParser(String risLogsFolderPath, String regexp) {
        this.risLogsFolderPath = risLogsFolderPath;
        this.regexp = regexp;
    }

    public String getRegexp() {
        return regexp;
    }

    /**
     * This method creates all needed tables
     */
    public void createTables() {
        daoFacade.getCallDAO().createTable();
    }

    /**
     * This method parses and adds call-identifiers to call DB. Each call will only be added if it absents in calls DB.
     */
    public void addCallsFromFiles() throws Exception {
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
                                    logger.info("Call " + regexFoundString + " has been added to calls DB");
                                } catch (UncategorizedSQLException e) {
                                    logger.trace("Call " + regexFoundString + " already added in the DB");
                                }
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
