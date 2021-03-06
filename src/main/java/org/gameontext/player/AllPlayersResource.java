/*******************************************************************************
 * Copyright (c) 2015 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.gameontext.player;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.time.temporal.ChronoUnit;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.ektorp.CouchDbConnector;
import org.ektorp.ViewQuery;
import org.gameontext.player.control.PlayerAccountModificationException;
import org.gameontext.player.entity.ErrorResponse;
import org.gameontext.player.entity.PlayerArgument;
import org.gameontext.player.entity.PlayerDbRecord;
import org.gameontext.player.entity.PlayerResponse;
import org.gameontext.player.utils.SharedSecretGenerator;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.opentracing.Traced;

/**
 * All the players, and searching for players.
 *
 */
@Path("/accounts")
@Api( tags = {"players"})
public class AllPlayersResource {
    @Context
    HttpServletRequest httpRequest;

    @Inject
    protected CouchDbConnector db;

    @ConfigProperty(name = "SYSTEM_ID", defaultValue = "x")
    String systemId;

    /**
     * GET /players/v1/accounts
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List all players",
        notes = "Get a list of registered players. Use link headers for pagination.",
        response = PlayerResponse.class,
        responseContainer = "List")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = Messages.SUCCESSFUL, response = PlayerResponse.class),
        @ApiResponse(code = 204, message = Messages.CONFLICT, response=ErrorResponse.class)
    })
    @Fallback(fallbackMethod = "getAllPlayersFallback")
    @Timeout(value = 2, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 2, maxDuration= 10000)
    @Timed(name = "getAllPlayers_timer",
        tags = "label=allPlayersResource")
    @Counted(name = "getAllPlayers_count",
        tags = "label=allPlayersResource")
    @Metered(name = "getAllPlayers_meter",
        tags = "label=allPlayersResource")
    @Traced(value = true, operationName = "getAllPlayers.response")
    public Response getAllPlayers() throws IOException {
        try{
            ViewQuery all = new ViewQuery().designDocId("_design/players").viewName("all").cacheOk(true).includeDocs(true);
            List<PlayerDbRecord> results = db.queryView(all, PlayerDbRecord.class);
            if ( results.isEmpty() )
                return Response.noContent().build();
            else {
                List<PlayerResponse> prs = results.stream()
                        .map(record -> {PlayerResponse pr = new PlayerResponse(record); pr.setCredentials(null); return pr;})
                        .collect(Collectors.toList());

                // TODO -- this should be done better. Stream, something.
                GenericEntity<List<PlayerResponse>> entity = new GenericEntity<List<PlayerResponse>>(prs) {};

                return Response.ok().entity(entity).build();
            }
        }catch(Exception e){
            e.printStackTrace();
            throw e;
        }
    }
    
    public Response getAllPlayersFallback() {
        List<PlayerResponse> prs = new ArrayList<PlayerResponse>();
        PlayerResponse pr = new PlayerResponse();
        pr.setCredentials(null); 
        pr.setName("null");
        pr.setFavoriteColor("null");
        pr.setLocation(null);
        pr.setId("null");
        pr.setRev("null");
        prs.add(pr);
        GenericEntity<List<PlayerResponse>> entity = new GenericEntity<List<PlayerResponse>>(prs) {};
        return Response.ok().entity(entity).build();
    }

    /**
     * POST /players/v1/accounts
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create a new player account",
        notes = "",
        response = PlayerResponse.class,
        code = HttpURLConnection.HTTP_CREATED )
    @ApiResponses(value = {
            @ApiResponse(code = HttpServletResponse.SC_CREATED, message = Messages.SUCCESSFUL, response = PlayerResponse.class),
            @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = "Authenticated user id must match new player id", response=ErrorResponse.class),
            @ApiResponse(code = HttpServletResponse.SC_CONFLICT, message = Messages.CONFLICT, response=ErrorResponse.class)
        })
    @Timed(name = "createPlayer_timer",
        tags = "label=allPlayersResource")
    @Counted(name = "getAllPlayers_count",
        tags = "label=allPlayersResource")
    @Metered(name = "getAllPlayers_meter",
        tags = "label=allPlayersResource")
    @Timeout(value = 2, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 2, maxDuration= 10000)
    public Response createPlayer(PlayerArgument player) throws IOException {

        // set by the auth filter.
        String authId = (String) httpRequest.getAttribute("player.id");

        // only allow create for matching id.
        if (stripSensitiveData(authId, player.getId())) {
            throw new PlayerAccountModificationException(
                    Response.Status.FORBIDDEN,
                    "Player could not be created",
                    "Authenticated id must match new player id");
        }

        PlayerDbRecord pFull = new PlayerDbRecord();
        pFull.update(player);   // get all proposed updates
        
        // make sure an API key is generated for the new user
        pFull.setApiKey(SharedSecretGenerator.generateApiKey());
        
        //add in story/playerMode if they are present in the users auth jwt.
        if (httpRequest.getAttribute("player.story")!=null) {
            pFull.setStory(httpRequest.getAttribute("player.story").toString());
        }
        if (httpRequest.getAttribute("player.playerMode")!=null) {
            pFull.setPlayerMode(httpRequest.getAttribute("player.playerMode").toString());
        }        

        // NOTE: Thrown exceptions are mapped (see ErrorResponseMapper)
        db.create(pFull);

        PlayerResponse pr = new PlayerResponse(pFull);

        return Response.created(URI.create("/players/v1/accounts/" + player.getId())).entity(pr).build();
    }

    private boolean stripSensitiveData(String user, String player) {
        return ( user == null || !(player.equals(user) || systemId.equals(user)) );
    }
}
