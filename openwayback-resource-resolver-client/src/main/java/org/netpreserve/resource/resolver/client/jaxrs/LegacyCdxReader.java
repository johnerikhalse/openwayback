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
package org.netpreserve.resource.resolver.client.jaxrs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import org.netpreserve.commons.cdx.CdxRecord;
import org.netpreserve.commons.cdx.CdxRecordFactory;
import org.netpreserve.commons.cdx.cdxrecord.CdxLine;
import org.netpreserve.commons.cdx.cdxrecord.CdxLineFormat;

/**
 *
 */
@Provider
@Consumes("application/vnd.org.netpreserve.cdx")
public class LegacyCdxReader implements MessageBodyReader<List<CdxLine>> {
    private static final GenericType ACCEPTABLE_TYPE = new GenericType<List<CdxRecord>>() { };

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return genericType.equals(ACCEPTABLE_TYPE.getType());
    }

    @Override
    public List<CdxLine> readFrom(Class<List<CdxLine>> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {

        List<CdxLine> result = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(entityStream))) {
            String line;
            while ((line = in.readLine()) != null) {
                result.add((CdxLine) CdxRecordFactory.create(line, CdxLineFormat.CDX11LINE));
            }
        }

        return result;
    }

}
