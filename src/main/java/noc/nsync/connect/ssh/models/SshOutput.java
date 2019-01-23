package noc.nsync.connect.ssh.models;

import org.json.JSONObject;

import java.time.Instant;

import static noc.nsync.connect.ssh.SshOutputSchema.*;

public class SshOutput {
    private String nodeId;
    private String output;
    private Instant modifiedAt;

    /**
     * No args constructor for use in serialization
     */
    public SshOutput() { }

    /**
     * @param nodeId nodeName
     * @param output content of ssh.
     * @param modifiedAt file modified time.
     */
    public SshOutput(String nodeId, String output, Instant modifiedAt) {
        super();

        this.nodeId = nodeId;
        this.output = output;
        this.modifiedAt = modifiedAt;
    }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public SshOutput withNodeId(String nodeId) {
        this.nodeId = nodeId;
        return this;
    }

    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }
    public SshOutput withOutput(String sshOutput) {
        this.output = sshOutput;
        return this;
    }


    public Instant getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(Instant modifiedAt) { this.modifiedAt = modifiedAt; }
    public SshOutput withModifiedAt(Instant modifiedAt) {
        this.modifiedAt = modifiedAt;
        return this;
    }

    public static SshOutput fromJson(JSONObject jsonObject) {
        SshOutput sshOutput = new SshOutput();
        sshOutput.withNodeId(jsonObject.getString(NODE_ID_FIELD));
        sshOutput.withOutput(jsonObject.getString(OUTPUT_FIELD));
        sshOutput.withModifiedAt(Instant.parse(jsonObject.getString(MODIFIED_AT_FIELD)));

        return sshOutput;
    }
}
