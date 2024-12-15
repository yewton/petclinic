/*
 * Copyright 2017 the original author or authors.
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
import reactor.core.publisher.Flux;

import static io.r2dbc.postgresql.client.EncodedParameter.NULL_VALUE;
import static io.r2dbc.postgresql.client.ParameterAssert.assertThat;
import static io.r2dbc.postgresql.message.Format.FORMAT_TEXT;
import static io.r2dbc.postgresql.util.TestByteBufAllocator.TEST;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link EncodedParameter}.
 */
final class ParameterUnitTests {

    @Test
    void constructorNoFormat() {
        assertThatIllegalArgumentException().isThrownBy(() -> new EncodedParameter(null, 100, NULL_VALUE))
            .withMessage("format must not be null");
    }

    @Test
    void constructorNoValue() {
        assertThatIllegalArgumentException().isThrownBy(() -> new EncodedParameter(FORMAT_TEXT, 100, null))
            .withMessage("value must not be null");
    }

    @Test
    void getters() {
        EncodedParameter parameter = new EncodedParameter(FORMAT_TEXT, 100, Flux.just(TEST.buffer(4).writeInt(200)));

        assertThat(parameter)
            .hasFormat(FORMAT_TEXT)
            .hasType(100)
            .hasValue(TEST.buffer(4).writeInt(200));
    }

}
