package com.iubiquity.spreadsheets.model;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.util.Key;

public class SpreadsheetUrl extends GenericUrl {

  public static final String ROOT_URL = "https://spreadsheets.google.com/feeds/spreadsheets";

  @Key("max-results")
  public Integer maxResults;

  public SpreadsheetUrl(String url) {
    super(url);
    //this.prettyprint = true;
  }

  private static SpreadsheetUrl forRoot() {
    return new SpreadsheetUrl(ROOT_URL);
  }

  public static SpreadsheetUrl forSpreadSheetMetafeed() {
    SpreadsheetUrl result = forRoot();
    result.getPathParts().add("private");
    result.getPathParts().add("full");
    return result;
  }

}
