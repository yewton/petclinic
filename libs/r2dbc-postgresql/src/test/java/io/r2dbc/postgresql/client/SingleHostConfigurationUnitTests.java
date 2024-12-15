/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.r2dbc.postgresql.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link SingleHostConfiguration}.
 */
final class SingleHostConfigurationUnitTests {

    @Test
    void builderNoHostAndSocket() {
        assertThatIllegalArgumentException().isThrownBy(() -> SingleHostConfiguration.builder().build())
            .withMessage("host or socket must not be null");
    }

    @Test
    void builderHostAndSocket() {
        assertThatIllegalArgumentException().isThrownBy(() -> SingleHostConfiguration.builder().host("host").socket("socket").build())
            .withMessageContaining("either host/port or socket");
    }
}
