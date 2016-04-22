package ru.cti.verintsipbyehandler.controller.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import ru.cti.verintsipbyehandler.model.domainobjects.Call;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CallDAO implements GenericDAO<Call, Integer> {
    JdbcTemplate jdbcTemplate;

    public CallDAO(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void createTable() {
        jdbcTemplate.update("CREATE TABLE IF NOT EXISTS Calls (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "callId TEXT UNIQUE , timeOfCall LONG, isEnded BOOLEAN);");
    }

    @Override
    public int create(Call entity) {
        return jdbcTemplate.update("INSERT INTO Calls VALUES (NULL, ?, ?, ?);", entity.getCallId(), entity.getTimeOfCall(),
                entity.isEnded());
    }

    @Override
    public List<Call> getAll() {
        return jdbcTemplate.query("SELECT * FROM Calls;", new ItemMapper());
    }

    public List<Call> getAllActiveCalls() {
        return jdbcTemplate.query("SELECT * FROM Calls WHERE isEnded = 0;", new ItemMapper());
    }

    public List<Call> getAllCompletedCalls() {
        return jdbcTemplate.query("SELECT * FROM Calls WHERE isEnded = 1;", new ItemMapper());
    }

    @Override
    public Call read(Integer key) {
        return jdbcTemplate.queryForObject("SELECT * FROM Calls where id = ?;", new ItemMapper(), key);
    }

    @Override
    public int update(Call entity) {
        return jdbcTemplate.update("UPDATE Calls SET callId = ?, timeOfCall = ?, isEnded = ? where id = ?;",
                entity.getCallId(), entity.getTimeOfCall(), entity.isEnded(), entity.getId());
    }

    public int updateClosedCall(String callId) {
        return jdbcTemplate.update("UPDATE Calls SET isEnded = 1 WHERE callId = ?;", callId);
    }

    @Override
    public int delete(Integer key) {
        return jdbcTemplate.update("delete FROM Calls where id = ?;", key);
    }
}

//todo сделать внутренним класом
final class ItemMapper implements RowMapper<Call> {
    public Call mapRow(ResultSet rs, int rowNum) throws SQLException {
        Call entity = new Call(rs.getInt(1), rs.getString(2), rs.getLong(3), rs.getBoolean(4));
        return entity;
    }
}
