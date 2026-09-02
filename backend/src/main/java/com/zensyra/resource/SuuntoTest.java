package com.zensyra.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/suunto")
@Produces(MediaType.APPLICATION_JSON)
public class SuuntoTest {
    

    @GET
    public String test() {
        return "Suunto Test";
    }
}
