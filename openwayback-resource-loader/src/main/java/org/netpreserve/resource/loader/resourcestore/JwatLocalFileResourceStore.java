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
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.jwat.common.RandomAccessFileInputStream;
import org.jwat.warc.WarcReaderCompressed;
import org.jwat.warc.WarcReaderFactory;
import org.jwat.warc.WarcRecord;
import org.netpreserve.commons.uri.Uri;
import org.netpreserve.resource.loader.ResourceResponse;
import org.netpreserve.resource.loader.ResourceStore;

/**
 *
 */
public class JwatLocalFileResourceStore implements ResourceStore {

    private final Path directory;

    public JwatLocalFileResourceStore(String directory) {
        this.directory = Paths.get(directory);
    }

    @Override
    public ResourceResponse getResource(Uri resourceRef) throws IOException {
        System.out.println("Searching for " + resourceRef + " in " + directory);
        Path path = directory.resolve(resourceRef.getDecodedPath());
        if (Files.isDirectory(path) || !Files.isReadable(path)) {
            System.out.println("Nothing found in " + directory);
            return null;
        }

//        InputStream in = Files.newInputStream(path);
//        RandomAccessFile file = new RandomAccessFile(path.toFile(), "r");
        Long offset = Long.parseLong(resourceRef.getFragment());
        RandomAccessFileInputStream in = new RandomAccessFileInputStream(new RandomAccessFile(path.toFile(), "r"));

        in.skip(offset);

        WarcReaderCompressed reader = WarcReaderFactory.getReaderCompressed();
        WarcRecord record = reader.getNextRecordFrom(in, offset);

        return new JwatWarcRecordResourceResponse(record, in);
    }

    @Override
    public String toString() {
        return "JwatLocalFileResourceStore{" + "directory=" + directory + '}';
    }

}
