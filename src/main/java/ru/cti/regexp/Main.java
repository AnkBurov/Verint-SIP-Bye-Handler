package ru.cti.regexp;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Main {
    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("context.xml");
        ParseAndAddCalls parseAndAddCalls = (ParseAndAddCalls) context.getBean("parseAndAddCalls");
        parseAndAddCalls.addCallsFromFile();
    }
}
