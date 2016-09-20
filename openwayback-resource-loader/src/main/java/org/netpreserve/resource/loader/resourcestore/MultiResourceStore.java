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
import java.util.ArrayList;
import java.util.List;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;
import org.netpreserve.commons.uri.Uri;
import org.netpreserve.resource.loader.ResourceResponse;
import org.netpreserve.resource.loader.ResourceStore;
import rx.Observable;
import rx.Subscriber;

/**
 *
 */
public class MultiResourceStore implements ResourceStore {

    List<ResourceStore> resourceStores = new ArrayList<>();

    @Override
    public ResourceResponse getResource(Uri resourceRef) throws IOException {
        Observable<ResourceResponse> observable = null;
        for (ResourceStore store : resourceStores) {
            System.out.println(store);
            Observable<ResourceResponse> so = new GetResourceCommand(store, resourceRef).observe();
            if (observable == null) {
                observable = so;
            } else {
                observable.mergeWith(so);
            }
        }
        return observable.toBlocking().firstOrDefault(null);
    }

    public void addResourceStore(ResourceStore resourceStore) {
        resourceStores.add(resourceStore);
    }

    class GetResourceCommand extends HystrixObservableCommand<ResourceResponse> {

        private final ResourceStore resourceStore;

        private final Uri resourceRef;

        public GetResourceCommand(ResourceStore resourceStore, Uri resourceRef) {
            super(HystrixCommandGroupKey.Factory.asKey("ResourceStore"));
            this.resourceStore = resourceStore;
            this.resourceRef = resourceRef;
        }

        @Override
        protected Observable<ResourceResponse> construct() {
            return Observable.create(new Observable.OnSubscribe<ResourceResponse>() {
                @Override
                public void call(Subscriber<? super ResourceResponse> observer) {
                    try {
                        if (!observer.isUnsubscribed()) {
                            ResourceResponse response = resourceStore.getResource(resourceRef);
                            if (response != null) {
                                observer.onNext(response);
                            }
                            observer.onCompleted();
                        }
                    } catch (Exception ex) {
                        observer.onError(ex);
                    }
                }
            });
        }

    }

}
