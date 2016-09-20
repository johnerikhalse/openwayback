/*
 * Copyright 2016 The International Internet Preservation Consortium.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.netpreserve.resource.loader.resourcestore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import org.jwat.common.HeaderLine;
import org.jwat.warc.WarcRecord;
import org.netpreserve.resource.loader.ResourceResponse;

/**
 *
 */
public class JwatWarcRecordResourceResponse implements ResourceResponse {

    private final WarcRecord record;

    private final InputStream in;

    public JwatWarcRecordResourceResponse(WarcRecord record, InputStream in) {
        this.record = record;
        this.in = in;
    }

    @Override
    public void writePayload(OutputStream output) {
        try {
            InputStream is = record.getPayload().getInputStream();
            int n;
            byte[] buffer = new byte[1024];
            while ((n = is.read(buffer)) > -1) {
                output.write(buffer, 0, n);   // Don't allow any extra bytes to creep in, final write
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public String getRecordType() {
        return record.header.warcTypeStr;
    }

    @Override
    public String getContentType() {
        return record.getHttpHeader().contentType;
    }

    @Override
    public String getTargetUri() {
        return record.header.warcTargetUriStr;
    }

    @Override
    public int getStatus() {
        return record.getHttpHeader().statusCode;
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        MultivaluedMap<String, String> originalHeaders = new MultivaluedHashMap<>();

        for (HeaderLine h : record.getHttpHeader().getHeaderList()) {
            originalHeaders.add(h.name, h.value);
            for (HeaderLine h2 : h.lines) {
                originalHeaders.add(h2.name, h2.value);
            }
        }
        return originalHeaders;
    }

    @Override
    public void close() {
        try {
            record.close();
            in.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
