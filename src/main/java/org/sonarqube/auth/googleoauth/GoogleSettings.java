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

import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.server.ServerSide;

import javax.annotation.CheckForNull;
import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;
import static org.sonar.api.PropertyType.SINGLE_SELECT_LIST;

@ServerSide
public class GoogleSettings {

  public static final String CONSUMER_KEY = "sonar.auth.googleoauth.clientId.secured";
  public static final String CONSUMER_SECRET = "sonar.auth.googleoauth.clientSecret.secured";
  public static final String ENABLED = "sonar.auth.googleoauth.enabled";
  public static final String ALLOW_USERS_TO_SIGN_UP = "sonar.auth.googleoauth.allowUsersToSignUp";
  public static final String LIMIT_DOMAIN = "sonar.auth.googleoauth.limitOauthDomain";
  public static final String API_URL = "sonar.auth.googleoauth.apiUrl";
  public static final String DEFAULT_API_URL = "https://www.googleapis.com/";
  public static final String WEB_URL = "sonar.auth.googleoauth.webUrl";
  public static final String DEFAULT_WEB_URL = "https://accounts.google.com/o/oauth2/v2/auth";
  public static final String LOGIN_STRATEGY = "sonar.auth.googleoauth.loginStrategy";
  public static final String LOGIN_STRATEGY_UNIQUE = "Unique";
  public static final String LOGIN_STRATEGY_PROVIDER_LOGIN = "Same as Google login";
  public static final String LOGIN_STRATEGY_DEFAULT_VALUE = LOGIN_STRATEGY_UNIQUE;
  public static final String CATEGORY = "security";
  public static final String SUBCATEGORY = "googleoauth";
  public static final String SONAR_CONTEXT = "sonar.web.context";

  private final Settings settings;

  public GoogleSettings(Settings settings) {
    this.settings = settings;
  }

  @CheckForNull
  public String clientId() {
    return settings.getString(CONSUMER_KEY);
  }

  @CheckForNull
  public String clientSecret() {
    return settings.getString(CONSUMER_SECRET);
  }

  public boolean isEnabled() {
    return settings.getBoolean(ENABLED) && clientId() != null && clientSecret() != null;
  }

  public boolean allowUsersToSignUp() {
    return settings.getBoolean(ALLOW_USERS_TO_SIGN_UP);
  }

  public String loginStrategy() {
    return settings.getString(LOGIN_STRATEGY);
  }

  public String getSonarBaseURL() {
    return settings.getString("sonar.core.serverBaseURL");
  }

  public String webURL() {
    String url = settings.getString(WEB_URL);
    if (url == null) {
      url = DEFAULT_WEB_URL;
    }
    return url;
  }
  
  public String getWebContext() {
	  return settings.getString(SONAR_CONTEXT) == null ? "" : settings.getString(SONAR_CONTEXT);
  }

  public String apiURL() {
    String url = settings.getString(API_URL);
    if (url == null) {
      url = DEFAULT_API_URL;
    }
    return urlWithEndingSlash(url);
  }

  public String oauthDomain() {
    return settings.getString(LIMIT_DOMAIN);
  }

  private static String urlWithEndingSlash(String url) {
    if (!url.endsWith("/")) {
      return url + "/";
    }
    return url;
  }

  public static List<PropertyDefinition> definitions() {
    int index = 1;
    return Arrays.asList(
      PropertyDefinition.builder(ENABLED)
        .name("Enabled")
        .description("Enable Google users to login. Value is ignored if client ID and secret are not defined.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .type(PropertyType.BOOLEAN)
        .defaultValue(String.valueOf(false))
        .index(index++)
        .build(),
      PropertyDefinition.builder(CONSUMER_KEY)
        .name("OAuth client ID")
        .description("The Client ID provided by Google when registering the application.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .index(index++)
        .build(),
      PropertyDefinition.builder(CONSUMER_SECRET)
        .name("OAuth Client Secret")
        .description("Client Secret provided by Google when registering the application.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .index(index++)
        .build(),
      PropertyDefinition.builder(ALLOW_USERS_TO_SIGN_UP)
        .name("Allow users to sign-up")
        .description("Allow new users to authenticate. When set to 'false', only existing users will be able to authenticate to the server.")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .type(PropertyType.BOOLEAN)
        .defaultValue(String.valueOf(true))
        .index(index++)
        .build(),
      PropertyDefinition.builder(LOGIN_STRATEGY)
        .name("Login generation strategy")
        .description(format("When the login strategy is set to '%s', the user's login will be auto-generated the first time so that it is unique. " +
                        "When the login strategy is set to '%s', the user's login will be the Google login.",
                LOGIN_STRATEGY_UNIQUE, LOGIN_STRATEGY_PROVIDER_LOGIN))
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .type(SINGLE_SELECT_LIST)
        .defaultValue(LOGIN_STRATEGY_DEFAULT_VALUE)
        .options(LOGIN_STRATEGY_UNIQUE, LOGIN_STRATEGY_PROVIDER_LOGIN)
        .index(index++)
        .build(),
      PropertyDefinition.builder(LIMIT_DOMAIN)
        .name("Allowed Domain(s)")
        .description("When set, this will only allow users from the specified GApps domain(s) to authenticate (comma-separated)")
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .index(index++)
        .build(),
      PropertyDefinition.builder(WEB_URL)
        .name("Google authentication URI")
        .description("When set, this will override the URI used to send an authentication request to Google")
        .defaultValue(DEFAULT_WEB_URL)
        .category(CATEGORY)
        .subCategory(SUBCATEGORY)
        .index(index++)
        .build()
      );
  }
}
