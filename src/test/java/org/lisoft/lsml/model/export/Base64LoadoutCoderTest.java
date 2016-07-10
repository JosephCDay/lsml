/*
 * @formatter:off
 * Li Song Mechlab - A 'mech building tool for PGI's MechWarrior: Online.
 * Copyright (C) 2013  Li Song
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
//@formatter:on
package org.lisoft.lsml.model.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URLEncoder;
import java.nio.charset.Charset;

import org.junit.Test;
import org.lisoft.lsml.model.loadout.Loadout;
import org.lisoft.lsml.model.loadout.LoadoutStandard;
import org.lisoft.lsml.util.Base64;
import org.lisoft.lsml.util.DecodingException;
import org.lisoft.lsml.util.EncodingException;

/**
 * Test suite for {@link Base64LoadoutCoder}.
 *
 * @author Li Song
 */
/**
 * @author Li Song
 *
 */
public class Base64LoadoutCoderTest {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private final LoadoutCoderV1 coderV1 = mock(LoadoutCoderV1.class);
    private final LoadoutCoderV2 coderV2 = mock(LoadoutCoderV2.class);
    private final LoadoutCoderV3 coderV3 = mock(LoadoutCoderV3.class);
    private final Base64 base64 = mock(Base64.class);
    private final Loadout loadout = mock(Loadout.class);
    private final Base64LoadoutCoder cut = new Base64LoadoutCoder(base64, coderV1, coderV2, coderV3);

    @Test
    public void testEncodeHTTPTrampoline() throws EncodingException {
        // Setup
        final byte[] encodedRaw = "data".getBytes(UTF8);
        final char[] encodedBase64 = "base/64=".toCharArray();
        when(coderV3.encode(loadout)).thenReturn(encodedRaw);
        when(base64.encode(encodedRaw)).thenReturn(encodedBase64);

        // Execute
        final String ans = cut.encodeHTTPTrampoline(loadout);

        // Verify
        assertEquals("http://t.li-soft.org/?l=base%2F64%3D", ans);
    }

    @Test(expected = RuntimeException.class)
    public void testEncodeHTTPTrampolineEncodingError() throws EncodingException {
        // Setup
        when(coderV3.encode(loadout)).thenThrow(new EncodingException("Couldn't encode loadout!"));

        cut.encodeHTTPTrampoline(loadout);
    }

    @Test
    public void testEncodeLSML() throws EncodingException {
        // Setup
        final byte[] encodedRaw = "data".getBytes(UTF8);
        final char[] encodedBase64 = "base/64=".toCharArray();
        when(coderV3.encode(loadout)).thenReturn(encodedRaw);
        when(base64.encode(encodedRaw)).thenReturn(encodedBase64);

        // Execute
        final String ans = cut.encodeLSML(loadout);

        // Verify
        assertEquals("lsml://" + String.copyValueOf(encodedBase64), ans);
    }

    @Test(expected = RuntimeException.class)
    public void testEncodeLSMLEncodingError() throws EncodingException {
        // Setup
        when(coderV3.encode(loadout)).thenThrow(new EncodingException("Couldn't encode loadout!"));

        cut.encodeLSML(loadout);
    }

    @Test
    public void testParseHTTPV1() throws Exception {
        final char[] data = "base/64=".toCharArray();
        final byte[] bitStream = "rawdata".getBytes();
        final String url = "http://t.li-soft.org/?l=" + URLEncoder.encode(String.copyValueOf(data), "UTF-8");
        verifyParseV1(data, bitStream, url);
    }

    @Test
    public void testParseHTTPV2() throws Exception {
        final char[] data = "base/64=".toCharArray();
        final byte[] bitStream = "rawdata".getBytes();
        final String url = "http://t.li-soft.org/?l=" + URLEncoder.encode(String.copyValueOf(data), "UTF-8");
        final LoadoutStandard loadoutStd = mock(LoadoutStandard.class);

        verifyParseV2(data, bitStream, url, loadoutStd);
    }

    @Test
    public void testParseHTTPV3() throws Exception {
        final char[] data = "base/64=".toCharArray();
        final byte[] bitStream = "rawdata".getBytes();
        final String url = "http://t.li-soft.org/?l=" + URLEncoder.encode(String.copyValueOf(data), "UTF-8");

        verifyParseV3(data, bitStream, url);
    }

    @Test(expected = DecodingException.class)
    public void testParseLSMLRubbish() throws Exception {
        final char[] data = "base/64=".toCharArray();
        final byte[] bitStream = "rawdata".getBytes();
        final String url = "lsml://" + String.copyValueOf(data);
        when(base64.decode(data)).thenReturn(bitStream);
        // All decoders return false on "canDecode"

        cut.parse(url);
    }

