package noc.nsync.connect.ssh;

import com.jcraft.jsch.JSchException;
import net.sf.expectit.Expect;
import net.sf.expectit.ExpectBuilder;
import net.sf.expectit.MultiResult;
import net.sf.expectit.Result;
import org.apache.kafka.common.config.ConfigDef;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static net.sf.expectit.filter.Filters.removeColors;
import static net.sf.expectit.filter.Filters.removeNonPrintable;
import static net.sf.expectit.matcher.Matchers.*;
import static noc.nsync.connect.ssh.AlarmSourceConnectorConfig.*;
import static org.junit.jupiter.api.Assertions.*;

class SshClientAPITest {
    private Map<String, String> initConfig() {
        Map<String, String> baseProps = new HashMap<>();
        baseProps.put(NODE_HOST_CONFIG, "remote");
        baseProps.put(NODE_AUTH_USERNAME_CONFIG, "user");
        baseProps.put(NODE_AUTH_PASSWORD_CONFIG, "pwd");

        return baseProps;
    }

    @Test
    void initConnection() {
        SshClientAPI client = new SshClientAPI(new AlarmSourceConnectorConfig(initConfig()));

        try {
            Expect expect = new ExpectBuilder()
                    .withTimeout(63, TimeUnit.SECONDS)
                    .withOutput(client.channel.getOutputStream())
                    .withInputs(client.channel.getInputStream(), client.channel.getExtInputStream())
                    // register filters to remove ANSI color characters
                    .withInputFilters(removeColors())
                    .build();
            try {
                client.channel.connect();
                // define the command line prompt
                final String PROMPT = ">";
                expect.expect(contains(PROMPT));
                expect.send("mml -a\r");
                expect.expect(contains("<"));
                expect.send("allip;\r");
                Result expect1 = expect.expect(contains("<"));
                System.out.println(expect1.getBefore());
                expect.send("\u0004"); //Ctrl D
                while (true) {
                    // expect either the end of the page or the end of the command
                    MultiResult result = expect.expect(anyOf(contains("HB\r\n\u0004"), contains("END\r\n\u0004"), contains(PROMPT)));

                    if (result.getResults().get(0).isSuccessful()) {
                        //TODO: Flag that I AM ALIVE
                        System.out.print(result.getBefore() + "HB\r\n");
                    }

                    if (result.getResults().get(1).isSuccessful()) {
                        //TODO: Plot the interesting stuff
                        System.out.print(result.getBefore() + "END\r\n");
                        System.out.print("================");
                    }

                    if (result.getResults().get(2).isSuccessful()) {
                        break;
                    }
                    // scroll to the next page
                    //expect.send(" ");
                }
//            expect.sendLine("mml -a");
//            Result mmlOutput = expect.expect(eof());
//            System.out.println(mmlOutput.getInput());
            } catch (JSchException e) {
                e.printStackTrace();
                System.out.printf(e.getMessage());
            } finally {
                expect.close();
                client.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}