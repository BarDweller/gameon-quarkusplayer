package org.gameontext.player.utils;

import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Set;
import java.util.logging.Level;

import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.JWTCallerPrincipal;
import io.smallrye.jwt.auth.principal.JWTCallerPrincipalFactory;
import io.smallrye.jwt.auth.principal.ParseException;


/**
 * Common class for handling JSON Web Tokens
 */

public class JWT {
    private final AuthenticationState state;
    private FailureCode code;
    private String token = null;
    private JWTCallerPrincipal jwtcp = null;

    public JWT(Certificate cert, String... sources) {
        state = processSources(cert.getPublicKey(), sources);
    }

    public JWT(PublicKey key, String... sources) {
        state = processSources(key, sources);
    }

    // the authentication steps that are performed on an incoming request
    public enum AuthenticationState {
        PASSED, ACCESS_DENIED           // end state
    }

    public enum FailureCode {
        NONE,
        BAD_SIGNATURE,
        EXPIRED
    }

    private enum ProcessState {
        FIND_SOURCE,
        VALIDATE,
        COMPLETE
    }

    private AuthenticationState processSources(PublicKey key, String[] sources) {
        AuthenticationState state = AuthenticationState.ACCESS_DENIED; // default
        ProcessState process = ProcessState.FIND_SOURCE;
        while (!process.equals(ProcessState.COMPLETE)) {
            switch (process) {
            case FIND_SOURCE :
                //find the first non-empty source
                for(int i = 0; i < sources.length && ((token == null) || token.isEmpty()); token = sources[i++]);
                process = ((token == null) || token.isEmpty()) ? ProcessState.COMPLETE : ProcessState.VALIDATE;
                break;
            case VALIDATE: // validate the jwt
                boolean jwtValid = false;
                try {
                    JWTAuthContextInfo ctx = new JWTAuthContextInfo(key, "test");
                    ctx.setIssuedBy(null);
                    ctx.setRequiredClaims(Set.of("sub","aud","name","id","exp","iat"));
                    ctx.setExpGracePeriodSecs(60);
                    JWTCallerPrincipalFactory factory = JWTCallerPrincipalFactory.instance();
                    jwtcp = factory.parse(token, ctx);
                    jwtValid = true;
                    code = FailureCode.NONE;
                } catch (ParseException e) {
                    Log.log(Level.WARNING, this, "JWT did NOT validate ok, bad signature.");
                    code = FailureCode.BAD_SIGNATURE;
                } 
                state = !jwtValid ? AuthenticationState.ACCESS_DENIED : AuthenticationState.PASSED;
                process = ProcessState.COMPLETE;
                break;
            default:
                process = ProcessState.COMPLETE;
                break;
            }
        }
        return state;
    }

    public AuthenticationState getState() {
        return state;
    }

    public FailureCode getCode() {
        return code;
    }

    public String getToken() {
        return token;
    }

    public Object getClaim(String key) {
        return jwtcp.getClaim(key);
    }


}
