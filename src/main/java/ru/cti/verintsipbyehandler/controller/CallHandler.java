package ru.cti.verintsipbyehandler.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import ru.cti.verintsipbyehandler.SipLayer;
import ru.cti.verintsipbyehandler.controller.dao.DAOFacade;
import ru.cti.verintsipbyehandler.model.domainobjects.Call;
import ru.cti.verintsipbyehandler.model.fabric.CallsFabric;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

public class CallHandler {
    private static final Logger logger = LogManager.getLogger(CallHandler.class);
    @Autowired
    DAOFacade daoFacade;
    @Autowired
    CallsFabric callsFabric;
    @Autowired
    private SipLayer sipLayer;
    private long callTerminationTimeout;
    private long completedCallDeletionTimer;
    private int sipByeSenderPause;

    public CallHandler(long callTerminationTimeout, long completedCallDeletionTimer, int sipByeSenderPause) {
        this.callTerminationTimeout = callTerminationTimeout * 60000;
        this.completedCallDeletionTimer = completedCallDeletionTimer * 86400000;
        this.sipByeSenderPause = sipByeSenderPause;
    }

    /**
     * This method processes each call in current call DB and invokes sendMessage method of SipLayer class.
     * sendMessage method invokes only if call is older than specified callTerminationTimeout
     */
    public void processWhichCallsNeedToBeEnded() {
        logger.info("The set call termination timeout is " + callTerminationTimeout / 60000 + " minutes and " +
                "completed calls deletion timeout is " + completedCallDeletionTimer / + 86400000 + " days");
        List<Call> calls = daoFacade.getCallDAO().getAllActiveCalls();
        for (Call call : calls) {
            if (System.currentTimeMillis() - call.getTimeOfCall() > callTerminationTimeout) {
                // sending SIP Bye
                try {
                    logger.info("Trying to send SIP BYE message on call " + call.getCallId());
                    sipLayer.sendMessage(call.getCallId(), "");
                    // delay needed for not overweighting remote SIP adapter
                    try {
                        Thread.currentThread().sleep(sipByeSenderPause);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                    logger.catching(e);
                } catch (InvalidArgumentException e) {
                    e.printStackTrace();
                    logger.catching(e);
                } catch (SipException e) {
                    e.printStackTrace();
                    logger.catching(e);
                }
            }
        }
    }
}
