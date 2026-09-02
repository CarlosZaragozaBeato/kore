package com.zensyra.ccollector.core.resource.nutrition;

import com.zensyra.ccollector.core.dto.catalog.SeedResult;
import com.zensyra.ccollector.core.dto.nutrition.IngredientDTO;
import com.zensyra.ccollector.core.dto.nutrition.IngredientRequest;
import com.zensyra.ccollector.core.dto.response.ResponseDTO;
import com.zensyra.ccollector.core.service.nutrition.IngredientService;
import com.zensyra.ccollector.core.session.CurrentSession;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/ingredients")
@Produces(MediaType.APPLICATION_JSON)
public class IngredientResource {

    private final IngredientService ingredients;
    private final CurrentSession session;

    public IngredientResource(IngredientService ingredients, CurrentSession session) {
        this.ingredients = ingredients;
        this.session = session;
    }

    @GET
    public ResponseDTO<List<IngredientDTO>> list() {
        Long userId = session.requireUserId();
        return ResponseDTO.ok(ingredients.list(userId).stream().map(IngredientDTO::from).toList());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseDTO<IngredientDTO> create(IngredientRequest request) {
        return ResponseDTO.ok(IngredientDTO.from(ingredients.create(session.requireUserId(), request)));
    }

    /** Siembra el catálogo con ingredientes comunes de ejemplo. */
    @POST
    @Path("/seed")
    public ResponseDTO<SeedResult> seed() {
        return ResponseDTO.ok(ingredients.seed(session.requireUserId()));
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseDTO<IngredientDTO> update(@PathParam("id") Long id, IngredientRequest request) {
        return ResponseDTO.ok(IngredientDTO.from(ingredients.update(session.requireUserId(), id, request)));
    }

    @DELETE
    @Path("/{id}")
    public ResponseDTO<Void> delete(@PathParam("id") Long id) {
        ingredients.delete(session.requireUserId(), id);
        return ResponseDTO.ok(null);
    }
}
