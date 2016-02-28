package ru.cti.regexp;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Main {


    // todo хуячить ip адрес будем руками, но пока командой автоматом

    // todo переделать и нормальный вызов контекста, чтобы все классы вызываемые были полями
    public static void main(String[] args) {
        try {
            String ipAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        ApplicationContext context = new ClassPathXmlApplicationContext("context.xml");
        ParseAndAddCalls parseAndAddCalls = (ParseAndAddCalls) context.getBean("parseAndAddCalls");
        threadSleep(500);
        parseAndAddCalls.addCallsFromFiles();
        parseAndAddCalls.processWhichCallsNeedToBeEnded();
        System.out.println(parseAndAddCalls.getCallHashMap().size());
        threadSleep(500);
        parseAndAddCalls.commitDbChangesAndCloseDb();
        System.exit(0);
    }

    private static void threadSleep(int milliseconds) {
        try {
            Thread.currentThread().sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
