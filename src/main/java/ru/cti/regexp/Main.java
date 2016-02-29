package ru.cti.regexp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Main {
    @Autowired
    ParseAndAddCalls parseAndAddCalls;

    public Main() {
    }

    public void start() {
        try {
            String ipAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        threadSleep(500);
        parseAndAddCalls.addCallsFromFiles();
        parseAndAddCalls.processWhichCallsNeedToBeEnded();
        threadSleep(5000);
        System.out.println(parseAndAddCalls.getCallHashMap().size());
        parseAndAddCalls.commitDbChangesAndCloseDb();
        System.exit(0);
    }

    private void threadSleep(int milliseconds) {
        try {
            Thread.currentThread().sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // todo хуячить ip адрес будем руками, но пока командой автоматом

    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("context.xml");
        Main main = (Main) context.getBean("main");
        main.start();
    }
}
