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

import com.github.scribejava.apis.GoogleApi20;
import com.github.scribejava.core.extractors.AccessTokenExtractor;
import com.github.scribejava.core.extractors.JsonTokenExtractor;
import com.github.scribejava.core.model.OAuthConfig;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.utils.OAuthEncoder;
import org.sonar.api.server.ServerSide;

@ServerSide
public class GoogleScribeApi extends GoogleApi20 {

  public static final String GOOGLE_OAUTH_URL = "?response_type=code&client_id=%s&redirect_uri=%s&scope=%s";
  private final GoogleSettings settings;

  public GoogleScribeApi(GoogleSettings settings) {
    this.settings = settings;
  }

  @Override
  public String getAccessTokenEndpoint() {
    return settings.apiURL() + "oauth2/v3/token";
  }

  @Override
  public Verb getAccessTokenVerb() {
    return Verb.POST;
  }

  @Override
  public String getAuthorizationUrl(OAuthConfig config) {
    StringBuilder sb = new StringBuilder(settings.webURL())
            .append(String.format(GOOGLE_OAUTH_URL, new Object[]{config.getApiKey(), OAuthEncoder.encode(config.getCallback()), OAuthEncoder.encode(config.getScope())}));
    String state = config.getState();
    if(state != null) {
      sb.append('&').append("state").append('=').append(OAuthEncoder.encode(state));
    }
    if (settings.oauthDomain() != null) {
      sb.append('&').append("hd=").append(settings.oauthDomain());
    }

    return sb.toString();
  }

  @Override
  public AccessTokenExtractor getAccessTokenExtractor() {
    return new JsonTokenExtractor();
  }
}
