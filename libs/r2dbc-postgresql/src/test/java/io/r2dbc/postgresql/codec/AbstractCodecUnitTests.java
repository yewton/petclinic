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

package io.r2dbc.postgresql.codec;

import io.r2dbc.postgresql.client.EncodedParameter;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static io.r2dbc.postgresql.client.EncodedParameter.NULL_VALUE;
import static io.r2dbc.postgresql.client.ParameterAssert.assertThat;
import static io.r2dbc.postgresql.codec.PostgresqlObjectId.INT4;
import static io.r2dbc.postgresql.codec.PostgresqlObjectId.VARCHAR;
import static io.r2dbc.postgresql.message.Format.FORMAT_BINARY;
import static io.r2dbc.postgresql.message.Format.FORMAT_TEXT;
import static io.r2dbc.postgresql.util.TestByteBufAllocator.TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link AbstractCodec}.
 */
final class AbstractCodecUnitTests {

    @Test
    void canDecode() {
        MockCodec<String> codec = MockCodec.builder(String.class)
            .canDecode(FORMAT_BINARY, VARCHAR)
            .build();

        assertThat(codec.canDecode(VARCHAR.getObjectId(), FORMAT_BINARY, String.class)).isTrue();
        assertThat(codec.canDecode(VARCHAR.getObjectId(), FORMAT_BINARY, Void.class)).isFalse();
    }

    @Test
    void canDecodeNoFormat() {
        assertThatIllegalArgumentException().isThrownBy(() -> MockCodec.empty(String.class).canDecode(100, null, String.class))
            .withMessage("format must not be null");
    }

    @Test
    void canDecodeNoType() {
        assertThatIllegalArgumentException().isThrownBy(() -> MockCodec.empty(String.class).canDecode(100, FORMAT_BINARY, null))
            .withMessage("type must not be null");
    }

    @Test
    void canEncode() {
        assertThat(MockCodec.empty(String.class).canEncode("")).isTrue();
        assertThat(MockCodec.empty(String.class).canEncode(new Object())).isFalse();
    }

    @Test
    void canEncodeNoValue() {
        assertThatIllegalArgumentException().isThrownBy(() -> MockCodec.empty(String.class).canEncode(null))
            .withMessage("value must not be null");
    }

    @Test
    void canEncodeNull() {
        assertThatIllegalArgumentException().isThrownBy(() -> MockCodec.empty(String.class).canEncodeNull(null))
            .withMessage("type must not be null");
    }

    @Test
    void canEncodeNullNoValue() {
        assertThat(MockCodec.empty(String.class).canEncodeNull(String.class)).isTrue();
        assertThat(MockCodec.empty(String.class).canEncodeNull(Void.class)).isFalse();
    }

    @Test
    void constructorNoType() {
        assertThatIllegalArgumentException().isThrownBy(() -> MockCodec.empty(null))
            .withMessage("type must not be null");
    }

    @Test
    void create() {
        EncodedParameter parameter = AbstractCodec.create(FORMAT_TEXT, INT4, Flux.just(TEST.buffer(4).writeInt(100)));

        assertThat(parameter)
            .hasFormat(FORMAT_TEXT)
            .hasType(INT4.getObjectId())
            .hasValue(TEST.buffer(4).writeInt(100));
    }

    @Test
    void createNoFormat() {
        assertThatIllegalArgumentException().isThrownBy(() -> AbstractCodec.create(null, INT4, Mono.empty()))
            .withMessage("format must not be null");
    }

    @Test
    void createNull() {
        EncodedParameter parameter = AbstractCodec.createNull(FORMAT_TEXT, INT4);

        assertThat(parameter).isEqualTo(new EncodedParameter(FORMAT_TEXT, INT4.getObjectId(), NULL_VALUE));
    }

    @Test
    void createNullNoFormat() {
        assertThatIllegalArgumentException().isThrownBy(() -> AbstractCodec.createNull(null, INT4))
            .withMessage("format must not be null");
    }

    @Test
    void encode() {
        EncodedParameter parameter = new EncodedParameter(FORMAT_TEXT, INT4.getObjectId(), Flux.just(TEST.buffer(4).writeInt(100)));
        Object value = new Object();

        MockCodec<Object> codec = MockCodec.builder(Object.class)
            .encoding(value, parameter)
            .build();

        assertThat(codec.doEncode(value)).isSameAs(parameter);
    }

    @Test
    void encodeNoValue() {
        assertThatIllegalArgumentException().isThrownBy(() -> MockCodec.empty(Object.class).encode(null))
            .withMessage("value must not be null");
    }

}
