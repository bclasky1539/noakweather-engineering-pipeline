package weather.model.components.remark.visibilityremarks;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import weather.model.components.Visibility;

import static org.assertj.core.api.Assertions.assertThat;

class VisibilityRemarksTest {

    @Test
    @DisplayName("empty() should have all fields null")
    void testEmpty() {
        VisibilityRemarks remarks = VisibilityRemarks.empty();

        assertThat(remarks.towerVisibility()).isNull();
        assertThat(remarks.surfaceVisibility()).isNull();
        assertThat(remarks.variableVisibility()).isNull();
        assertThat(remarks.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Should be non-empty when only towerVisibility is present")
    void testIsEmpty_OnlyTowerVisibility() {
        Visibility towerVis = Visibility.statuteMiles(1.5);
        VisibilityRemarks remarks = new VisibilityRemarks(towerVis, null, null);

        assertThat(remarks.isEmpty()).isFalse();
        assertThat(remarks.towerVisibility()).isEqualTo(towerVis);
    }
}
