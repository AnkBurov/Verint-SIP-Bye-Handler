package ru.cti.regexp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Main {
    // todo сделать privatte
    @Autowired
    ParseAndAddCalls parseAndAddCalls;
    private static final Logger logger = LogManager.getRootLogger();


    public Main() {
    }

    public void start() throws Exception {
        Thread.currentThread().sleep(500);
        logger.info("Application has been started");
        try {
            parseAndAddCalls.addCallsFromFiles();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("An error with adding calls from RIS log path", e);
        }
        parseAndAddCalls.processWhichCallsNeedToBeEnded();
        //todo поставить 5 секунд при релизе
        Thread.currentThread().sleep(500);
        System.out.println(parseAndAddCalls.getCallHashMap().size());
        parseAndAddCalls.commitDbChangesAndCloseDb();
        logger.info("The application has been accomplished\n");
        System.exit(0);
    }

    public static void main(String[] args) {
        try {
            ApplicationContext context = new ClassPathXmlApplicationContext("context.xml");
            Main main = (Main) context.getBean("main");
            main.start();
        } catch (Exception e) {
            e.printStackTrace();
            logger.catching(e);
            System.exit(-1);
        }
    }
}
