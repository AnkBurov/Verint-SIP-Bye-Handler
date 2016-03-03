package ru.cti.verintsipbyehandler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

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
    private ParseAndProcessCalls parseAndProcessCalls;
    private static final Logger logger = LogManager.getRootLogger();

    public Main() {
    }

    public void setParseAndProcessCalls(ParseAndProcessCalls parseAndProcessCalls) {
        this.parseAndProcessCalls = parseAndProcessCalls;
    }

    public void start() throws Exception {
        Thread.currentThread().sleep(500);
        logger.info("Application has been started");
        try {
            parseAndProcessCalls.addCallsFromFiles();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("An error with adding calls from RIS log path", e);
        }
        parseAndProcessCalls.processWhichCallsNeedToBeEnded();
        Thread.currentThread().sleep(15000);
        parseAndProcessCalls.commitDbChangesAndCloseDb();
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
