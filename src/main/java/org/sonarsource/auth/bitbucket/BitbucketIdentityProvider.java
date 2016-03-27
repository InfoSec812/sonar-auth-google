/*
 * Bitbucket Authentication for SonarQube
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
package org.sonarsource.auth.bitbucket;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Token;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.model.Verifier;
import com.github.scribejava.core.oauth.OAuthService;
import javax.annotation.CheckForNull;
import javax.servlet.http.HttpServletRequest;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.authentication.Display;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static java.lang.String.format;

@ServerSide
public class BitbucketIdentityProvider implements OAuth2IdentityProvider {

  private static final Logger LOGGER = Loggers.get(BitbucketIdentityProvider.class);

  public static final String REQUIRED_SCOPE = "account";
  public static final String KEY = "bitbucket";
  private static final Token EMPTY_TOKEN = null;

  private final BitbucketSettings settings;
  private final UserIdentityFactory userIdentityFactory;
  private final BitbucketScribeApi scribe;

  public BitbucketIdentityProvider(BitbucketSettings settings, UserIdentityFactory userIdentityFactory, BitbucketScribeApi scribe) {
    this.settings = settings;
    this.userIdentityFactory = userIdentityFactory;
    this.scribe = scribe;
  }

  @Override
  public String getKey() {
    return KEY;
  }

  @Override
  public String getName() {
    return "Bitbucket";
  }

  @Override
  public Display getDisplay() {
    return Display.builder()
      // URL of src/main/resources/static/bitbucket.svg at runtime
      .setIconPath("/static/authbitbucket/bitbucket.svg")
      .setBackgroundColor("#205081")
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
    GsonEmails gsonEmails = requestEmails(scribe, accessToken);
    UserIdentity userIdentity = userIdentityFactory.create(gsonUser, gsonEmails);
    context.authenticate(userIdentity);
    context.redirectToRequestedPage();
  }

  private GsonUser requestUser(OAuthService scribe, Token accessToken) {
    OAuthRequest userRequest = new OAuthRequest(Verb.GET, settings.apiURL() + "2.0/user", scribe);
    scribe.signRequest(accessToken, userRequest);
    Response userResponse = userRequest.send();

    if (!userResponse.isSuccessful()) {
      throw new IllegalStateException(format("Can not get Bitbucket user profile. HTTP code: %s, response: %s",
        userResponse.getCode(), userResponse.getBody()));
    }
    String userResponseBody = userResponse.getBody();
    LOGGER.trace("User response received : %s", userResponseBody);
    return GsonUser.parse(userResponseBody);
  }

  @CheckForNull
  private GsonEmails requestEmails(OAuthService scribe, Token accessToken) {
    OAuthRequest userRequest = new OAuthRequest(Verb.GET, settings.apiURL() + "2.0/user/emails", scribe);
    scribe.signRequest(accessToken, userRequest);
    Response emailsResponse = userRequest.send();
    if (emailsResponse.isSuccessful()) {
      return GsonEmails.parse(emailsResponse.getBody());
    }
    return null;
  }

  private ServiceBuilder newScribeBuilder(OAuth2IdentityProvider.OAuth2Context context) {
    if (!isEnabled()) {
      throw new IllegalStateException("Bitbucket authentication is disabled");
    }
    return new ServiceBuilder()
      .provider(scribe)
      .apiKey(settings.clientId())
      .apiSecret(settings.clientSecret())
      .grantType("authorization_code")
      .callback(context.getCallbackUrl());
  }
}
