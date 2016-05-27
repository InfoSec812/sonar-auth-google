/*
 * Google Authentication for SonarQube
 * Copyright (C) 2016-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarqube.auth.googleoauth;

/*-
 * #%L
 * Google Authentication for SonarQube
 * %%
 * Copyright (C) 2016 SonarSource
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.*;
import com.github.scribejava.core.oauth.OAuthService;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.authentication.Display;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static java.lang.String.format;

@ServerSide
public class GoogleIdentityProvider implements OAuth2IdentityProvider {

  private static final Logger LOGGER = Loggers.get(GoogleIdentityProvider.class);

  public static final String REQUIRED_SCOPE = "openid email";
  public static final String KEY = "googleoauth";
  private static final Token EMPTY_TOKEN = null;

  private final GoogleSettings settings;
  private final UserIdentityFactory userIdentityFactory;
  private final GoogleScribeApi scribeApi;

  public GoogleIdentityProvider(GoogleSettings settings, UserIdentityFactory userIdentityFactory, GoogleScribeApi scribeApi) {
    this.settings = settings;
    this.userIdentityFactory = userIdentityFactory;
    this.scribeApi = scribeApi;
  }

  @Override
  public String getKey() {
    return KEY;
  }

  @Override
  public String getName() {
    return "Google";
  }

  @Override
  public Display getDisplay() {
    return Display.builder()
      // URL of src/main/resources/static/googleoauth.svg at runtime
      .setIconPath("/static/authgoogle/googleoauth.svg")
      .setBackgroundColor("#236487")
      .build();
  }

  @Override
  public boolean isEnabled() {
    return settings.isEnabled();
  }

  @Override
  public boolean allowsUsersToSignUp() {
    return settings.allowUsersToSignUp();
  }

  @Override
  public void init(InitContext context) {
    OAuthService scribe = newScribeBuilder(context)
      .scope(REQUIRED_SCOPE)
      .build();
    String url = scribe.getAuthorizationUrl(EMPTY_TOKEN);
    context.redirectTo(url);
  }

  @Override
  public void callback(CallbackContext context) {
    HttpServletRequest request = context.getRequest();
    OAuthService scribe = newScribeBuilder(context).build();
    String oAuthVerifier = request.getParameter("code");
    Token accessToken = scribe.getAccessToken(EMPTY_TOKEN, new Verifier(oAuthVerifier));

    GsonUser gsonUser = requestUser(scribe, accessToken);
    String redirectTo;
    if (settings.oauthDomain()==null || (settings.oauthDomain()!=null && gsonUser.getEmail().endsWith("@"+settings.oauthDomain()))) {
      redirectTo = settings.getSonarBaseURL();
      UserIdentity userIdentity = userIdentityFactory.create(gsonUser);
      context.authenticate(userIdentity);
    } else {
      redirectTo = settings.getSonarBaseURL()+"/sessions/unauthorized#";
    }
    try {
      context.getResponse().sendRedirect(redirectTo);
    } catch (IOException ioe) {
      throw MessageException.of("Unable to redirect after OAuth login", ioe);
    }
  }

  private GsonUser requestUser(OAuthService scribe, Token accessToken) {
    OAuthRequest userRequest = new OAuthRequest(Verb.GET, settings.apiURL() + "oauth2/v1/userinfo", scribe);
    scribe.signRequest(accessToken, userRequest);
    Response userResponse = userRequest.send();

    if (!userResponse.isSuccessful()) {
      throw new IllegalStateException(format("Can not get Google user profile. HTTP code: %s, response: %s",
        userResponse.getCode(), userResponse.getBody()));
    }
    String userResponseBody = userResponse.getBody();
    LOGGER.trace("User response received : %s", userResponseBody);
    return GsonUser.parse(userResponseBody);
  }

  private ServiceBuilder newScribeBuilder(OAuth2IdentityProvider.OAuth2Context context) {
    if (!isEnabled()) {
      throw new IllegalStateException("Google authentication is disabled");
    }
    return new ServiceBuilder()
      .provider(scribeApi)
      .apiKey(settings.clientId())
      .apiSecret(settings.clientSecret())
      .grantType("authorization_code")
      .callback(context.getCallbackUrl());
  }
}
