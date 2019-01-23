package noc.nsync.connect.ssh.validators;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;

public class TimeoutValidator implements ConfigDef.Validator {
    @Override
    public void ensureValid(String name, Object value) {
        Long seconds = (Long) value;

        if (!(5 <= seconds)){
            throw new ConfigException(name, value, "value must be >= 5");
        }
    }
}
