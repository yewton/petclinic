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

import java.time.LocalDateTime;
import java.time.ZoneId;

import static io.r2dbc.postgresql.client.EncodedParameter.NULL_VALUE;
import static io.r2dbc.postgresql.client.ParameterAssert.assertThat;
import static io.r2dbc.postgresql.codec.PostgresqlObjectId.MONEY;
import static io.r2dbc.postgresql.codec.PostgresqlObjectId.TIMESTAMP;
import static io.r2dbc.postgresql.codec.PostgresqlObjectId.VARCHAR;
import static io.r2dbc.postgresql.message.Format.FORMAT_BINARY;
import static io.r2dbc.postgresql.message.Format.FORMAT_TEXT;
import static io.r2dbc.postgresql.util.ByteBufUtils.encode;
import static io.r2dbc.postgresql.util.TestByteBufAllocator.TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link LocalDateTimeCodec}.
 */
final class LocalDateTimeCodecUnitTests {

    private static final int dataType = TIMESTAMP.getObjectId();

    private final LocalDateTimeCodec codec = new LocalDateTimeCodec(TEST, ZoneId::systemDefault);

    @Test
    void constructorNoByteBufAllocator() {
        assertThatIllegalArgumentException().isThrownBy(() -> new LocalDateTimeCodec(null, null))
            .withMessage("byteBufAllocator must not be null");
    }

    @Test
    void decode() {
        LocalDateTime localDateTime = LocalDateTime.parse("2018-11-05T00:06:31.700426");

        assertThat(this.codec.decode(encode(TEST, "2018-11-05 00:06:31.700426"), dataType, FORMAT_TEXT, LocalDateTime.class))
            .isEqualTo(localDateTime);
    }

    @Test
    void decodeUTC() {
        LocalDateTime localDateTime = LocalDateTime.parse("2018-11-05T00:06:31.700426");

        assertThat(this.codec.decode(encode(TEST, "2018-11-05 00:06:31.700426+00:00"), dataType, FORMAT_TEXT, LocalDateTime.class))
            .isEqualTo(localDateTime);
        assertThat(this.codec.decode(encode(TEST, "2018-11-05 00:06:31.700426+00"), dataType, FORMAT_TEXT, LocalDateTime.class))
            .isEqualTo(localDateTime);
        assertThat(this.codec.decode(encode(TEST, "2018-11-05 00:06:31.700426+00:00:00"), dataType, FORMAT_TEXT, LocalDateTime.class))
            .isEqualTo(localDateTime);
    }

    @Test
    void decodeNoByteBuf() {
        assertThat(this.codec.decode(null, dataType, FORMAT_TEXT, LocalDateTime.class)).isNull();
    }

    @Test
    void doCanDecode() {
        LocalDateTimeCodec codec = this.codec;

        assertThat(codec.doCanDecode(TIMESTAMP, FORMAT_BINARY)).isTrue();
        assertThat(codec.doCanDecode(MONEY, FORMAT_TEXT)).isFalse();
        assertThat(codec.doCanDecode(TIMESTAMP, FORMAT_TEXT)).isTrue();
    }

    @Test
    void doCanDecodeNoFormat() {
        assertThatIllegalArgumentException().isThrownBy(() -> this.codec.doCanDecode(VARCHAR, null))
            .withMessage("format must not be null");
    }

    @Test
    void doCanDecodeNoType() {
        assertThatIllegalArgumentException().isThrownBy(() -> this.codec.doCanDecode(null, FORMAT_TEXT))
            .withMessage("type must not be null");
    }

    @Test
    void doEncode() {
        LocalDateTime localDateTime = LocalDateTime.parse("2023-02-16T15:13:46.23");

        assertThat(this.codec.doEncode(localDateTime))
            .hasFormat(FORMAT_TEXT)
            .hasType(TIMESTAMP.getObjectId())
            .hasValue(encode(TEST, "2023-02-16 15:13:46.23"));
    }

    @Test
    void doEncodeNoValue() {
        assertThatIllegalArgumentException().isThrownBy(() -> this.codec.doEncode(null))
            .withMessage("value must not be null");
    }

    @Test
    void encodeNull() {
        assertThat(this.codec.encodeNull())
            .isEqualTo(new EncodedParameter(FORMAT_TEXT, TIMESTAMP.getObjectId(), NULL_VALUE));
    }

}
