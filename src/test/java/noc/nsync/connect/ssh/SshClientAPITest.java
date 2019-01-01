package noc.nsync.connect.ssh;

import net.sf.expectit.Expect;
import net.sf.expectit.ExpectBuilder;
import net.sf.expectit.Result;
import org.apache.kafka.common.config.ConfigDef;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static net.sf.expectit.filter.Filters.removeColors;
import static net.sf.expectit.filter.Filters.removeNonPrintable;
import static net.sf.expectit.matcher.Matchers.*;
import static noc.nsync.connect.ssh.AlarmSourceConnectorConfig.*;
import static org.junit.jupiter.api.Assertions.*;

class SshClientAPITest {
    ConfigDef configDef = AlarmSourceConnectorConfig.config();

    private Map<String, String> initConfig() {
        Map<String, String>  baseProps = new HashMap<>();
        baseProps.put(NODE_HOST_CONFIG, "host");
        baseProps.put(NODE_AUTH_USERNAME_CONFIG, "user");
        baseProps.put(NODE_AUTH_PASSWORD_CONFIG, "pwd");

        return baseProps;
    }

    @Test
    void initConnection() throws IOException {
        SshClientAPI client = new SshClientAPI(new AlarmSourceConnectorConfig(initConfig()));

        Expect expect = new ExpectBuilder()
                .withOutput(client.channel.getOutputStream())
                .withInputs(client.channel.getInputStream(), client.channel.getExtInputStream())
                .withEchoInput(System.out)
                .withEchoOutput(System.err)
                .withInputFilters(removeColors(), removeNonPrintable())
                .withExceptionOnFailure()
                .build();
        try {
            //expect.expect(regexp("> "));
            expect.sendLine("mml -a");
            expect.expect(sequence(regexp(".+"), regexp("<")));
            expect.sendLine("ALLIP;");
            Result allipOutput = expect.expect(sequence(regexp(".+"), regexp("<")));
            System.out.println(allipOutput.getInput());
            expect.send("\u0004"); //Ctrl D

            expect.expect(anyOf(regexp("HB"), regexp("END")));
            //expect.expect(contains("[RETURN]"));
            //expect.sendLine();
            //String ipAddress = expect.expect(regexp("Trying (.*)\\.\\.\\.")).group(1);
            //System.out.println("Captured IP: " + ipAddress);
            //expect.expect(contains("login:"));
            //expect.sendLine("testuser");
            //expect.expect(contains("(Y/N)"));
            //expect.send("N");
            //expect.expect(regexp(": $"));
        } finally {
            expect.close();
            client.disconnect();
        }

    }
}