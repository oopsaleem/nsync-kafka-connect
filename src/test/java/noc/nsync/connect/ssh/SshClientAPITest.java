package noc.nsync.connect.ssh;

import net.sf.expectit.MultiResult;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.sf.expectit.matcher.Matchers.anyOf;
import static net.sf.expectit.matcher.Matchers.contains;
import static noc.nsync.connect.ssh.AlarmSourceConnectorConfig.*;

class SshClientAPITest {
    private Map<String, String> initConfig() {
        Map<String, String> baseProps = new HashMap<>();
        baseProps.put(NODE_HOST_CONFIG, "remote");
        baseProps.put(NODE_AUTH_USERNAME_CONFIG, "user");
        baseProps.put(NODE_AUTH_PASSWORD_CONFIG, "something");
        baseProps.put(EXPECT_TIMEOUT_SEC_CONFIG, "5");

        return baseProps;
    }

    @Disabled
    void Constructor() {
        try {
            SshClientAPI client = new SshClientAPI(new AlarmSourceConnectorConfig(initConfig()));
            String clientErrorMessages = client.getErrorMessages();
            if(!clientErrorMessages.isEmpty()) {
                System.out.println(clientErrorMessages);
            }

            assert  clientErrorMessages.isEmpty();

            System.out.println(client.getAllip());

            while (true) {
                if (client.isConnected()) {
                    System.out.println(DateTime.now());
                    MultiResult result = client.getExpect().expect(anyOf(contains("HB\r\n\u0004"), contains("END\r\n\u0004"), contains(client.getOsPrompt())));
                    if (result.getResults().get(0).isSuccessful())
                        System.out.print(result.getBefore() + "HB\r\n");

                    if (result.getResults().get(1).isSuccessful()) {
                        System.out.println(result.getBefore() + "END\r\n");
                        System.out.print("================");
                    }

                    if (result.getResults().get(2).isSuccessful()) {
                        //BACK in OS prompt
                        break;
                    }
                } else {
                    System.out.println(DateTime.now());
                    System.out.println("DISCONNECTED !!");
                }
            }
            client.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.printf(e.getMessage());
        }
    }

    @Disabled
    void execute() {
        SshClient ssh = new SshClient("remote", "user", "something");

        List<String> commandsToExecute = new ArrayList<String>();
        commandsToExecute.add("mml");
        commandsToExecute.add("allip;");
        String outputLog = ssh.execute(commandsToExecute);
        System.out.println(outputLog);
    }
}