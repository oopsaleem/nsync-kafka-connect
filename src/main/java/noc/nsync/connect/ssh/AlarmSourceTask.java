package noc.nsync.connect.ssh;

import com.github.jcustenborder.kafka.connect.utils.VersionUtil;
import net.sf.expectit.MultiResult;
import noc.nsync.connect.ssh.models.SshOutput;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.sf.expectit.matcher.Matchers.anyOf;
import static net.sf.expectit.matcher.Matchers.contains;
import static noc.nsync.connect.ssh.SshOutputSchema.*;

public class AlarmSourceTask extends SourceTask {
    private static final Logger log = LoggerFactory.getLogger(AlarmSourceTask.class);
    public AlarmSourceConnectorConfig config;
    private SshClientAPI client;

    @Override
    public String version() {
        return VersionUtil.version(this.getClass());
    }

    @Override
    public void start(Map<String, String> map) {
        //TODO: Do things here that are required to start your task. This could be open a connection to a database, etc.
        config = new AlarmSourceConnectorConfig(map);

        try {
            client = new SshClientAPI(config);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private boolean initTake = false;

    @Override
    public List<SourceRecord> poll() throws InterruptedException {
        final ArrayList<SourceRecord> records = new ArrayList<>();

        if(initTake) {
            SourceRecord sourceRecord = generateSourceRecord(new SshOutput(config.nodeHostConfig, client.getAllip(), Instant.now()));
            records.add(sourceRecord);
            initTake = true;
        }

        if (client.isConnected()) {
            MultiResult result = null;
            try {
                result = client.getExpect().expect(anyOf(contains("HB\r\n\u0004"), contains("END\r\n\u0004"), contains(client.getOsPrompt())));
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }

            if (result.getResults().get(0).isSuccessful()) {
                //Plot, HB
                String output = result.getBefore() + "HB\r\n";
                SourceRecord sourceRecord = generateSourceRecord(new SshOutput(config.nodeHostConfig, output, Instant.now()));
                records.add(sourceRecord);
            }

            if (result.getResults().get(1).isSuccessful()) {
                //Plot the interesting stuff
                String output = result.getBefore() + "END\r\n";
                SourceRecord sourceRecord = generateSourceRecord(new SshOutput(config.nodeHostConfig, output, Instant.now()));
                records.add(sourceRecord);
            }

            if (result.getResults().get(2).isSuccessful()) {
                //TODO: Flag, I AM BACK in OS prompt
                log.info("I AM BACK in OS prompt!");
                String output = result.getBefore() + client.getOsPrompt();
                SourceRecord sourceRecord = generateSourceRecord(new SshOutput(config.nodeHostConfig, output, Instant.now()));
                records.add(sourceRecord);
                Thread.sleep(10 * 1_000);
            }
        } else {
            //TODO: DISCONNECTED, Recover connection
            log.info("NODE DISCONNECTED !");
            Thread.sleep(60 * 1_000);
            return null;
        }

        return records;
    }

    private SourceRecord generateSourceRecord(SshOutput sshOutput) {
        return new SourceRecord(
                sourcePartition(),
                sourceOffset(),
                config.topicConfig,
                null, // partition will be inferred by the framework
                SshOutputSchema.KEY_SCHEMA,
                new Struct(KEY_SCHEMA).put(NODE_ID_FIELD, sshOutput.getNodeId()),
                SshOutputSchema.VALUE_SCHEMA,
                new Struct(VALUE_SCHEMA).put(OUTPUT_FIELD, sshOutput.getOutput()),
                sshOutput.getModifiedAt().toEpochMilli());
    }

    private Map<String, String> sourcePartition() {
        Map<String, String> map = new HashMap<>();
        map.put(NODE_ID_FIELD, String.valueOf(config.nodeHostConfig));
        return map;
    }

    private Map<String, Long> sourceOffset() {
        Map<String, Long> map = new HashMap<>();
        map.put("instant", Instant.now().toEpochMilli());
        return map;
    }

    @Override
    public void stop() {
        //Do whatever is required to stop your task.
    }
}