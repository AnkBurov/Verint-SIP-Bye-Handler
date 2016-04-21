package ru.cti.verintsipbyehandler.model.domainobjects;

public class Call extends DomainEntity {
    private int id;
    private String callId;
    private long timeOfCall;
    private boolean isEnded; // возможно не нужно

    public Call(int id, String callId, long timeOfCall, boolean isEnded) {
        this.id = id;
        this.callId = callId;
        this.timeOfCall = timeOfCall;
        this.isEnded = isEnded;
    }

    public Call(String callId, long timeOfCall, boolean isEnded) {
        this.callId = callId;
        this.timeOfCall = timeOfCall;
        this.isEnded = isEnded;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public long getTimeOfCall() {
        return timeOfCall;
    }

    public void setTimeOfCall(long timeOfCall) {
        this.timeOfCall = timeOfCall;
    }

    public boolean isEnded() {
        return isEnded;
    }

    public void setEnded(boolean ended) {
        isEnded = ended;
    }
}
