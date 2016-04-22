package ru.cti.verintsipbyehandler.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Verint SIP Bye Handler v1.0
 * Successfully solves the problem between Cisco UCM and recording server when CUCM recording calls stuck
 * in preserved mode until SIP Trunk reboot.
 * Details of bug are here: https://tools.cisco.com/bugsearch/bug/CSCuv29131
 * The application itself closes such calls by sending SIP Bye requests.
 * <p>
 * Known bug - warning no appenders could be found due to awful using of log4j module by JAIN SIP library.
 *
 * @author Eugeny
 */
public class Main {
    @Autowired
    private CallParser callParser;
    @Autowired
    private CallHandler callHandler;
    private static final Logger logger = LogManager.getRootLogger();
    private int applicationClosingTimer;

    public Main(int applicationClosingTimer) {
        this.applicationClosingTimer = applicationClosingTimer * 1000;
    }

    public void start() throws Exception {
        Thread.currentThread().sleep(500);
        logger.info("Application has been started");
        callParser.createTables();
        try {
            callParser.addCallsFromFiles();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("An error with adding calls from RIS log path", e);
        }
        callHandler.processWhichCallsNeedToBeEnded();
        logger.info("Closing timer " + applicationClosingTimer + " ms has been started");
        Thread.currentThread().sleep(applicationClosingTimer);
        callHandler.removeOldCalls();
        logger.info("The application has been accomplished\n");
        System.exit(0);
    }

    public static void main(String[] args) {
        try {
            ApplicationContext context = new AnnotationConfigApplicationContext(
                    ru.cti.verintsipbyehandler.Configuration.class);
            Main main = (Main) context.getBean("main");
            main.start();
        } catch (Exception e) {
            e.printStackTrace();
            logger.catching(e);
            System.exit(-1);
        }
    }
}
