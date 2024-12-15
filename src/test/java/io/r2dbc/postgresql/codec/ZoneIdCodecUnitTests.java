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

import java.time.ZoneId;

import static io.r2dbc.postgresql.client.EncodedParameter.NULL_VALUE;
import static io.r2dbc.postgresql.client.ParameterAssert.assertThat;
import static io.r2dbc.postgresql.codec.PostgresqlObjectId.BPCHAR;
import static io.r2dbc.postgresql.codec.PostgresqlObjectId.MONEY;
import static io.r2dbc.postgresql.codec.PostgresqlObjectId.VARCHAR;
import static io.r2dbc.postgresql.message.Format.FORMAT_BINARY;
import static io.r2dbc.postgresql.message.Format.FORMAT_TEXT;
import static io.r2dbc.postgresql.util.ByteBufUtils.encode;
import static io.r2dbc.postgresql.util.TestByteBufAllocator.TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link ZoneIdCodec}.
 */
final class ZoneIdCodecUnitTests {

    private static final int dataType = VARCHAR.getObjectId();

    @Test
    void constructorNoByteBufAllocator() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ZoneIdCodec(null))
            .withMessage("byteBufAllocator must not be null");
    }

    @Test
    void decode() {
        ZoneId zoneId = ZoneId.systemDefault();

        assertThat(new ZoneIdCodec(TEST).decode(encode(TEST, zoneId.getId()), dataType, FORMAT_TEXT, ZoneId.class))
            .isEqualTo(zoneId);
    }

    @Test
    void decodeNoByteBuf() {
        assertThat(new ZoneIdCodec(TEST).decode(null, dataType, FORMAT_TEXT, ZoneId.class)).isNull();
    }

    @Test
    void doCanDecode() {
        ZoneIdCodec codec = new ZoneIdCodec(TEST);

        assertThat(codec.doCanDecode(VARCHAR, FORMAT_BINARY)).isTrue();
        assertThat(codec.doCanDecode(MONEY, FORMAT_TEXT)).isFalse();
        assertThat(codec.doCanDecode(BPCHAR, FORMAT_TEXT)).isTrue();
        assertThat(codec.doCanDecode(VARCHAR, FORMAT_TEXT)).isTrue();
    }

    @Test
    void doCanDecodeNoFormat() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ZoneIdCodec(TEST).doCanDecode(VARCHAR, null))
            .withMessage("format must not be null");
    }

    @Test
    void doCanDecodeNoType() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ZoneIdCodec(TEST).doCanDecode(null, FORMAT_TEXT))
            .withMessage("type must not be null");
    }

    @Test
    void doEncode() {
        ZoneId zoneId = ZoneId.systemDefault();

        assertThat(new ZoneIdCodec(TEST).doEncode(zoneId))
            .hasFormat(FORMAT_TEXT)
            .hasType(VARCHAR.getObjectId())
            .hasValue(encode(TEST, zoneId.getId()));
    }

    @Test
    void doEncodeNoValue() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ZoneIdCodec(TEST).doEncode(null))
            .withMessage("value must not be null");
    }

    @Test
    void encodeNull() {
        assertThat(new ZoneIdCodec(TEST).encodeNull())
            .isEqualTo(new EncodedParameter(FORMAT_TEXT, VARCHAR.getObjectId(), NULL_VALUE));
    }

}
