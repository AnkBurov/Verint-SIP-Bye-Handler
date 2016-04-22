package ru.cti.verintsipbyehandler;

import gov.nist.javax.sip.header.CallID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import ru.cti.verintsipbyehandler.controller.CallHandler;
import ru.cti.verintsipbyehandler.controller.CallParser;

import javax.sip.*;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.*;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.TooManyListenersException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class implements all SIP related logic.
 * Class has methods for sending requests and processing responses.
 * sendMessage method sends SIP Bye requests with different Call-ID headers
 * processMessage method receives SIP Responses and processes them by invoking removeClosedCall of parseAndProcessClass
 * Based on JAIN SIP library.
 *
 * @author Eugeny
 */
public class SipLayer implements SipListener {
    private static final Logger logger = LogManager.getLogger(SipLayer.class);
    private String username;
    private SipStack sipStack;
    private SipFactory sipFactory;
    private AddressFactory addressFactory;
    private HeaderFactory headerFactory;
    private MessageFactory messageFactory;
    private SipProvider sipProvider;
    private String sipDestinationAddress;
    @Autowired
    ParseAndProcessCalls parseAndProcessCalls;
    @Autowired
    CallHandler callHandler;

    /**
     * Here we initialize the SIP stack.
     * So many tries and System.exit(-1) need to override bug of Event Scanner thread after receiving exception during
     * class initialization.
     * Method sipStack.stop() doesn't actually close last Event Scanner thread. See sources of method
     * for more details.
     * Parameters are pretty straightforward
     */
    public SipLayer(String username, String ip, int srcPort, String sipDestinationAddress, String sipLibraryLogLevel)
            throws PeerUnavailableException, TransportNotSupportedException,
            InvalidArgumentException, ObjectInUseException,
            TooManyListenersException, UnknownHostException {
        try {
            if (ip.isEmpty()) {
                ip = Inet4Address.getLocalHost().getHostAddress();
            }
            setUsername(username);
            this.sipDestinationAddress = sipDestinationAddress;
            sipFactory = SipFactory.getInstance();
            sipFactory.setPathName("gov.nist");
            Properties properties = new Properties();
            properties.setProperty("javax.sip.STACK_NAME", "SipByeHandler");
            try {
                properties.setProperty("javax.sip.IP_ADDRESS", ip.isEmpty() ?
                        InetAddress.getLocalHost().getHostAddress() : ip);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                logger.error("Automatic invocation of server's IP address has been failed. Try to " +
                        "specify server's IP address manually in config.properties " + e);
            }
            //DEBUGGING: Information will go to files
            //logs/siplibrary.log and logs/siplibrarydebug.log
            properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", sipLibraryLogLevel);
            properties.setProperty("gov.nist.javax.sip.SERVER_LOG",
                    "logs/siplibrary.log");
            properties.setProperty("gov.nist.javax.sip.DEBUG_LOG",
                    "logs/siplibrarydebug.log");

            sipStack = sipFactory.createSipStack(properties);
            headerFactory = sipFactory.createHeaderFactory();
            addressFactory = sipFactory.createAddressFactory();
            messageFactory = sipFactory.createMessageFactory();

            ListeningPoint tcp = sipStack.createListeningPoint(ip, srcPort, "tcp");
            ListeningPoint udp = sipStack.createListeningPoint(ip, srcPort, "udp");

            sipProvider = sipStack.createSipProvider(tcp);
            sipProvider.addSipListener(this);
            sipProvider = sipStack.createSipProvider(udp);
            sipProvider.addSipListener(this);
        } catch (PeerUnavailableException e) {
            e.printStackTrace();
            logger.catching(e);
            System.exit(-1);
        } catch (TransportNotSupportedException e) {
            e.printStackTrace();
            logger.catching(e);
            System.exit(-1);
        } catch (InvalidArgumentException e) {
            e.printStackTrace();
            logger.catching(e);
            System.exit(-1);
        } catch (ObjectInUseException e) {
            e.printStackTrace();
            logger.catching(e);
            System.exit(-1);
        } catch (TooManyListenersException e) {
            e.printStackTrace();
            logger.catching(e);
            System.exit(-1);
        }
    }

