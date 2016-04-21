package ru.cti.verintsipbyehandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import ru.cti.verintsipbyehandler.controller.CallParser;
import ru.cti.verintsipbyehandler.controller.dao.DAOFacade;
import ru.cti.verintsipbyehandler.model.fabric.CallsFabric;

import javax.sip.InvalidArgumentException;
import javax.sip.ObjectInUseException;
import javax.sip.PeerUnavailableException;
import javax.sip.TransportNotSupportedException;
import javax.sql.DataSource;
import java.net.UnknownHostException;
import java.util.TooManyListenersException;

/**
 * Spring Java configuration file
 */
@org.springframework.context.annotation.Configuration
@PropertySources({ @PropertySource(value = "file:etc/config.properties")})
public class Configuration {
    @Autowired
    Environment env;

    @Bean
    public Main main() {
        return new Main(Integer.parseInt(env.getProperty("applicationClosingTimer")));
    }

    @Bean
    public ParseAndProcessCalls parseAndProcessCalls() throws Exception {
        return new ParseAndProcessCalls(env.getProperty("regexp"),
                env.getProperty("risLogsFolderPath"),
                Integer.parseInt(env.getProperty("callTerminationTimeout")),
                Integer.parseInt(env.getProperty("completedCallDeletionTimer")),
                Integer.parseInt(env.getProperty("sipByeSenderPause")));
    }

    @Bean
    public SipLayer sipLayer() throws InvalidArgumentException, PeerUnavailableException, UnknownHostException,
            TransportNotSupportedException, ObjectInUseException, TooManyListenersException {
        return new SipLayer(env.getProperty("sip.username"),
                env.getProperty("srcIpAddress"),
                Integer.parseInt(env.getProperty("sip.srcPort")),
                env.getProperty("sip.destinationAddress"),
                env.getProperty("sipLibraryLogLevel"));
    }

    @Bean
    public CallsFabric callsFabric() {
        return new CallsFabric();
    }

    @Bean
    public DataSource dataSource() {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(org.sqlite.JDBC.class);
        dataSource.setUrl("jdbc:sqlite:db/sqlite.db");
        return dataSource;
    }

    @Bean
    public DAOFacade daoFacade() {
        return new DAOFacade(dataSource());
    }

    @Bean
    public CallParser callParser() {
        return new CallParser(env.getProperty("risLogsFolderPath"),
                env.getProperty("regexp"));
    }
}
