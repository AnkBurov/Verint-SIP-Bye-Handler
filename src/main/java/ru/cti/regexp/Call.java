package ru.cti.regexp;

/**
 * Created by e.karpov on 25.02.2016.
 */
public class Call {
    private long time;
    private String callId;

    //todo временный конструктор для спринг контекста. Переделать
    public Call() {
    }

    public Call(long time, String callId) {
        this.time = time;
        this.callId = callId;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Call call = (Call) o;

        return callId.equals(call.callId);

    }

    @Override
    public int hashCode() {
        return callId.hashCode();
    }
}