    /**
     * This method uses the SIP stack to send a message.
     */
    public void sendMessage(String callId, String message) throws ParseException,
            InvalidArgumentException, SipException {
        SipURI from = addressFactory.createSipURI(getUsername(), getHost()
                + ":" + getPort());
        Address fromNameAddress = addressFactory.createAddress(from);
        fromNameAddress.setDisplayName(getUsername());
        //todo проверить textclient
        FromHeader fromHeader = headerFactory.createFromHeader(fromNameAddress,
                "VerintSIPByeHandler");

        String username = sipDestinationAddress.substring(sipDestinationAddress.indexOf(":") + 1,
                sipDestinationAddress.indexOf("@"));
        String address = sipDestinationAddress.substring(sipDestinationAddress.indexOf("@") + 1);

        SipURI toAddress = addressFactory.createSipURI(username, address);
        Address toNameAddress = addressFactory.createAddress(toAddress);
        toNameAddress.setDisplayName(username);
        ToHeader toHeader = headerFactory.createToHeader(toNameAddress, null);

        SipURI requestURI = addressFactory.createSipURI(username, address);
        requestURI.setTransportParam("udp");

        ArrayList viaHeaders = new ArrayList();
        ViaHeader viaHeader = headerFactory.createViaHeader(getHost(),
                getPort(), "udp", "branch1");
        viaHeaders.add(viaHeader);

        CallIdHeader callIdHeader = transformStringToCallId(callId);

        CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1,
                Request.BYE);

        MaxForwardsHeader maxForwards = headerFactory
                .createMaxForwardsHeader(70);

        Request request = messageFactory.createRequest(requestURI,
                Request.BYE, callIdHeader, cSeqHeader, fromHeader,
                toHeader, viaHeaders, maxForwards);

        SipURI contactURI = addressFactory.createSipURI(getUsername(),
                getHost());
        contactURI.setPort(getPort());
        Address contactAddress = addressFactory.createAddress(contactURI);
        contactAddress.setDisplayName(getUsername());
        ContactHeader contactHeader = headerFactory
                .createContactHeader(contactAddress);
        request.addHeader(contactHeader);

        ContentTypeHeader contentTypeHeader = headerFactory
                .createContentTypeHeader("text", "plain");
        request.setContent(message, contentTypeHeader);

        sipProvider.sendRequest(request);
        logger.info("SIP BYE message of call " + callId + " has been send");
        logger.debug(request.toString());
    }

    public CallIdHeader transformStringToCallId(String callId) {
        String var1 = callId;
        CallID var2 = new CallID();

        try {
            var2.setCallId(var1);
        } catch (ParseException e) {
            logger.catching(e);
        }

        return var2;
    }

    /**
     * This method is called by the SIP stack when a response arrives. Processes each SIP Response
     * by invoking removeClosedCall of another class
     */
    public void processResponse(ResponseEvent evt) {
        Response response = evt.getResponse();
        Pattern pattern = Pattern.compile(ParseAndProcessCalls.getRegexp());
        Matcher matcher = pattern.matcher(response.getHeader(CallID.CALL_ID).toString());
        matcher.find();
        String matchedCallIdString = matcher.group();
        if (response.getStatusCode() == 200 && response.getReasonPhrase().equals("OK")) {
            logger.info("Received SIP Response " + response.getStatusCode() + " " + response.getReasonPhrase() +
                    " therefore call " + matchedCallIdString + " has been ended");
            logger.debug(response.toString());
        } else if (response.getStatusCode() == 400 && response.getReasonPhrase().equals("Bad request")) {
            logger.info("Received SIP Response " + response.getStatusCode() + " " + response.getReasonPhrase() +
                    " call " + matchedCallIdString + " wasn't found on remote side of SIP dialog. " +
                    "Probably it was already terminated");
            logger.debug(response.toString());
        } else {
            logger.warn("Received incorrect SIP Response for this application on call " + matchedCallIdString +
                    ". The SIP Response message is below " +
                    "\n" + response.toString());
        }
        callHandler.removeClosedCall(matchedCallIdString);
    }

    /**
     * This method is called by the SIP stack when a new request arrives.
     */
    public void processRequest(RequestEvent evt) {
    }

    /**
     * This method is called by the SIP stack when there's no answer
     * to a message. Note that this is treated differently from an error
     * message.
     */
    public void processTimeout(TimeoutEvent evt) {
    }

    /**
     * This method is called by the SIP stack when there's an asynchronous
     * message transmission error.
     */
    public void processIOException(IOExceptionEvent evt) {
    }

    /**
     * This method is called by the SIP stack when a dialog (session) ends.
     */
    public void processDialogTerminated(DialogTerminatedEvent evt) {
    }

    /**
     * This method is called by the SIP stack when a transaction ends.
     */
    public void processTransactionTerminated(TransactionTerminatedEvent evt) {
    }

    public String getHost() {
        int port = sipProvider.getListeningPoint().getPort();
        String host = sipStack.getIPAddress();
        return host;
    }

    public int getPort() {
        int port = sipProvider.getListeningPoint().getPort();
        return port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String newUsername) {
        username = newUsername;
    }
}
