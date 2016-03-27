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

import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.extractors.AccessTokenExtractor;
import com.github.scribejava.core.extractors.JsonTokenExtractor;
import com.github.scribejava.core.model.OAuthConfig;
import com.github.scribejava.core.model.Verb;
import org.sonar.api.server.ServerSide;

import static com.github.scribejava.core.utils.OAuthEncoder.encode;

@ServerSide
public class BitbucketScribeApi extends DefaultApi20 {

  private final BitbucketSettings settings;

  public BitbucketScribeApi(BitbucketSettings settings) {
    this.settings = settings;
  }

  @Override
  public String getAccessTokenEndpoint() {
    return settings.webURL() + "site/oauth2/access_token";
  }

  @Override
  public Verb getAccessTokenVerb() {
    return Verb.POST;
  }

  @Override
  public String getAuthorizationUrl(OAuthConfig config) {
    return new StringBuilder(settings.webURL())
      .append("site/oauth2/authorize?response_type=code&client_id=").append(config.getApiKey())
      .append("&redirect_uri=").append(encode(config.getCallback()))
      .append("&scope=").append(encode(config.getScope()))
      .toString();
  }

  @Override
  public AccessTokenExtractor getAccessTokenExtractor() {
    return new JsonTokenExtractor();
  }
}
