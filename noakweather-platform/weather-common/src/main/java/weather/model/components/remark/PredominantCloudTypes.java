package weather.model.components.remark;

import weather.model.enums.HighCloudType;
import weather.model.enums.LowCloudType;
import weather.model.enums.MiddleCloudType;

import java.util.Objects;

/**
 * Predominant cloud type remark ("8/C_L C_M C_H"), reporting the predominant
 * low, middle, and high cloud type per WMO Cloud Atlas coding instructions.
 * <p>
 * Example: "8/87/" (NSTU) → low=CUMULUS_AND_STRATOCUMULUS_DIFFERENT_LEVELS,
 * middle=ALTOCUMULUS_MULTI_LEVEL_OR_WITH_ALTOSTRATUS_OR_OPACUS, high=OBSCURED
 * (the high layer could not be observed due to an overcast layer below —
 * consistent with this METAR's own OVC100 in the main body)
 *
 * @param lowCloud    predominant low cloud type, never null once parsed
 * @param middleCloud predominant middle cloud type, never null once parsed
 * @param highCloud   predominant high cloud type, never null once parsed
 * @author bclasky1539
 */
public record PredominantCloudTypes(
        LowCloudType lowCloud,
        MiddleCloudType middleCloud,
        HighCloudType highCloud
) {

    /**
     * Compact constructor with validation.
     */
    public PredominantCloudTypes {
        Objects.requireNonNull(lowCloud, "Low cloud type cannot be null");
        Objects.requireNonNull(middleCloud, "Middle cloud type cannot be null");
        Objects.requireNonNull(highCloud, "High cloud type cannot be null");
    }

    /**
     * @return a human-readable summary combining all three layers
     */
    public String getSummary() {
        return "Low: " + lowCloud.getSummary()
                + "; Middle: " + middleCloud.getSummary()
                + "; High: " + highCloud.getSummary();
    }
}
