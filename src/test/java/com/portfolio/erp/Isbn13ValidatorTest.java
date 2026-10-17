package com.portfolio.erp;

import com.portfolio.erp.common.Isbn13Validator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;

class Isbn13ValidatorTest {
    private final Isbn13Validator validator = new Isbn13Validator();
    @ParameterizedTest @NullSource @ValueSource(strings = {"9780000000002", "9790000000001", "9780306406157"})
    void validOrOptionalIsbn(String isbn) { assertThat(validator.isValid(isbn, null)).isTrue(); }
    @ParameterizedTest @ValueSource(strings = {"9780000000001", "1234567890128", "978-0306406157", "", "abcdefghijklm", "978030640615"})
    void malformedOrBadChecksum(String isbn) { assertThat(validator.isValid(isbn, null)).isFalse(); }
}
