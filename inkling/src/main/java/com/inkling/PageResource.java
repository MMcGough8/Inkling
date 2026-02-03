package com.inkling;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Serves the frontend pages.
 */
@Path("/")
public class PageResource {

    @Inject
    Template index;

    /**
     * Serve the main application page.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index() {
        return index.instance();
    }
}
