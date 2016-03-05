package ru.cti.verintsipbyehandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;

import javax.sip.InvalidArgumentException;
import javax.sip.ObjectInUseException;
import javax.sip.PeerUnavailableException;
import javax.sip.TransportNotSupportedException;
import java.net.UnknownHostException;
import java.util.TooManyListenersException;

/**
 * Spring Java configuration file
 */
@org.springframework.context.annotation.Configuration
@PropertySources({ @PropertySource(value = "config.properties")})
public class Configuration {
    @Autowired
    Environment env;

    @Bean
    public Main main() {
        return new Main();
    }

    @Bean
    public ParseAndProcessCalls parseAndProcessCalls() throws Exception {
        return new ParseAndProcessCalls(env.getProperty("regexp"),
                env.getProperty("risLogsFolderPath"),
                Integer.parseInt(env.getProperty("callTerminationTimeout")),
                Integer.parseInt(env.getProperty("completedCallDeletionTimer")));
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
}
