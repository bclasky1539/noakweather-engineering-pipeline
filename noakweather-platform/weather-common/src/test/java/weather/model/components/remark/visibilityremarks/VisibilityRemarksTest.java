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

    @Test
    @DisplayName("withTowerVisibility should replace tower visibility and preserve others")
    void testWithTowerVisibility() {
        Visibility surface = Visibility.statuteMiles(0.5);
        Visibility tower = Visibility.statuteMiles(1.5);
        VisibilityRemarks original = new VisibilityRemarks(null, surface, null);

        VisibilityRemarks updated = original.withTowerVisibility(tower);

        assertThat(updated.towerVisibility()).isEqualTo(tower);
        assertThat(updated.surfaceVisibility()).isEqualTo(surface);
        assertThat(original.towerVisibility()).isNull();
    }

    @Test
    @DisplayName("withSurfaceVisibility should replace surface visibility and preserve others")
    void testWithSurfaceVisibility() {
        Visibility tower = Visibility.statuteMiles(1.5);
        Visibility surface = Visibility.statuteMiles(0.5);
        VisibilityRemarks original = new VisibilityRemarks(tower, null, null);

        VisibilityRemarks updated = original.withSurfaceVisibility(surface);

        assertThat(updated.surfaceVisibility()).isEqualTo(surface);
        assertThat(updated.towerVisibility()).isEqualTo(tower);
    }

    @Test
    @DisplayName("withVariableVisibility should replace variable visibility and preserve others")
    void testWithVariableVisibility() {
        Visibility tower = Visibility.statuteMiles(1.5);
        VariableVisibility variable = VariableVisibility.of(
                Visibility.statuteMiles(0.5), Visibility.statuteMiles(2.0));
        VisibilityRemarks original = new VisibilityRemarks(tower, null, null);

        VisibilityRemarks updated = original.withVariableVisibility(variable);

        assertThat(updated.variableVisibility()).isEqualTo(variable);
        assertThat(updated.towerVisibility()).isEqualTo(tower);
        assertThat(original.variableVisibility()).isNull();
    }
}