    @Test
    public void testParseLSMLV1() throws Exception {
        final char[] data = "base/64=".toCharArray();
        final byte[] bitStream = "rawdata".getBytes();
        final String url = "lsml://" + String.copyValueOf(data);
        verifyParseV1(data, bitStream, url);
    }

    @Test
    public void testParseLSMLV2() throws Exception {
        final char[] data = "base/64=".toCharArray();
        final byte[] bitStream = "rawdata".getBytes();
        final String url = "lsml://" + String.copyValueOf(data);
        final LoadoutStandard loadoutStd = mock(LoadoutStandard.class);

        verifyParseV2(data, bitStream, url, loadoutStd);
    }

    @Test
    public void testParseLSMLV3() throws Exception {
        final char[] data = "base/64=".toCharArray();
        final byte[] bitStream = "rawdata".getBytes();
        final String url = "lsml://" + String.copyValueOf(data);

        verifyParseV3(data, bitStream, url);
    }

    @Test(expected = DecodingException.class)
    public void testParseRawRubbishInValidBase64() throws Exception {
        final char[] data = "12base/64=".toCharArray();
        final byte[] bitStream = "rawdata".getBytes();
        final String url = String.copyValueOf(data);
        when(base64.decode(data)).thenReturn(bitStream);
        // All decoders return false on "canDecode"

        cut.parse(url);
    }

    @Test(expected = DecodingException.class)
    public void testParseRawRubbishValidBase64() throws Exception {
        final char[] data = "1234base/64=".toCharArray();
        final byte[] bitStream = "rawdata".getBytes();
        final String url = String.copyValueOf(data);
        when(base64.decode(data)).thenReturn(bitStream);
        // All decoders return false on "canDecode"

        cut.parse(url);
    }

    /**
     * Passing in a string without a protocol identifier is assumed to be LSML format with protocol identifier stripped.
     * Test for V1 links.
     */
    @Test
    public void testParseRawV1() throws Exception {
        final char[] data = "base/64=".toCharArray();
        final byte[] bitStream = "rawdata".getBytes();
        final String url = String.copyValueOf(data);
        verifyParseV1(data, bitStream, url);
    }

    /**
     * Trailing slashes shall be stripped.
     */
    @Test
    public void testParseRawV1TrailingSlash() throws Exception {
        final char[] data = "base/64=".toCharArray();
        final byte[] bitStream = "rawdata".getBytes();
        final String url = String.copyValueOf(data) + "//";
        verifyParseV1(data, bitStream, url);
    }

    /**
     * Passing in a string without a protocol identifier is assumed to be LSML format with protocol identifier stripped.
     * Test for V2 links.
     */
    @Test
    public void testParseRawV2() throws Exception {
        final char[] data = "base/64=".toCharArray();
        final byte[] bitStream = "rawdata".getBytes();
        final String url = String.copyValueOf(data);
        final LoadoutStandard loadoutStd = mock(LoadoutStandard.class);

        verifyParseV2(data, bitStream, url, loadoutStd);
    }

    /**
     * Passing in a string without a protocol identifier is assumed to be LSML format with protocol identifier stripped.
     * Test for V3 links.
     */
    @Test
    public void testParseRawV3() throws Exception {
        final char[] data = "base/64=".toCharArray();
        final byte[] bitStream = "rawdata".getBytes();
        final String url = String.copyValueOf(data);

        verifyParseV3(data, bitStream, url);
    }

    private void verifyParseV1(final char[] data, final byte[] bitStream, final String url)
            throws DecodingException, Exception {
        final LoadoutStandard loadoutStd = mock(LoadoutStandard.class);

        when(base64.decode(data)).thenReturn(bitStream);
        when(coderV1.canDecode(bitStream)).thenReturn(true);
        when(coderV1.decode(bitStream)).thenReturn(loadoutStd);

        final Loadout ans = cut.parse(url);

        assertSame(loadoutStd, ans);
    }

    private void verifyParseV2(final char[] data, final byte[] bitStream, final String url,
            final LoadoutStandard loadoutStd) throws DecodingException, Exception {
        when(base64.decode(data)).thenReturn(bitStream);
        when(coderV2.canDecode(bitStream)).thenReturn(true);
        when(coderV2.decode(bitStream)).thenReturn(loadoutStd);

        final Loadout ans = cut.parse(url);

        assertSame(loadoutStd, ans);
    }

    private void verifyParseV3(final char[] data, final byte[] bitStream, final String url)
            throws DecodingException, Exception {
        when(base64.decode(data)).thenReturn(bitStream);
        when(coderV3.canDecode(bitStream)).thenReturn(true);
        when(coderV3.decode(bitStream)).thenReturn(loadout);

        final Loadout ans = cut.parse(url);

        assertSame(loadout, ans);
    }
}
