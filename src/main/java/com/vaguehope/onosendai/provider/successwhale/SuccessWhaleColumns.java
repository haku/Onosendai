package com.vaguehope.onosendai.provider.successwhale;

import java.util.Collections;
import java.util.List;

import com.vaguehope.onosendai.config.Column;

public class SuccessWhaleColumns {

	private final List<Column> columns;

	public SuccessWhaleColumns (final List<Column> columns) {
		this.columns = Collections.unmodifiableList(columns);
	}

	public List<Column> getColumns () {
		return this.columns;
	}

}
