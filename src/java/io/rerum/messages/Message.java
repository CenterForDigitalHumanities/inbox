/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.rerum.messages;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.json.JsonObject;
import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonObjectBuilder;
import javax.json.JsonArray;
import javax.json.stream.JsonParser;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.function.Consumer;
import javax.json.JsonValue;

//import net.sf.json.JSONObject;
//import net.sf.json.JSONArray;
import javax.net.ssl.HttpsURLConnection;
import javax.ws.rs.POST;
import javax.ws.rs.core.Response;

/**
 * REST Web Service
 *
 * @author cubap
 */
@Path("id/{noteId}")
public class Message {

    static final String ID_KEY = "@id";
    private final String CONTEXT = "http://www.w3.org/ns/ldp";

    @Context
    private UriInfo context;

    @PathParam("noteId")
    String noteId;

    /**
     * Creates a new instance of Message
     */
    public Message() {
    }

    /**
     * Retrieves representation of an instance of io.rerum.messages.Message
     * proxy to firebase.io. The request looks like:
     * https://rerum-inbox.firebaseio.com/messages/-KopRqrP9czBnzJtieaE.json is
     * https://rerum-inbox.firebaseio.com/messages/{noteId}.json
     *
     * The response is like: { "@type":"Announce",
     * "motivation":"iiif:supplement:range",
     * "object":"http://scta.info/iiif/wodehamordinatio/sorb193/ranges/toc/wrapper",
     * "source":"http://scta.info", "actor":{ "@id":
     * "https://scta.info/#identity, "label": "SCTA" },
     * "target":"https://nubis.univ-paris1.fr/items/presentation/4853/manifest",
     * "published":"2017-03-18 13:12:46 UTC" }
     *
     * and needs an @id and @context constructed for it, if not present
     *
     * @return an instance of java.lang.String
     * @throws java.lang.Exception
     */
    @GET

    @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public String getJson() throws Exception {

        JsonObject note = getMessage();
        return note.toString();
    }

    /**
     * PUT method for updating or creating an instance of Message
     *
     * @param content representation for the resource
     * @return
     */
    @PUT
    @Consumes(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public String putJson(String content) {
        // TODO: PUT content
        return content;
    }

    @POST
    @Consumes(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public String postJson(String content) {
        JsonObject annoucement;
        try (JsonReader reader = Json.createReader(new StringReader(content))) {
            annoucement = reader.readObject();
        }
        // Simple check for parts
        // ERRORS
        if (annoucement.getJsonString("@id").toString().length() > 0) {
            // There should not be one already; these are new annoucements.
            Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity("POST is for new annoucements only.").build();
            return "{\"error\":\"Property '@id' indicates this is not a new annoucement.\"}";
        }
        if (annoucement.getJsonString("motivation").toString().length() > 0) {
            // This should always exist.
            Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity("Missing 'motivation' property.").build();
            return "{\"error\":\"Annoucements without 'motivation' are not allowed on this server.\"}";
        }

        //NORMALIZATIONS       
        if (annoucement.getJsonString("@context").toString().length() > 0) {
            // Put in a default, please.
            annoucement = new Message().generate(annoucement, "@context", CONTEXT);
        }

        return annoucement.toString();
    }

    private JsonObject getMessage() throws Exception {
        URL noteUrl = new URL("https://rerum-inbox.firebaseio.com/messages/" + noteId + ".json");
        HttpURLConnection conn = (HttpURLConnection) noteUrl.openConnection();

        StringBuilder response;
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            String inputLine;
            response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }
        System.out.println(response.toString() + " | https://rerum-inbox.firebaseio.com/messages/" + noteId + ".json");
        if (response.toString().isEmpty()) { // No dice... this still let's null through.
            throw new Exception("No message found");
        }
        JsonObject note;
        try {
            note = generate(response.toString(), "@id", "http://inbox.rerum.io/id/" + noteId + "");
        } catch (Exception ex) {
            throw new Exception("No message found. " + ex.getMessage());
        }
        return note;
    }

    /**
     *
     * @param parsable
     * @param key
     * @param value
     * @return
     */
    public JsonObject generate(String parsable, String key, String value) {
        JsonReader reader = Json.createReader(new StringReader(parsable));
        JsonObject source = reader.readObject();
        reader.close();
        return generate(source, key, value);
    }

    /**
     *
     * @param source
     * @param key
     * @param value
     * @return
     */
    public JsonObject generate(JsonObject source, String key, String value) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add(key, value);
        source.entrySet().
                forEach((Map.Entry<String, JsonValue> e) -> {
                    builder.add(e.getKey(), e.getValue());
                });
        return builder.build();
    }
}
