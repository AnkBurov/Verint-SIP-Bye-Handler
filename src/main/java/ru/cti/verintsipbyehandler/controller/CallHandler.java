package ru.cti.verintsipbyehandler.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import ru.cti.verintsipbyehandler.controller.dao.DAOFacade;
import ru.cti.verintsipbyehandler.model.domainobjects.Call;
import ru.cti.verintsipbyehandler.model.factory.CallsFactory;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.List;

/**
 * This class invokes sendMessage (SIP Bye) method of SipLayer class to end each call when their time comes.
 * "Their time" is managed by callTerminationTimeout attribute. See config.properties for more information.
 * When application receives SIP Response for each SIP Bye request, special method inserts isEnded field as true
 * Before normal exit from application, method removeOldCalls removes old calls call database.
 * The attribute completedCallDeletionTimer used to decide whether to remove call or it's too early.
 *
 * @author Eugeny
 */
public class CallHandler {
    private static final Logger logger = LogManager.getLogger(CallHandler.class);
    @Autowired
    DAOFacade daoFacade;
    @Autowired
    CallsFactory callsFabric;
    @Autowired
    private SipLayer sipLayer;
    private long callTerminationTimeout;
    private long completedCallDeletionTimer;
    private int sipByeSenderPause;

    /**
     * Constructor method
     *
     * @param callTerminationTimeout      timeout before each call will be ended by sending SIP Bye request.
     *                                    measures in ms in code.
     * @param completedCallDeletionTimer: timeout before each call will be removed from completed calls DB
     *                                    measures in ms in code;
     * @param sipByeSenderPause           Number of MILLISECONDS between each outgoing SIP Bye message.
     *                                    Delay needed for not overweighting remote SIP adapter
     */
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
                "completed calls deletion timeout is " + completedCallDeletionTimer / +86400000 + " days");
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

    /**
     * method updates isEnded field in DB from false to true
     */
    public boolean closeClosedCall(String callId) {
        try {
            daoFacade.getCallDAO().updateClosedCall(callId);
            logger.info("Call " + callId + " has been removed from current calls DB and added to completed calls DB");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            logger.catching(e);
            return false;
        }
    }

    /**
     * method removeOldCalls removes old calls that older then specified completedCallDeletionTimer
     */
    public boolean removeOldCalls() {
        try {
            List<Call> calls = daoFacade.getCallDAO().getAllCompletedCalls();
            for (Call call : calls) {
                if (System.currentTimeMillis() - call.getTimeOfCall() > completedCallDeletionTimer) {
                    long markedForRemovalCallAge = (System.currentTimeMillis() - call.getTimeOfCall()) / 86400000;
                    logger.debug("Call " + call.getCallId() + " with age " + markedForRemovalCallAge + " marked for deletion");
                    daoFacade.getCallDAO().delete(call.getId());
                }
            }
            logger.info("All DB changes have been successfully done");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            logger.catching(e);
            return false;
        }
    }
}
