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
import com.github.scribejava.core.model.OAuthConfig;
import com.github.scribejava.core.utils.OAuthEncoder;

import static java.lang.String.format;

public class BitbucketScribeApi extends DefaultApi20 {
  private static final String TOKEN_URL = "https://bitbucket.org/site/oauth2/access_token";

  @Override
  public String getAccessTokenEndpoint() {
    return TOKEN_URL;
  }

  public String getAuthorizationUrl(OAuthConfig config) {
    StringBuilder sb = new StringBuilder(format("https://bitbucket.org/site/oauth2/authorize?response_type=code&client_id=%s&redirect_uri=%s", config.getApiKey(),
      OAuthEncoder.encode(config.getCallback())));
    if (config.hasScope()) {
      sb.append('&').append("scope").append('=').append(OAuthEncoder.encode(config.getScope()));
    }
    return sb.toString();
  }
}
