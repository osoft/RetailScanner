package com.iubiquity.spreadsheets.model;

import com.google.api.client.util.Key;


public class Cell {

  @Key("@row")
  public int row;

  @Key("@col")
  public int col;

  @Key("@inputValue")
  public String value;

}
