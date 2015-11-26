/*
 * Copyright 2015 OpenCB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencb.cellbase.core.api;

import org.opencb.commons.datastore.core.Query;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.commons.datastore.core.QueryResult;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by imedina on 25/11/15.
 */
public interface CellBaseDBAdaptor<T> extends Iterable<T> {

    QueryResult<Long> count();

    QueryResult<Long> count(Query query);


    QueryResult distinct(Query query, String field);


    QueryResult stats();

    QueryResult stats(Query query);


    /*
     Main methods to query.
     */
    QueryResult first();

    QueryResult<T> get(Query query, QueryOptions options);

    List<QueryResult<T>> get(List<Query> queries, QueryOptions options);

    QueryResult nativeGet(Query query, QueryOptions options);

    List<QueryResult> nativeGet(List<Query> queries, QueryOptions options);



    @Override
    Iterator<T> iterator();

    Iterator nativeIiterator();

    Iterator<T> iterator(Query query, QueryOptions options);

    Iterator nativeIterator(Query query, QueryOptions options);


    /*
     Iterator queries
     */
    @Override
    void forEach(Consumer action);

    void forEach(Query query, Consumer<? super Object> action, QueryOptions options);


    /*
     Aggregation queries over a specific query
     */
    QueryResult rank(Query query, String field, int numResults, boolean asc);

    QueryResult groupBy(Query query, String field, QueryOptions options);

    QueryResult groupBy(Query query, List<String> fields, QueryOptions options);

}
