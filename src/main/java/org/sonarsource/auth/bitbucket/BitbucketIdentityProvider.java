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
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;

/**
 * TODO add debug logs
 * TODO document that scope "Account Read" is required (it includes scope "Account Email")
 */
@ServerSide
public class BitbucketIdentityProvider implements OAuth2IdentityProvider {

  public static final String REQUIRED_SCOPE = "account";
  private static final Token EMPTY_TOKEN = null;

  private final BitbucketSettings settings;

  public BitbucketIdentityProvider(BitbucketSettings settings) {
    this.settings = settings;
  }

  @Override
  public String getKey() {
    return "bitbucket";
  }

  @Override
  public String getName() {
    return "Bitbucket";
  }

  @Override
  public String getIconPath() {
    // URL of src/main/resources/static/bitbucket.svg at runtime
    return "/static/authbitbucket/bitbucket.svg";
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
    OAuthService scribe = prepareScribe(context)
      .scope(REQUIRED_SCOPE)
      .build();
    String url = scribe.getAuthorizationUrl(EMPTY_TOKEN);
    context.redirectTo(url);
  }

  @Override
  public void callback(CallbackContext context) {
    HttpServletRequest request = context.getRequest();
    OAuthService scribe = prepareScribe(context).build();
    String oAuthVerifier = request.getParameter("code");
    Token accessToken = scribe.getAccessToken(EMPTY_TOKEN, new Verifier(oAuthVerifier));

    GsonUser gsonUser = requestUser(scribe, accessToken);
    String primaryEmail = requestPrimaryEmail(scribe, accessToken);

    UserIdentity userIdentity = UserIdentity.builder()
      .setId(gsonUser.getUsername())
      .setName(gsonUser.getDisplayName())
      .setEmail(primaryEmail)
      .build();
    context.authenticate(userIdentity);
    context.redirectToRequestedPage();
  }

  private GsonUser requestUser(OAuthService scribe, Token accessToken) {
    OAuthRequest userRequest = new OAuthRequest(Verb.GET, "https://api.bitbucket.org/2.0/user", scribe);
    scribe.signRequest(accessToken, userRequest);
    Response userResponse = userRequest.send();
    // TODO test if successful and if information is available. Callback can be called even if the request scope
    // was not accepted by user
    return GsonUser.parse(userResponse.getBody());
  }

  @CheckForNull
  private String requestPrimaryEmail(OAuthService scribe, Token accessToken) {
    OAuthRequest userRequest = new OAuthRequest(Verb.GET, "https://api.bitbucket.org/2.0/user/emails", scribe);
    scribe.signRequest(accessToken, userRequest);
    Response emailsResponse = userRequest.send();
    if (emailsResponse.isSuccessful()) {
      return GsonEmails.extractPrimaryEmail(emailsResponse.getBody());
    }
    return null;
  }

  private ServiceBuilder prepareScribe(OAuth2IdentityProvider.OAuth2Context context) {
    if (!isEnabled()) {
      throw new IllegalStateException("Bitbucket Authentication is disabled");
    }
    return new ServiceBuilder()
      .provider(BitbucketScribeApi.class)
      .apiKey(settings.clientId())
      .apiSecret(settings.clientSecret())
      .grantType("authorization_code")
      .callback(context.getCallbackUrl());
  }

}
