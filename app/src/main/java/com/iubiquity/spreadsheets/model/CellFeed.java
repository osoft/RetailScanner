package com.iubiquity.spreadsheets.model;

import com.google.api.client.util.Key;

import java.util.ArrayList;
import java.util.List;

public class CellFeed extends Feed {

	@Key("title")
	public String title;

	@Key("author")
	public Author author;

	@Key("entry")
	public List<CellEntry> cells = new ArrayList<CellEntry>();

	public List<CellEntry> getEntries() {
		return cells;
	}

	public String getBatchError() {
		for (CellEntry ce : cells) {
			BatchStatus batchStatus = ce.batchStatus;


			if (batchStatus != null
                        && isSuccessStatusCode(batchStatus.code)) {
                        //&& !HttpResponse.isSuccessStatusCode(batchStatus.code)) {
				return batchStatus.reason;
			}
		}
		return null;
	}

    private boolean isSuccessStatusCode(int code) {
        return code >= 200 && code < 300;
    }

	public CellEntry getCellEntry(final String title) {
		if (title != null) {
			final String s = title.trim();
			for (CellEntry e : cells) {
				if (s.equals(e.title)) {
					return e;
				}
			}
		}
		return null;
	}
}
