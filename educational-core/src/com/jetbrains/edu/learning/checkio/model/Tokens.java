package com.jetbrains.edu.learning.checkio.model;

import com.google.gson.annotations.SerializedName;
import com.intellij.util.xmlb.annotations.Property;

public class Tokens {
  @Property
  @SerializedName("access_token")
  private String myAccessToken;

  @SerializedName("refresh_token")
  private String myRefreshToken;

  @SerializedName("expires_in")
  private int expiresIn;

  private int receivingTime;

  public String getAccessToken() {
    return myAccessToken;
  }

  public String getRefreshToken() {
    return myRefreshToken;
  }

  public void received() {
    receivingTime = getCurrentTimeSeconds();
  }

  public boolean isUpToDate() {
    // TODO: maybe `receivingTime + expiresIn < getCurrentTimeSeconds() - delta` for escaping boundary case
    return receivingTime + expiresIn < getCurrentTimeSeconds();
  }

  private static int getCurrentTimeSeconds() {
    return (int) (System.currentTimeMillis() / 1000);
  }
}
