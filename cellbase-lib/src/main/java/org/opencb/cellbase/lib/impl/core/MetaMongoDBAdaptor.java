/*
 * Copyright 2015-2020 OpenCB
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

package org.opencb.cellbase.lib.impl.core;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.codehaus.jackson.map.ObjectMapper;
import org.opencb.cellbase.core.api.query.AbstractQuery;
import org.opencb.cellbase.core.api.query.ProjectionQueryOptions;
import org.opencb.cellbase.core.exception.CellBaseException;
import org.opencb.cellbase.core.result.CellBaseDataResult;
import org.opencb.cellbase.core.token.Quota;
import org.opencb.cellbase.lib.iterator.CellBaseIterator;
import org.opencb.commons.datastore.core.FacetField;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.commons.datastore.mongodb.MongoDBCollection;
import org.opencb.commons.datastore.mongodb.MongoDataStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by fjlopez on 07/06/16.
 */
@Deprecated
public class MetaMongoDBAdaptor extends MongoDBAdaptor implements CellBaseCoreDBAdaptor {

    private MongoDBCollection mongoDBCollection;
    private MongoDBCollection quotaMongoDBCollection;

    public MetaMongoDBAdaptor(MongoDataStore mongoDataStore) {
        super(mongoDataStore);

        init();
    }


    private void init() {
        logger.debug("MetaMongoDBAdaptor: in 'constructor'");
        mongoDBCollection = mongoDataStore.getCollection("metadata");
        quotaMongoDBCollection = mongoDataStore.getCollection("quota");
    }

    public CellBaseDataResult getAll() {
        return new CellBaseDataResult<>(mongoDBCollection.find(new BsonDocument(), new QueryOptions()));
    }

    @Override
    public CellBaseDataResult query(AbstractQuery query) {
        return new CellBaseDataResult<>(mongoDBCollection.find(new BsonDocument(), null));
    }

    @Override
    public List<CellBaseDataResult> query(List queries) {
        return null;
    }

    @Override
    public CellBaseIterator iterator(AbstractQuery query) {
        return null;
    }


    @Override
    public CellBaseDataResult<Long> count(AbstractQuery query) {
        return null;
    }

    @Override
    public CellBaseDataResult<String> distinct(AbstractQuery query) {
        return null;
    }

    @Override
    public List<CellBaseDataResult> info(List ids, ProjectionQueryOptions queryOptions, int dataRelease, String token) {
        return null;
    }

    @Override
    public CellBaseDataResult<FacetField> aggregationStats(AbstractQuery query) {
        return null;
    }

    @Override
    public CellBaseDataResult groupBy(AbstractQuery query) {
        return null;
    }

    public CellBaseDataResult getQuota(String token, String date) {
        List<Bson> andBsonList = new ArrayList<>();
        andBsonList.add(Filters.eq("token", token));
        andBsonList.add(Filters.eq("date", date));
        Bson query = Filters.and(andBsonList);

        return new CellBaseDataResult<>(quotaMongoDBCollection.find(query, null, Quota.class, QueryOptions.empty()));
    }

    public CellBaseDataResult initQuota(String token, String date) throws CellBaseException {
        try {
            Quota quota = new Quota(token, date, 0);
            Document document = Document.parse(new ObjectMapper().writeValueAsString(quota));
            return new CellBaseDataResult<>(quotaMongoDBCollection.insert(document, QueryOptions.empty()));
        } catch (IOException e) {
            throw new CellBaseException("Error initializing quota for token '" + token.substring(0, 10) + "...': " + e.getMessage());
        }
    }

    public CellBaseDataResult update(String token, String date, long value) {
        List<Bson> andBsonList = new ArrayList<>();
        andBsonList.add(Filters.eq("token", token));
        andBsonList.add(Filters.eq("date", date));
        Bson query = Filters.and(andBsonList);

        Document projection = new Document("numQueries", true);
        Bson update = Updates.set("numQueries", value);
        QueryOptions queryOptions = new QueryOptions("replace", true);
        System.out.println("query = " + query);
        System.out.println("update = " + update);
        return new CellBaseDataResult<>(quotaMongoDBCollection.findAndUpdate(query, projection, null, update, Quota.class, queryOptions));
    }
}
