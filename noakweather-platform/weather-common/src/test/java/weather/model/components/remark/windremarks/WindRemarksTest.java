package weather.model.components.remark.windremarks;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import weather.model.components.Wind;

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

    @Test
    @DisplayName("withPeakWind should replace peak wind and preserve other fields")
    void testWithPeakWind() {
        WindShift shift = new WindShift(15, 30, false);
        WindAtLocation atAlt = WindAtLocation.atAltitude(1400, Wind.of(230, 10, "KT"));
        PeakWind peak = new PeakWind(280, 32, 15, 30);
        WindRemarks original = new WindRemarks(null, shift, List.of(atAlt));

        WindRemarks updated = original.withPeakWind(peak);

        assertThat(updated.peakWind()).isEqualTo(peak);
        assertThat(updated.windShift()).isEqualTo(shift);
        assertThat(updated.windsAtLocation()).containsExactly(atAlt);
        assertThat(original.peakWind()).isNull();
    }

    @Test
    @DisplayName("withPeakWind(null) should clear peak wind")
    void testWithPeakWind_Null() {
        WindRemarks original = new WindRemarks(new PeakWind(280, 32, 15, 30), null, null);

        assertThat(original.withPeakWind(null).peakWind()).isNull();
    }

    @Test
    @DisplayName("withWindShift should replace wind shift and preserve other fields")
    void testWithWindShift() {
        PeakWind peak = new PeakWind(280, 32, 15, 30);
        WindShift shift = new WindShift(15, 30, true);
        WindRemarks original = new WindRemarks(peak, null, null);

        WindRemarks updated = original.withWindShift(shift);

        assertThat(updated.windShift()).isEqualTo(shift);
        assertThat(updated.peakWind()).isEqualTo(peak);
        assertThat(original.windShift()).isNull();
    }

    @Test
    @DisplayName("withWindsAtLocation should replace the list; null becomes empty")
    void testWithWindsAtLocation() {
        WindAtLocation first = WindAtLocation.atAltitude(1400, Wind.of(230, 10, "KT"));
        WindAtLocation second = WindAtLocation.atRunway("26", Wind.of(0, 0, "KT"));
        WindRemarks original = new WindRemarks(null, null, List.of(first));

        assertThat(original.withWindsAtLocation(List.of(second)).windsAtLocation())
                .containsExactly(second);
        assertThat(original.withWindsAtLocation(null).windsAtLocation()).isEmpty();
    }

    @Test
    @DisplayName("addWindAtLocation should append without mutating the original")
    void testAddWindAtLocation() {
        WindAtLocation first = WindAtLocation.atAltitude(1400, Wind.of(230, 10, "KT"));
        WindAtLocation second = WindAtLocation.atRunway("26", Wind.of(0, 0, "KT"));
        WindRemarks original = new WindRemarks(null, null, List.of(first));

        WindRemarks updated = original.addWindAtLocation(second);

        assertThat(updated.windsAtLocation()).containsExactly(first, second);
        assertThat(original.windsAtLocation()).containsExactly(first);
    }

    @Test
    @DisplayName("addWindAtLocation(null) should return the same instance")
    void testAddWindAtLocation_Null() {
        WindRemarks original = new WindRemarks(null, null, null);

        assertThat(original.addWindAtLocation(null)).isSameAs(original);
    }
}
