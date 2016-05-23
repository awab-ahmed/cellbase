package org.opencb.cellbase.client.rest;

import org.opencb.biodata.models.variant.Variant;
import org.opencb.cellbase.client.config.ClientConfiguration;
import org.opencb.commons.datastore.core.Query;
import org.opencb.commons.datastore.core.QueryResponse;

import java.io.IOException;

/**
 * Created by swaathi on 23/05/16.
 */
public class VariationClient extends FeatureClient<Variant> {

    public VariationClient(ClientConfiguration configuration) {
        super(configuration);
        this.clazz = Variant.class;

        this.category = "feature";
        this.subcategory = "variation";
    }

    public QueryResponse<Variant> getNextSnp(String id) throws IOException {
        return execute(id, "next", null, Variant.class);
    }

    public QueryResponse<String> getAllConsequenceTypes(Query query) throws IOException {
        return execute("consequence_types", query, String.class);
    }

    public QueryResponse<String> getConsequenceTypeById(String id, Query query) throws IOException {
        return execute(id, "consequence_type", query, String.class);
    }
}
