package noc.nsync.connect.ssh.validators;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;

public class PortValidator implements ConfigDef.Validator {
    @Override
    public void ensureValid(String name, Object value) {
        Integer port = (Integer) value;

        if (!(1 <= port && port <= 32_767)){
            throw new ConfigException(name, value, "Port number must be between 1 and 32767");
        }
    }
}

