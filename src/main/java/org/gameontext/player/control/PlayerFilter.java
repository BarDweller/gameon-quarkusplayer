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
package org.gameontext.player.control;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.config.ConfigProvider;
import org.gameontext.signed.SignedJWT;
import org.gameontext.signed.SignedJWT.AuthenticationState;

@WebFilter(filterName = "playerJWTAuthFilter", urlPatterns = { "/*" })
public class PlayerFilter implements Filter {

    String pemCert;

    private static Certificate signingCert = null;

    private synchronized void readCert() throws IOException {
        try {

            if(null==pemCert || pemCert.equals("x")){
                pemCert = ConfigProvider.getConfig().getValue("JWT_PUBLIC_CERT", String.class);
            }
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            signingCert = factory.generateCertificate(new ByteArrayInputStream(pemCert.getBytes()));
        } catch (CertificateException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    private final static String jwtParamName = "jwt";
    private final static String jwtHeaderName = "gameon-jwt";


    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if(signingCert==null){
            readCert();
        }

        Object playerId = null;
        Object story = null;
        Object playerMode = null;
        Object audience = null;

        HttpServletRequest req = ((HttpServletRequest) request);
        String jwtHeader = null;
        String jwtParam = null;

        //reject the request if multiple jwt headers or parameters were supplied
        for(Enumeration<String> headers = req.getHeaders(jwtHeaderName); headers.hasMoreElements(); ) {
            if(jwtHeader == null) {
                jwtHeader = headers.nextElement();
            } else {
                //multiple header values are an error, so get a bad request
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        }
        String[] params = req.getParameterValues(jwtParamName);
        if(params != null) {
            for(String param : params) {
                if(jwtParam == null) {
                    jwtParam = param;
                } else {
                    //multiple header values are an error, so get a bad request
                    ((HttpServletResponse) response).sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
            }
        }
        
        SignedJWT jwt = new SignedJWT(signingCert, jwtHeader, jwtParam);
        
        if(jwt.getState().equals(AuthenticationState.ACCESS_DENIED)) {
            String ctxPath = req.getContextPath();
            boolean protectedUrl = ctxPath.contains("account");
            
            //JWT is not valid, however we let GET requests with no parameters through to protected urls.
            if(protectedUrl && !("GET".equals(req.getMethod()) && (req.getQueryString()==null || req.getQueryString().isEmpty()))){
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        } else {
            playerId = jwt.getClaim("sub");
            story = jwt.getClaim("story");
            playerMode = jwt.getClaim("playerMode");
            audience = jwt.getClaim("aud");
        }
    
        request.setAttribute("player.id", playerId);
        if(story!=null) request.setAttribute("player.story", story);
        request.setAttribute("player.playerMode", playerMode);
        request.setAttribute("player.audience", audience);
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }

}
