package org.vaadin.tokenfield;

import com.vaadin.server.Sizeable.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * Verifies the sizing overrides ({@code setHeight}/{@code setWidth}/
 * {@code setSizeFull}/{@code setSizeUndefined}) propagate to the internal
 * layout, and the {@code setInput*} family propagates to the embedded
 * ComboBox.
 */
class TokenFieldSizingTest {

    private TestTokenField field;

    @BeforeEach
    void setup() {
        field = new TestTokenField();
    }

    @Test
    void setHeightPropagatesToLayout() {
        field.setHeight(42, Unit.PIXELS);
        assertThat(field.getInternalLayout().getHeight()).isEqualTo(42f);
        assertThat(field.getInternalLayout().getHeightUnits()).isEqualTo(Unit.PIXELS);
    }

    @Test
    void setWidthPropagatesToLayout() {
        field.setWidth(50, Unit.PERCENTAGE);
        assertThat(field.getInternalLayout().getWidth()).isEqualTo(50f);
        assertThat(field.getInternalLayout().getWidthUnits()).isEqualTo(Unit.PERCENTAGE);
    }

    @Test
    void setSizeFullPropagatesToLayout() {
        field.setSizeFull();
        assertThat(field.getInternalLayout().getWidth()).isEqualTo(100f);
        assertThat(field.getInternalLayout().getWidthUnits()).isEqualTo(Unit.PERCENTAGE);
        assertThat(field.getInternalLayout().getHeight()).isEqualTo(100f);
        assertThat(field.getInternalLayout().getHeightUnits()).isEqualTo(Unit.PERCENTAGE);
    }

    @Test
    void setSizeUndefinedPropagatesToLayout() {
        field.setSizeFull();
        field.setSizeUndefined();
        assertThat(field.getInternalLayout().getWidth()).isEqualTo(-1f);
        assertThat(field.getInternalLayout().getHeight()).isEqualTo(-1f);
    }

    @Test
    void setInputHeightStringPropagatesToComboBox() {
        field.setInputHeight("30px");
        assertThat(field.getComboBox().getHeight()).isEqualTo(30f);
        assertThat(field.getComboBox().getHeightUnits()).isEqualTo(Unit.PIXELS);
    }

    @Test
    void setInputWidthStringPropagatesToComboBox() {
        field.setInputWidth("200px");
        assertThat(field.getComboBox().getWidth()).isEqualTo(200f);
        assertThat(field.getComboBox().getWidthUnits()).isEqualTo(Unit.PIXELS);
    }

    @Test
    void setInputHeightFloatUnitPropagatesToComboBox() {
        field.setInputHeight(25, Unit.PIXELS);
        assertThat(field.getComboBox().getHeight()).isEqualTo(25f);
        assertThat(field.getComboBox().getHeightUnits()).isEqualTo(Unit.PIXELS);
    }

    @Test
    void setInputWidthFloatUnitPropagatesToComboBox() {
        field.setInputWidth(75, Unit.PERCENTAGE);
        assertThat(field.getComboBox().getWidth()).isEqualTo(75f);
        assertThat(field.getComboBox().getWidthUnits()).isEqualTo(Unit.PERCENTAGE);
    }

    @Test
    void setInputSizeFullPropagatesToComboBox() {
        field.setInputSizeFull();
        assertThat(field.getComboBox().getWidth()).isEqualTo(100f);
        assertThat(field.getComboBox().getWidthUnits()).isEqualTo(Unit.PERCENTAGE);
    }

    @Test
    void setInputSizeUndefinedPropagatesToComboBox() {
        field.setInputSizeFull();
        field.setInputSizeUndefined();
        assertThat(field.getComboBox().getWidth()).isEqualTo(-1f);
        assertThat(field.getComboBox().getHeight()).isEqualTo(-1f);
    }
}
