package ru.cti.verintsipbyehandler.model.factory;

import ru.cti.verintsipbyehandler.model.domainobjects.Call;

public class CallsFactory extends DomainEntitiesFactory<Call> {

    public CallsFactory() {
    }

    @Override
    public Call create() {
        return new Call(-1, "s", 3, false);
    }

    public Call create(String callId) {
        return new Call(-1, callId, System.currentTimeMillis(), false);
    }

    public Call create(int id, String callId) {
        return new Call(id, callId, System.currentTimeMillis(), false);
    }
}
