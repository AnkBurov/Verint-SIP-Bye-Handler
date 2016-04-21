package ru.cti.verintsipbyehandler.model.fabric;

import ru.cti.verintsipbyehandler.model.domainobjects.Call;

public class CallsFabric extends DomainEntitiesFabric<Call> {

    public CallsFabric() {
    }

    @Override
    public Call create() {
        return new Call(-1, "s", 3, false);
    }

    public Call create(String callId) {
        return new Call(-1, callId, System.currentTimeMillis(), true);
    }

    public Call create(int id, String callId) {
        return new Call(id, callId, System.currentTimeMillis(), false);
    }
}
