package com.gongsoop.global.converter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class YesNoBooleanConverterTest {

    private final YesNoBooleanConverter converter = new YesNoBooleanConverter();

    @Test
    void convertsBooleanToOracleYesNo() {
        assertThat(converter.convertToDatabaseColumn(true)).isEqualTo("Y");
        assertThat(converter.convertToDatabaseColumn(false)).isEqualTo("N");
        assertThat(converter.convertToDatabaseColumn(null)).isEqualTo("N");
    }

    @Test
    void convertsOracleYesNoToBoolean() {
        assertThat(converter.convertToEntityAttribute("Y")).isTrue();
        assertThat(converter.convertToEntityAttribute("N")).isFalse();
        assertThat(converter.convertToEntityAttribute(null)).isFalse();
    }
}
