package noc.nsync.connect.ssh;

import com.jcraft.jsch.*;

import net.sf.expectit.Expect;
import net.sf.expectit.ExpectBuilder;
import net.sf.expectit.Result;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static net.sf.expectit.filter.Filters.removeColors;
import static net.sf.expectit.filter.Filters.removeNonPrintable;
import static net.sf.expectit.matcher.Matchers.regexp;
import static net.sf.expectit.matcher.Matchers.sequence;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SshClientAPI {
    private static final Logger log = LoggerFactory.getLogger(SshClientAPI.class);

    private Session session;
    public Channel channel;
    private List<String> errorMessages;

    AlarmSourceConnectorConfig config;

    public SshClientAPI(AlarmSourceConnectorConfig config) {
        this.config = config;
        errorMessages = new ArrayList<>();
        initConnection();
    }

    private void initConnection() {
        final JSch ssh = new JSch();
        if (!config.nodeAuthUsernameConfig.isEmpty() && !config.nodeHostConfig.isEmpty()) {
            try {
                session = ssh.getSession(config.nodeAuthUsernameConfig, config.nodeHostConfig, config.nodePortConfig);
            } catch (JSchException e) {
                //TODO:fetch new IPAddress
                errorMessages.add(e.getMessage());
                log.error("Could not create ssh session", e);
            } catch (Exception e) {
                errorMessages.add(e.getMessage());
                //e.printStackTrace();
            }

            session.setPassword(config.nodeAuthPasswordConfig);

            Properties properties = new Properties();
            properties.put("PreferredAuthentications", "publickey,keyboard-interactive,password");
            properties.put("StrictHostKeyChecking", "no");
            properties.put("UseDNS", "no");
            session.setConfig(properties);

            try {
                session.connect(30_000);
            } catch (JSchException e) {
                if (e.getMessage().contentEquals("Auth fail")) {
                    try {
                        //TODO: Fetch AAA
                        session.connect(30_000);
                    } catch (JSchException re) {
                        errorMessages.add(re.getMessage());
                        disconnect();
                        log.error("Could not establish ssh connection", e);
                        //e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                errorMessages.add(e.getMessage());
                //e.printStackTrace();
            }
            log.info("Connected to host: " + config.nodeHostConfig);

            try {
                channel = session.openChannel("shell");
                log.info("Shell channel opened. ");
                //channel.connect(60_000);
            } catch (JSchException e) {
                disconnect();
                log.error("Could not open shell channel", e);
                errorMessages.add(e.getMessage());
                //e.printStackTrace();
            } catch (Exception e) {
                log.error("Unexpected error !", e);
                errorMessages.add(e.getMessage());
                //e.printStackTrace();
            }
        }
    }

    public void disconnect() {
        try {
            if (channel != null)
                channel.disconnect();
        } finally {
            channel = null;
            try {
                if (session != null)
                    session.disconnect();
            } finally {
                session = null;
            }
        }
    }

}
