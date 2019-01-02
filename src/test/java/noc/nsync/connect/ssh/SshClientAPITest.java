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

import static net.sf.expectit.filter.Filters.removeColors;
import static net.sf.expectit.filter.Filters.removeNonPrintable;
import static net.sf.expectit.matcher.Matchers.*;
import static noc.nsync.connect.ssh.AlarmSourceConnectorConfig.*;
import static org.junit.jupiter.api.Assertions.*;

class SshClientAPITest {
    private Map<String, String> initConfig() {
        Map<String, String> baseProps = new HashMap<>();
        baseProps.put(NODE_HOST_CONFIG, "host");
        baseProps.put(NODE_AUTH_USERNAME_CONFIG, "user");
        baseProps.put(NODE_AUTH_PASSWORD_CONFIG, "pwd");

        return baseProps;
    }

    @Test
    void initConnection() throws IOException {
        SshClientAPI client = new SshClientAPI(new AlarmSourceConnectorConfig(initConfig()));

//        Expect expect = new ExpectBuilder()
//                .withOutput(client.channel.getOutputStream())
//                .withInputs(client.channel.getInputStream(), client.channel.getExtInputStream())
//                .withEchoInput(System.out)
//                .withEchoOutput(System.err)
//                .withInputFilters(removeColors(), removeNonPrintable())
//                .withExceptionOnFailure()
//                .build();
        try {
            Expect expect = new ExpectBuilder()
                    .withOutput(client.channel.getOutputStream())
                    .withInputs(client.channel.getInputStream(), client.channel.getExtInputStream())
                    // register filters to remove ANSI color characters
                    .withInputFilters(removeColors())
                    .build();
            try {
                client.channel.connect(60_000);
                // define the command line prompt
                final String PROMPT = "$";
                expect.expect(contains(PROMPT));
                expect.sendLine("man ls");

                while (true) {
                    // expect either the end of the page or the end of the command
                    MultiResult result = expect.expect(anyOf(contains("Manual"), contains("(END)"), contains(PROMPT)));
                    // print the result
                    System.out.println(result.getBefore());
                    // exit if reach the end
                    if (result.getResults().get(1).isSuccessful()) {
                        break;
                    }
                    // scroll to the next page
                    expect.send(" ");
                }
//            expect.sendLine("mml -a");
//            Result mmlOutput = expect.expect(eof());
//            System.out.println(mmlOutput.getInput());
            } catch (JSchException e) {
                e.printStackTrace();
            } finally {
                expect.close();
                client.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}