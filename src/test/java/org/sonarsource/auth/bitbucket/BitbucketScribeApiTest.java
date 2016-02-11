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

import com.github.scribejava.core.extractors.JsonTokenExtractor;
import com.github.scribejava.core.model.OAuthConfig;
import com.github.scribejava.core.model.Verb;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BitbucketScribeApiTest {

  BitbucketScribeApi underTest = new BitbucketScribeApi();

  @Test
  public void getAccessTokenEndpoint() throws Exception {
    assertThat(underTest.getAccessTokenEndpoint()).isEqualTo("https://bitbucket.org/site/oauth2/access_token");
  }

  @Test
  public void getAccessTokenVerb() throws Exception {
    assertThat(underTest.getAccessTokenVerb()).isEqualTo(Verb.POST);
  }

  @Test
  public void getAuthorizationUrl() throws Exception {
    OAuthConfig oAuthConfig = new OAuthConfig("key", null, "callback", null, "scope", null, null, null, null);
    assertThat(underTest.getAuthorizationUrl(oAuthConfig)).isEqualTo(
      "https://bitbucket.org/site/oauth2/authorize?response_type=code&client_id=key&redirect_uri=callback&scope=scope"
    );
  }

  @Test
  public void getAccessTokenExtractor() throws Exception {
    assertThat(underTest.getAccessTokenExtractor()).isInstanceOf(JsonTokenExtractor.class);
  }
}
