import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import io.github.dsheirer.source.config.SourceConfiguration;

public class Test {
    public static String getPreferredTuner(Channel channel) {
        SourceConfiguration mSourceConfiguration = channel.getSourceConfiguration();
        if(mSourceConfiguration instanceof SourceConfigTuner)
            return ((SourceConfigTuner)mSourceConfiguration).getPreferredTuner();
        else if(mSourceConfiguration instanceof SourceConfigTunerMultipleFrequency)
            return ((SourceConfigTunerMultipleFrequency)mSourceConfiguration).getPreferredTuner();
        return null;
    }
}
