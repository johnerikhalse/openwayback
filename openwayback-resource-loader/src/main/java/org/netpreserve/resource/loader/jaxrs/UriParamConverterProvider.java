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

package org.netpreserve.resource.loader.jaxrs;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import org.netpreserve.commons.uri.Uri;
import org.netpreserve.commons.uri.UriBuilder;

/**
 *
 */
@Provider
public class UriParamConverterProvider implements ParamConverterProvider {
    private static final ParamConverter CONVERTER = new ParamConverter<Uri>() {
            @Override
            public Uri fromString(String value) {
                return UriBuilder.strictUriBuilder().uri(value).build();
            }

            @Override
            public String toString(Uri value) {
                return value.toString();
            }
        };

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (rawType != Uri.class) {
            return null;
        }

        return CONVERTER;
    }

}
