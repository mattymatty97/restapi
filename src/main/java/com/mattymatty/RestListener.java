package com.mattymatty;

import com.mrpowergamerbr.temmiediscordauth.TemmieDiscordAuth;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@SuppressWarnings("Duplicates")
@CrossOrigin
@RestController
public class RestListener {

    private JSONObject help;

    {
        byte[] encoded;
        try {
            File fhelp = new File("./help.json");
            encoded = Files.readAllBytes(fhelp.toPath());
            help = new JSONObject(new String(encoded));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequestMapping(value = "/", produces = "application/json")
    public ResponseEntity<String> handleReq() {
        return new ResponseEntity<>(new JSONObject().put("STATUS", 400)
                .put("BOTS", new JSONArray()
                        .put(new JSONObject()
                                .put("NAME", "rolegroup")
                                .put("PATH", "/rg/"))).toString(), HttpStatus.valueOf(400));
    }

    @GetMapping(value = "/rg", produces = "application/json")
    public ResponseEntity<String> handleRg() {
        return new ResponseEntity<>(help.toString(), HttpStatus.valueOf(400));
    }

    @PostMapping(value = "/rg", consumes = {"application/json"}, produces = "application/json")
    public ResponseEntity<String> handleRgAuth(@RequestBody String body) {
        try {
            JSONObject req = new JSONObject(body);
            if (req.has("ACTION"))
                if (req.getString("ACTION").equals("auth"))
                    return mkAuth(req);
                else
                    return getServers(req);
            else
                return getBadRequest();
        } catch (JSONException ex) {
            return getBadRequest();
        }
    }


    @GetMapping(value = "/rg/{server}", produces = "application/json")
    public ResponseEntity<String> handleGuildGet(@PathVariable("server") String sserver) {
        try {
            long server = Long.parseLong(sserver);
            JSONObject req = new JSONObject().put("REQUEST", "guild").put("GUILD_ID", server);
            return getReponse(req);
        } catch (NumberFormatException ex) {
            return getBadRequest();
        }
    }

    @PostMapping(value = "/rg/{server}", consumes = {"application/json"}, produces = "application/json")
    public ResponseEntity<String> handleGuildPost(@PathVariable("server") String sserver, @RequestBody String body) {
        try {
            long server = Long.parseLong(sserver);
            JSONObject jbody = new JSONObject(body);
            JSONObject req = new JSONObject()
                    .put("REQUEST", "action")
                    .put("TARGET", "guild")
                    .put("GUILD_ID", server);
            long user = getAuth(jbody);
            if (user > 0) {
                req.put("USER_ID", user);
                jbody.remove("AUTH");
                req.put("ACTION", jbody);
                return getReponse(req);
            }
            if (user < 0)
                return getUnauthorized(req, "Unauthorized: Missing Token");
            return getUnauthorized(req, "Unauthorized: Expired Token");
        } catch (NumberFormatException | JSONException ex) {
            return getBadRequest();
        }
    }

    @GetMapping(value = "/rg/{server}/create", produces = "application/json")
    public RedirectView handleGroupCreateGet(@PathVariable("server") String sserver) {
        return new RedirectView("/rg");
    }

    @PostMapping(value = "/rg/{server}/create", consumes = {"application/json"}, produces = "application/json")
    public ResponseEntity<String> handleGroupCreate(@PathVariable("server") String sserver, @RequestBody String body) {
        try {
            long server = Long.parseLong(sserver);
            JSONObject jbody = new JSONObject(body);
            JSONObject req = new JSONObject().put("REQUEST", "action")
                    .put("TARGET", "create")
                    .put("GUILD_ID", server);
            long user = getAuth(jbody);
            if (user > 0) {
                req.put("USER_ID", user);
                req.put("USER_ID", user);
                jbody.remove("AUTH");
                req.put("ACTION", jbody);
                return getReponse(req);
            }
            if (user < 0)
                return getUnauthorized(req, "Unauthorized: Missing Token");
            return getUnauthorized(req, "Unauthorized: Expired Token");
        } catch (NumberFormatException | JSONException ex) {
            return getBadRequest();
        }
    }


    @GetMapping(value = "/rg/{server}/{group}", produces = "application/json")
    public ResponseEntity<String> handleGroupGet(@PathVariable("server") String sserver, @PathVariable("group") String sgroup) {
        try {
            long server = Long.parseLong(sserver);
            long group = Long.parseLong(sgroup);
            JSONObject req = new JSONObject().put("REQUEST", "group").put("GUILD_ID", server).put("GROUP_ID", group);
            return getReponse(req);
        } catch (NumberFormatException ex) {
            return getBadRequest();
        }
    }

    @PostMapping(value = "/rg/{server}/{group}", consumes = {"application/json"}, produces = "application/json")
    public ResponseEntity<String> handleGroupPost(@PathVariable("server") String sserver, @PathVariable("group") String sgroup, @RequestBody String body) {
        try {
            long server = Long.parseLong(sserver);
            long group = Long.parseLong(sgroup);
            JSONObject jbody = new JSONObject(body);
            JSONObject req = new JSONObject().put("REQUEST", "action")
                    .put("TARGET", "group")
                    .put("GUILD_ID", server)
                    .put("GROUP_ID", group);
            long user = getAuth(jbody);
            if (user > 0) {
                req.put("USER_ID", user);
                jbody.remove("AUTH");
                req.put("ACTION", jbody);
                return getReponse(req);
            }
            if (user < 0)
                return getUnauthorized(req, "Unauthorized: Missing Token");
            return getUnauthorized(req, "Unauthorized: Expired Token");
        } catch (NumberFormatException | JSONException ex) {
            return getBadRequest();
        }
    }

    private ResponseEntity<String> getReponse(JSONObject req) {
        JSONObject rep = getJSONReponse(req);
        if (rep == null)
            return new ResponseEntity<>("ERROR", HttpStatus.valueOf(500));
        return new ResponseEntity<>(rep.toString(), HttpStatus.valueOf(rep.getInt("STATUS")));
    }

    private JSONObject getJSONReponse(JSONObject req) {
        System.out.println("\nRequest: ");
        System.out.println(req.toString(3));
        JSONObject rep = RestApi.controller.request("rolegroup", req);
        try {
            JSONObject printRep = new JSONObject().put("ID", rep.get("ID")).put("STATUS", rep.get("STATUS"));
            if (rep.getInt("STATUS") != 200)
                printRep.put("REASON", rep.getString("REASON"));
            System.out.println("Answer: ");
            System.out.println(printRep.toString());
            return rep;
        } catch (NullPointerException e) {
            System.out.println("Answer: ");
            System.out.println("Sorry This BOT is Offline");
            return new JSONObject().put("STATUS", 500).put("ID", "Rest").put("REASON", "Sorry This BOT is Offline");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private ResponseEntity<String> getServers(JSONObject body) {
        JSONObject req = new JSONObject();
        long user = getAuth(body);
        if (user > 0) {
            req.put("REQUEST", "auth");
            req.put("USER_ID", user);
            body.remove("AUTH");
            req.put("ACTION", body);
            return getReponse(req);
        }
        if (user < 0)
            return getUnauthorized(req, "Unauthorized: Missing Token");
        return getUnauthorized(req, "Unauthorized: Expired Token");
    }


    private Map<String, Long> autorizations = new HashMap<>();
    private Map<Long, String> autorizationsRev = new HashMap<>();
    private Map<String, TemmieDiscordAuth> temmiesMap = new HashMap<>();
    private Map<Long, String> userTokens = new HashMap<>();

    private ResponseEntity<String> mkAuth(JSONObject req) {
        JSONObject rep = new JSONObject();
        rep.put("ID", "rolegroup");
        System.out.println("\nRequest: ");
        System.out.println(req.toString(2));
        if (req.has("USER_TOKEN")) {
            String user_token = req.getString("USER_TOKEN");
            TemmieDiscordAuth temmie = temmiesMap.get(user_token);
            if (temmie == null) {
                temmie = new TemmieDiscordAuth(user_token, "http://localhost:50451/api/discord/callback", "427145499766947840", "Uw7nUaDH_akGKxkNOZ0bYhkY5YQoqlgu");
                if (temmie.doTokenExchange().getAccessToken()!=null)
                    temmiesMap.put(user_token, temmie);
                else
                    return getBadRequest();
            }
            if (temmie.isValid()) {
                temmie.doTokenExchangeUsingRefreshToken();
            }
            long user_id = Long.parseLong(temmie.getCurrentUserIdentification().getId());
            String old_token = userTokens.get(user_id);
            if(!user_token.equals(old_token)){
                if(old_token!=null)
                    temmiesMap.remove(old_token);
                userTokens.put(user_id,user_token);
            }
            String apiToken;
            if ((apiToken = autorizationsRev.get(user_id)) != null) {
                autorizations.remove(apiToken);
                autorizationsRev.remove(user_id);
            }
            apiToken = getToken();
            autorizations.put(apiToken, user_id);
            autorizationsRev.put(user_id, apiToken);
            rep.put("TOKEN", apiToken);
            rep.put("STATUS", 200);
        } else

        {
            rep.put("STATUS", 400);
            rep.put("REASON", "Missing USER_ID");
        }
        System.out.println("Answer: ");
        System.out.println(rep.toString(2));
        return new ResponseEntity<>(rep.toString(), HttpStatus.valueOf(rep.getInt("STATUS")));
    }

    private long getAuth(JSONObject req) {
        if (req.has("AUTH")) {
            String token = req.getString("AUTH");
            long user_id = autorizations.getOrDefault(token, 0L);
            autorizations.remove(token);
            autorizationsRev.remove(user_id);
            return user_id;
        }
        return -1;
    }

    private String getToken() {
        Random random = new Random();
        Long buffer = random.nextLong();
        return Long.toUnsignedString(buffer);
    }

    private ResponseEntity<String> getUnauthorized(JSONObject req) {
        System.out.println("Request: ");
        System.out.println(req.toString(2));
        JSONObject rep = new JSONObject().put("STATUS", 403);
        System.out.println("Answer: ");
        System.out.println(rep.toString(2));
        return new ResponseEntity<>(rep.toString(), HttpStatus.valueOf(403));
    }

    private ResponseEntity<String> getUnauthorized(JSONObject req, String reason) {
        System.out.println("Request: ");
        System.out.println(req.toString(2));
        JSONObject rep = new JSONObject().put("STATUS", 403).put("ID", "Rest").put("REASON", reason);
        System.out.println("Answer: ");
        System.out.println(rep.toString(2));
        return new ResponseEntity<>(rep.toString(), HttpStatus.valueOf(403));
    }

    private ResponseEntity<String> getBadRequest() {
        return new ResponseEntity<>(new JSONObject().put("STATUS", 400).put("ID", "Rest").put("REASON", "Bad Request").toString(), HttpStatus.valueOf(400));
    }


}
