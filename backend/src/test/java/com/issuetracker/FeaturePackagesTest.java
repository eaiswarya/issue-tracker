package com.issuetracker;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.lang.NonNullApi;

class FeaturePackagesTest {

    @ParameterizedTest
    @ValueSource(strings = {"auth", "project", "ticket", "board", "comment", "notification", "common"})
    void featurePackageExists(String feature) throws ClassNotFoundException {
        Package pkg = Class.forName("com.issuetracker." + feature + ".package-info").getPackage();

        assertThat(pkg.isAnnotationPresent(NonNullApi.class)).isTrue();
    }
}
