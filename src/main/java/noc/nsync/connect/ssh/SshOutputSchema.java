package noc.nsync.connect.ssh;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;


public class SshOutputSchema {
    public static String NODE_ID_FIELD = "node_id";
    public static String OUTPUT_FIELD = "ssh_output";
    public static String MODIFIED_AT_FIELD = "modified_at";

    // Schema names
    public static String SCHEMA_KEY = "output_key";
    public static String SCHEMA_VALUE = "output_value";

    // Key Schema
    public static Schema KEY_SCHEMA = SchemaBuilder.struct().name(SCHEMA_KEY)
            .version(1)
            .field(NODE_ID_FIELD, Schema.STRING_SCHEMA)
            .build();

    // value Schema
    public static Schema VALUE_SCHEMA = SchemaBuilder.struct().name(SCHEMA_VALUE)
            .version(1)
            .field(OUTPUT_FIELD, Schema.STRING_SCHEMA)
            .build();
}
