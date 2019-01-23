package noc.nsync.connect.ssh;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import net.sf.expectit.Expect;
import net.sf.expectit.ExpectBuilder;
import net.sf.expectit.MultiResult;
import net.sf.expectit.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static net.sf.expectit.filter.Filters.removeColors;
import static net.sf.expectit.matcher.Matchers.anyOf;
import static net.sf.expectit.matcher.Matchers.contains;

public class SshClientAPI {
    private static final Logger log = LoggerFactory.getLogger(SshClientAPI.class);
    private String osPrompt;
    private String ALLIP_Output;
    private Session session;
    private Channel channel;
    private Expect expect ;
    private List<String> errorMessages;

    public SshClientAPI(AlarmSourceConnectorConfig config) throws IOException {
        errorMessages = new ArrayList<>();

        final JSch ssh = new JSch();
        if (!config.nodeAuthUsernameConfig.isEmpty() && !config.nodeHostConfig.isEmpty()) {
            try {
                session = ssh.getSession(config.nodeAuthUsernameConfig, config.nodeHostConfig, config.nodePortConfig);
            } catch (JSchException e) {
                errorMessages.add("Could not create ssh session. " + e.getMessage());
                return;
            } catch (Exception e) {
                errorMessages.add(e.getMessage());
                return;
            }

            assert session != null;
            session.setPassword(config.nodeAuthPasswordConfig);

            Properties prop = new Properties();
            prop.put("StrictHostKeyChecking", "no");
            prop.put("PreferredAuthentications", "publickey,keyboard-interactive,password");
            prop.put("UseDNS", "no");
            session.setConfig(prop);

            try {
                session.connect(30_000);
            } catch (JSchException e) {
                errorMessages.add(String.format("Error when connecting to host: %s. %s", config.nodeHostConfig, e.getMessage()));
                return;
            }

            log.info(config.nodeHostConfig + " is connected. ");

            try {
                channel = session.openChannel("shell");
            } catch (JSchException e) {
                errorMessages.add("Unable to open shell channel. " + e.getMessage());
                disconnect();
                return;
            } catch (Exception e) {
                errorMessages.add("Unexpected error. " + e.getMessage());
                disconnect();
                return;
            }
        }

        expect = new ExpectBuilder()
                .withTimeout(config.expectTimeOutConfig, TimeUnit.SECONDS)
                .withOutput(Objects.requireNonNull(channel).getOutputStream())
                .withInputs(channel.getInputStream(), channel.getExtInputStream())
                // register filters to remove ANSI color characters
                .withInputFilters(removeColors())
                .build();
        try {
            channel.connect();
            // define the command line prompt
            MultiResult prompt_result = expect.expect(anyOf(contains(">"), contains("$"), contains("#")));
            if (prompt_result.getResults().get(0).isSuccessful()) {
                osPrompt = ">";
            } else if (prompt_result.getResults().get(1).isSuccessful()) {
                osPrompt = "$";
            } else if (prompt_result.getResults().get(2).isSuccessful()) {
                osPrompt = "#";
            }

            expect.send("mml -a\r");
            expect.expect(contains("<"));
            expect.send("allip;\r");
            Result expect1 = expect.expect(contains("<"));
            ALLIP_Output = expect1.getBefore();
            expect.send("\u0004"); //Ctrl D
        } catch (JSchException e) {
            errorMessages.add(e.getMessage());
            disconnect();
        }
    }

    void disconnect() {
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

    boolean isConnected() { return channel != null && channel.isConnected(); }

    String getErrorMessages() { return String.join(System.lineSeparator(), errorMessages); }

    String getAllip() { return ALLIP_Output; }

    Expect getExpect() { return expect; }

    String getOsPrompt() { return osPrompt; }
}
