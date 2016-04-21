package ru.cti.verintsipbyehandler.controller.dao;

import javax.sql.DataSource;

public class DAOFacade {
    private DataSource dataSource;

    public DAOFacade(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public CallDAO getCallDAO() {
        return new CallDAO(dataSource);
    }
}
