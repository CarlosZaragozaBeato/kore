package com.zensyra.ccollector.core.resource.nutrition;

import com.zensyra.ccollector.core.dto.nutrition.RecipeDTO;
import com.zensyra.ccollector.core.dto.nutrition.RecipeRequest;
import com.zensyra.ccollector.core.dto.response.ResponseDTO;
import com.zensyra.ccollector.core.service.nutrition.RecipeService;
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

@Path("/recipes")
@Produces(MediaType.APPLICATION_JSON)
public class RecipeResource {

    private final RecipeService recipes;
    private final CurrentSession session;

    public RecipeResource(RecipeService recipes, CurrentSession session) {
        this.recipes = recipes;
        this.session = session;
    }

    @GET
    public ResponseDTO<List<RecipeDTO>> list() {
        return ResponseDTO.ok(recipes.list(session.requireUserId()));
    }

    @GET
    @Path("/{id}")
    public ResponseDTO<RecipeDTO> get(@PathParam("id") Long id) {
        return ResponseDTO.ok(recipes.get(session.requireUserId(), id));
    }

    /** Crea una receta (o "importa" una generada por un agente). */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseDTO<RecipeDTO> create(RecipeRequest request) {
        return ResponseDTO.ok(recipes.create(session.requireUserId(), request));
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseDTO<RecipeDTO> update(@PathParam("id") Long id, RecipeRequest request) {
        return ResponseDTO.ok(recipes.update(session.requireUserId(), id, request));
    }

    @DELETE
    @Path("/{id}")
    public ResponseDTO<Void> delete(@PathParam("id") Long id) {
        recipes.delete(session.requireUserId(), id);
        return ResponseDTO.ok(null);
    }
}
