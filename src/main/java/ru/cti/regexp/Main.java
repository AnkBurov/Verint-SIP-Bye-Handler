package ru.cti.regexp;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Main {
    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("context.xml");
        ParseAndAddCalls parseAndAddCalls = (ParseAndAddCalls) context.getBean("parseAndAddCalls");
        try {
            Thread.currentThread().sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("The number of calls is: " + parseAndAddCalls.addCallsFromFile());

    }
}
