package ru.cti.regexp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.Iterator;
import java.util.Set;

public class SipSenderManager {
    @Autowired
    @Qualifier("parseAndAddCalls")
    ParseAndAddCalls parseAndAddCalls;
    @Autowired
    SipLayer sipLayer;
    Set<Call> calls;
    String sipDestinationAddress;

    public SipSenderManager(String sipDestinationAddress, Set<Call> calls) {
//        this.calls = parseAndAddCalls.getCalls();
        this.sipDestinationAddress = sipDestinationAddress;
        this.calls = calls;
    }

    //todo пока void, потом переделать в более тестируемое
    public void sendByeMessages() {
//        for (Iterator<Call> iterator = calls.iterator(); iterator.hasNext(); ) {
//            Call next = iterator.next();
//            try {
//                sipLayer.sendMessage(sipDestinationAddress, next.getCallId(), "");
//                try {
//                    Thread.currentThread().sleep(1);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                iterator.remove();
//            } catch (ParseException e) {
//                e.printStackTrace();
//            } catch (InvalidArgumentException e) {
//                e.printStackTrace();
//            } catch (SipException e) {
//                e.printStackTrace();
//            }
//        }
//        System.exit(0);
    }
}
