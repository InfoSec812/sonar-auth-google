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

import javax.annotation.Nullable;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.authentication.UserIdentity;

import static java.lang.String.format;
import static org.sonarsource.auth.bitbucket.BitbucketSettings.LOGIN_STRATEGY_UNIQUE;

/**
 * Converts Bitbucket JSON documents to {@link UserIdentity}
 */
@ServerSide
public class UserIdentityFactory {

  private final BitbucketSettings settings;

  public UserIdentityFactory(BitbucketSettings settings) {
    this.settings = settings;
  }

  public UserIdentity create(GsonUser gsonUser, @Nullable GsonEmails gsonEmails) {
    UserIdentity.Builder builder = builder(gsonUser);
    if (gsonEmails != null) {
      builder.setEmail(gsonEmails.extractPrimaryEmail());
    }
    return builder.build();
  }

  private UserIdentity.Builder builder(GsonUser gsonUser) {
    return UserIdentity.builder()
      .setProviderLogin(gsonUser.getUsername())
      .setLogin(generateLogin(gsonUser))
      .setName(generateName(gsonUser));
  }

  private String generateLogin(GsonUser gsonUser) {
    switch (settings.loginStrategy()) {
      case BitbucketSettings.LOGIN_STRATEGY_PROVIDER_LOGIN:
        return gsonUser.getUsername();
      case LOGIN_STRATEGY_UNIQUE:
        return generateUniqueLogin(gsonUser);
      default:
        throw new IllegalStateException(format("Login strategy not supported : %s", settings.loginStrategy()));
    }
  }

  private static String generateName(GsonUser gson) {
    String name = gson.getDisplayName();
    return name == null ? gson.getUsername() : name;
  }

  private String generateUniqueLogin(GsonUser gsonUser) {
    return format("%s@%s", gsonUser.getUsername(), BitbucketIdentityProvider.KEY);
  }
}
