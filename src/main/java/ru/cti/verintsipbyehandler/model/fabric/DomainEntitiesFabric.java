package ru.cti.verintsipbyehandler.model.fabric;

import ru.cti.verintsipbyehandler.model.domainobjects.DomainEntity;

public abstract class DomainEntitiesFabric<T extends DomainEntity> {

    public DomainEntitiesFabric() {
    }

    public abstract T create();
}
