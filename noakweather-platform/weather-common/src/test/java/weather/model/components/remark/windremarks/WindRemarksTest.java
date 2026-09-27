package weather.model.components.remark.windremarks;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WindRemarksTest {

    @Test
    @DisplayName("empty() should have all fields null/empty")
    void testEmpty() {
        WindRemarks remarks = WindRemarks.empty();

        assertThat(remarks.peakWind()).isNull();
        assertThat(remarks.windShift()).isNull();
        assertThat(remarks.windsAtLocation()).isEmpty();
        assertThat(remarks.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Should default null windsAtLocation to empty list")
    void testConstructor_NullWindsAtLocation() {
        WindRemarks remarks = new WindRemarks(null, null, null);

        assertThat(remarks.windsAtLocation()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should be non-empty when only windShift is present")
    void testIsEmpty_OnlyWindShift() {
        WindShift windShift = new WindShift(15, 30, false);
        WindRemarks remarks = new WindRemarks(null, windShift, null);

        assertThat(remarks.isEmpty()).isFalse();
        assertThat(remarks.peakWind()).isNull();
        assertThat(remarks.windShift()).isEqualTo(windShift);
    }

    @Test
    @DisplayName("Should defensively copy windsAtLocation list")
    void testDefensiveCopy() {
        List<WindAtLocation> mutable = new java.util.ArrayList<>();
        mutable.add(WindAtLocation.atAltitude(1400, weather.model.components.Wind.of(230, 10, "KT")));

        WindRemarks remarks = new WindRemarks(null, null, mutable);
        mutable.add(WindAtLocation.atRunway("26", weather.model.components.Wind.of(0, 0, "KT")));

        assertThat(remarks.windsAtLocation()).hasSize(1);
    }
}
