package ru.cti.regexp;

import gov.nist.javax.sip.header.CallID;
import gov.nist.javax.sip.stack.MessageProcessor;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sip.*;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.*;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.TooManyListenersException;

public class SipLayer implements SipListener {

    private MessageProcessor messageProcessor;

    private String username;

    private SipStack sipStack;

    private SipFactory sipFactory;

    private AddressFactory addressFactory;

    private HeaderFactory headerFactory;

    private MessageFactory messageFactory;

    private SipProvider sipProvider;

    private String sipDestinationAddress;

    @Autowired
    ParseAndAddCalls parseAndAddCalls;

    /**
     * Here we initialize the SIP stack.
     */
    private SipLayer(String username, String ip, int srcPort, String sipDestinationAddress)
            throws PeerUnavailableException, TransportNotSupportedException,
            InvalidArgumentException, ObjectInUseException,
            TooManyListenersException {
        setUsername(username);
        this.sipDestinationAddress = sipDestinationAddress;
        sipFactory = SipFactory.getInstance();
        sipFactory.setPathName("gov.nist");
        Properties properties = new Properties();
        properties.setProperty("javax.sip.STACK_NAME", "SipByeHandler");
        properties.setProperty("javax.sip.IP_ADDRESS", ip);

        //DEBUGGING: Information will go to files
        //textclient.log and textclientdebug.log
        properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "32");
        properties.setProperty("gov.nist.javax.sip.SERVER_LOG",
                "textclient.txt");
        properties.setProperty("gov.nist.javax.sip.DEBUG_LOG",
                "textclientdebug.log");

        sipStack = sipFactory.createSipStack(properties);
        headerFactory = sipFactory.createHeaderFactory();
        addressFactory = sipFactory.createAddressFactory();
        messageFactory = sipFactory.createMessageFactory();

        // deprecated. Юзать это ListeningPoint tcp = sipStack.createListeningPoint("192.168.0.1", srcPort, "tcp");
        ListeningPoint tcp = sipStack.createListeningPoint(ip, srcPort, "tcp");
        ListeningPoint udp = sipStack.createListeningPoint(ip, srcPort, "udp");

        sipProvider = sipStack.createSipProvider(tcp);
        sipProvider.addSipListener(this);
        sipProvider = sipStack.createSipProvider(udp);
        sipProvider.addSipListener(this);
    }

    // to это в формате SIP:1016@172.16.33.186:5060
    // todo убрать или переделать message. или не надо

    /**
     * This method uses the SIP stack to send a message.
     */
    public void sendMessage(String callId, String message) throws ParseException,
            InvalidArgumentException, SipException {

        SipURI from = addressFactory.createSipURI(getUsername(), getHost()
                + ":" + getPort());
        Address fromNameAddress = addressFactory.createAddress(from);
        fromNameAddress.setDisplayName(getUsername());
        FromHeader fromHeader = headerFactory.createFromHeader(fromNameAddress,
                "textclientv1.0");

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

//        CallIdHeader callIdHeader = getSpecifiedCallId();
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
    }

    public CallIdHeader transformStringToCallId(String callId) {
        String var1 = callId;
        CallID var2 = new CallID();

        try {
            var2.setCallId(var1);
        } catch (ParseException var4) {
            ;
        }

        return var2;
    }

    // обрабатывает присланный SIP Response

    /**
     * This method is called by the SIP stack when a response arrives.
     */
    public void processResponse(ResponseEvent evt) {
        /*Response response = evt.getResponse();
        int status = response.getStatusCode();

        if ((status >= 200) && (status < 300)) { //Success!
            messageProcessor.processInfo("--Sent");
            return;
        }

        messageProcessor.processError("Previous message not sent: " + status);*/

        Response response = evt.getResponse();
        int status = response.getStatusCode();
        System.out.println(response.getHeader(CallID.CALL_ID).toString());
//        parseAndAddCalls.removeClosedCall();


//        if ((status >= 200) && (status < 300)) { //Success!
//            messageProcessor.processInfo("--Sent");
//            return;
//        }
//
//        messageProcessor.processError("Previous message not sent: " + status);
    }

    /**
     * This method is called by the SIP stack when a new request arrives.
     */
    public void processRequest(RequestEvent evt) {
        /*Request req = evt.getRequest();

        String method = req.getMethod();
        if (!method.equals("MESSAGE")) { //bad request type.
            messageProcessor.processError("Bad request type: " + method);
            return;
        }

        FromHeader from = (FromHeader) req.getHeader("From");
        messageProcessor.processMessage(from.getAddress().toString(),
                new String(req.getRawContent()));
        Response response = null;
        try { //Reply with OK
            response = messageFactory.createResponse(200, req);
            ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
            toHeader.setTag("888"); //This is mandatory as per the spec.
            ServerTransaction st = sipProvider.getNewServerTransaction(req);
            st.sendResponse(response);
        } catch (Throwable e) {
            e.printStackTrace();
            messageProcessor.processError("Can't send OK reply.");
        }*/
    }

    /**
     * This method is called by the SIP stack when there's no answer
     * to a message. Note that this is treated differently from an error
     * message.
     */
    public void processTimeout(TimeoutEvent evt) {
       /* messageProcessor
                .processError("Previous message not sent: " + "timeout");*/
    }

    /**
     * This method is called by the SIP stack when there's an asynchronous
     * message transmission error.
     */
    public void processIOException(IOExceptionEvent evt) {
        /*messageProcessor.processError("Previous message not sent: "
                + "I/O Exception");*/
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

    public MessageProcessor getMessageProcessor() {
        return messageProcessor;
    }

    public void setMessageProcessor(MessageProcessor newMessageProcessor) {
        messageProcessor = newMessageProcessor;
    }

}
