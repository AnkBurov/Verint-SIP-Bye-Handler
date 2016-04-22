package ru.cti.verintsipbyehandler.model.factory;

import ru.cti.verintsipbyehandler.model.domainobjects.DomainEntity;

public abstract class DomainEntitiesFactory<T extends DomainEntity> {

    public DomainEntitiesFactory() {
    }

    public abstract T create();
}
