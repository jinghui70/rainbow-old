package rainbow.core.util.converter.impl;

import java.sql.Timestamp;

import org.joda.time.DateTime;

import rainbow.core.util.converter.AbstractConverter;

/**
 * 
 * @author lijinghui
 *
 */
public class Timestamp2DateTime extends AbstractConverter<Timestamp, DateTime> {

	@Override
	public DateTime convert(Timestamp from, Class<?> toClass) {
		return new DateTime(from);
	}

}
